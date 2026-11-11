package com.ompsessionconnect.data.remote

import com.ompsessionconnect.data.crypto.FrameCipher
import com.ompsessionconnect.data.link.CollabLink
import com.ompsessionconnect.data.link.CollabLinkParser
import com.ompsessionconnect.data.protocol.Wire
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * One AES-GCM-sealed WebSocket to an omp collab relay room.
 *
 * Reconnects are owned by [com.ompsessionconnect.data.runtime.SessionConnection]
 * so it can expose a truthful status; this class only reports transport state.
 */
class CollabSocket(
  private val link: CollabLink,
  private val client: OkHttpClient,
  private val listener: Listener,
) {
  interface Listener {
    fun onOpen()
    fun onFrame(frame: JsonObject)
    fun onClosed(code: Int, reason: String)
    fun onFailure(message: String)
  }

  private val lock = Any()
  private val pending = ArrayDeque<ByteArray>()
  private var webSocket: WebSocket? = null
  private var opened = false
  private var closed = false

  fun connect() {
    val request = Request.Builder()
      .url("${link.wsUrl}?role=guest")
      .build()
    synchronized(lock) {
      if (closed) return
      opened = false
      webSocket = client.newWebSocket(request, SocketListener())
    }
  }

  /** Queues up to 128 frames while disconnected; returns false when queued. */
  fun send(frame: JsonObject): Boolean {
    val sealed = FrameCipher.seal(frame.toString(), link.key)
    val bytes = Wire.pack(0, sealed)
    synchronized(lock) {
      if (closed) return false
      val socket = webSocket
      if (!opened || socket == null) {
        if (pending.size >= MAX_PENDING_SENDS) pending.removeFirst()
        pending.addLast(bytes)
        return false
      }
      return socket.send(bytes.toByteString())
    }
  }

  fun close() {
    val socket = synchronized(lock) {
      if (closed) return
      closed = true
      opened = false
      pending.clear()
      webSocket.also { webSocket = null }
    }
    socket?.close(1000, "closed by app")
  }

  private fun flushPending(socket: WebSocket) {
    val queued = synchronized(lock) {
      if (closed || socket !== webSocket) return
      val copy = pending.toList()
      pending.clear()
      copy
    }
    queued.forEach { socket.send(it.toByteString()) }
  }

  private inner class SocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
      synchronized(lock) {
        if (closed) return
        this@CollabSocket.webSocket = webSocket
        opened = true
      }
      listener.onOpen()
      flushPending(webSocket)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
      val envelope = Wire.unpack(bytes.toByteArray()) ?: return
      val plaintext = try {
        FrameCipher.open(envelope.second, link.key)
      } catch (_: Exception) {
        failFatal("Bad room key or corrupted frame")
        return
      }
      val frame = Wire.parseObject(plaintext) ?: return
      listener.onFrame(frame)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
      // Relay control frames are plain JSON and carry no session data.
      val control = Wire.parseObject(text) ?: return
      if (control.stringLocal("t") == "room-closed") {
        listener.onClosed(4001, "room closed")
      }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
      webSocket.close(code, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
      synchronized(lock) {
        if (this@CollabSocket.webSocket === webSocket) {
          this@CollabSocket.webSocket = null
          opened = false
        }
      }
      if (closed) return
      listener.onClosed(code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
      synchronized(lock) {
        if (this@CollabSocket.webSocket === webSocket) {
          this@CollabSocket.webSocket = null
          opened = false
        }
      }
      failFatal(t.message ?: "WebSocket failure")
    }
  }

  private fun failFatal(message: String) {
    if (closed) return
    listener.onFailure(message)
  }

  private fun JsonObject.stringLocal(key: String): String? {
    val element = this[key] as? kotlinx.serialization.json.JsonPrimitive ?: return null
    return if (element.isString) element.content else null
  }

  private companion object {
    const val MAX_PENDING_SENDS = 128
  }
}

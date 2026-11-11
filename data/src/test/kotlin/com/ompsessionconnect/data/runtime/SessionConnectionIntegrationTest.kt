package com.ompsessionconnect.data.runtime

import com.ompsessionconnect.data.crypto.FrameCipher
import com.ompsessionconnect.data.link.CollabLink
import com.ompsessionconnect.data.link.CollabLinkParser
import com.ompsessionconnect.data.protocol.Wire
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.UserMessageItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SessionConnectionIntegrationTest {
  private val room = "integration01"
  private val key = ByteArray(32) { (it + 3).toByte() }
  private val token = ByteArray(16) { (it + 7).toByte() }

  @Test
  fun `joins, applies snapshot, answers ask, and sends prompt`() = runBlocking {
    val server = MockWebServer()
    val hello = CompletableFuture<JsonObject>()
    val prompt = CompletableFuture<JsonObject>()
    val uiResponse = CompletableFuture<JsonObject>()
    val command = CompletableFuture<JsonObject>()
    lateinit var hostSocket: WebSocket

    server.enqueue(
      MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          hostSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
          val envelope = Wire.unpack(bytes.toByteArray()) ?: return
          val frame = Wire.parseObject(FrameCipher.open(envelope.second, key)) ?: return
          when (frame.stringLocal("t")) {
            "hello" -> {
              hello.complete(frame)
              send(webSocket, welcome())
              send(webSocket, chunk())
              send(webSocket, uiRequest())
            }
            "ui-response" -> uiResponse.complete(frame)
            "prompt" -> prompt.complete(frame)
            "agent-cmd" -> command.complete(frame)
            "abort" -> Unit
          }
        }
      }),
    )

    server.start()
    val link = CollabLink(
      wsUrl = server.url("/r/$room").toString().replaceFirst("http", "ws"),
      roomId = room,
      key = key,
      writeToken = token,
    )
    val connection = SessionConnection(
      sessionId = "integration",
      link = link,
      initialTitle = "Integration",
      displayName = "JUnit phone",
      client = OkHttpClient(),
    )

    try {
      connection.start()
      val helloFrame = hello.get(10, TimeUnit.SECONDS)
      assertEquals(3, helloFrame.intLocal("proto"))
      assertEquals("JUnit phone", helloFrame.stringLocal("name"))
      assertEquals(CollabLinkParser.encodeBase64Url(token), helloFrame.stringLocal("writeToken"))

      val live = withTimeout(10_000) {
        connection.snapshot.first { it.phase == SessionPhase.LIVE && it.transcript.isNotEmpty() }
      }
      val message = live.transcript.single() as UserMessageItem
      assertEquals("hello from host", message.text)
      assertEquals("Integration room", live.title)

      val ask = withTimeout(10_000) {
        connection.snapshot.first { it.uiRequest?.requestId == 42 }
      }
      assertEquals("Answer the host", ask.uiRequest?.title)

      connection.respondToUi(42, "yes")
      assertEquals("yes", uiResponse.get(10, TimeUnit.SECONDS).stringLocal("value"))

      connection.sendPrompt("ping").getOrThrow()
      val promptFrame = prompt.get(10, TimeUnit.SECONDS)
      assertEquals("ping", promptFrame.stringLocal("text"))

      connection.sendCommand("/phone-test arg").getOrThrow()
      val commandFrame = command.get(10, TimeUnit.SECONDS)
      assertEquals("chat", commandFrame.stringLocal("cmd"))
      assertEquals("Main", commandFrame.stringLocal("agentId"))
      assertEquals("/phone-test arg", commandFrame.stringLocal("text"))

      send(hostSocket, buildJsonObject {
        put("t", "bye")
        put("reason", "test done")
      })
      val ended = withTimeout(10_000) {
        connection.snapshot.first { it.phase == SessionPhase.ENDED }
      }
      assertTrue(ended.endedReason!!.contains("test done"))
    } finally {
      connection.close()
      runCatching { server.shutdown() }
    }
  }

  private fun welcome(): JsonObject = Wire.parseObject(
    """{"t":"welcome","proto":3,
       "header":{"type":"session","id":"s1","title":"Integration room",
                 "timestamp":"2026-01-01T00:00:00Z","cwd":"/tmp/project"},
       "state":{"isStreaming":false,"queuedMessageCount":0,"cwd":"/tmp/project",
                "participants":[{"name":"host","role":"host"}]},
       "agents":[],"entryCount":1}""",
  )!!

  private fun chunk(): JsonObject = Wire.parseObject(
    """{"t":"snapshot-chunk","final":true,"entries":[
         {"id":"m1","parentId":null,"timestamp":"2026-01-01T00:00:00Z","type":"message",
          "message":{"role":"user","content":"hello from host","timestamp":0}}]}""",
  )!!

  private fun uiRequest(): JsonObject = Wire.parseObject(
    """{"t":"ui-request","request":{"kind":"editor","reqId":42,
        "title":"Answer the host","prefill":"maybe"}}""",
  )!!

  private fun send(socket: WebSocket, frame: JsonObject) {
    val sealed = FrameCipher.seal(frame.toString(), key)
    socket.send(Wire.pack(0, sealed).toByteStringCompat())
  }

  private fun ByteArray.toByteStringCompat(): ByteString = ByteString.of(*this)

  private fun JsonObject.stringLocal(key: String): String? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)
      ?.takeIf { it.isString }?.content

  private fun JsonObject.intLocal(key: String): Int? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
}

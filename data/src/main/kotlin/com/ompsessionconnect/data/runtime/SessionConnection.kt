package com.ompsessionconnect.data.runtime

import com.ompsessionconnect.data.link.CollabLink
import com.ompsessionconnect.data.link.CollabLinkParser
import com.ompsessionconnect.data.protocol.RemoteParsers
import com.ompsessionconnect.data.protocol.TodoParser
import com.ompsessionconnect.data.protocol.RemoteState
import com.ompsessionconnect.data.protocol.Wire
import com.ompsessionconnect.data.protocol.boolean
import com.ompsessionconnect.data.protocol.array
import com.ompsessionconnect.data.protocol.compact
import com.ompsessionconnect.data.protocol.int
import com.ompsessionconnect.data.protocol.obj
import com.ompsessionconnect.data.protocol.string
import com.ompsessionconnect.data.protocol.textOrNull
import com.ompsessionconnect.data.remote.CollabSocket
import com.ompsessionconnect.domain.model.AssistantMessageItem
import com.ompsessionconnect.domain.model.NoticeLevel
import com.ompsessionconnect.domain.model.SessionNotice
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.SessionSnapshot
import com.ompsessionconnect.domain.model.ToolCallItem
import com.ompsessionconnect.domain.model.TodoProgress
import com.ompsessionconnect.domain.model.ToolStatus
import com.ompsessionconnect.domain.model.TranscriptItem
import com.ompsessionconnect.domain.model.UiRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import java.util.ArrayDeque

/**
 * Owns the live replica of one omp collab room.
 *
 * All mutations run on a single-threaded coroutine context so frames apply in
 * arrival order. High-frequency assistant updates publish a new immutable
 * [SessionSnapshot] but reuse the transcript list until an entry changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionConnection(
  val sessionId: String,
  private val link: CollabLink,
  initialTitle: String,
  private val displayName: String,
  client: OkHttpClient,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
  private val _snapshot = MutableStateFlow(initialSnapshot(initialTitle))
  val snapshot: StateFlow<SessionSnapshot> = _snapshot.asStateFlow()

  private val client = client
  private var socket: CollabSocket? = null
  private var builder = TranscriptBuilder()
  private var transcriptView: List<TranscriptItem> = emptyList()

  private var pendingBuilder: TranscriptBuilder? = null
  private val pendingLive = mutableListOf<JsonObject>()
  private var expectedEntries = 0
  private var receivedEntries = 0
  private var loadingTotal = 0

  private var welcomed = false
  private var closedByUser = false
  private var reconnectAttempt = 0
  private var retryJob: Job? = null
  private var welcomeJob: Job? = null
  private var snapshotJob: Job? = null
  private var started = false

  private var title = initialTitle
  private var cwd: String? = null
  private var readOnly = link.isReadOnly
  private var working = false
  private var queuedMessages = 0
  private var participantCount = 0
  private var activeAgents = 0
  private var modelName: String? = null
  private var contextPercent: Int? = null
  private var contextTokens: Int? = null
  private var contextWindow: Int? = null
  private var streamingText: String? = null
  private var streamingThinking: String? = null
  private var streamDone = false
  private var endedReason: String? = null
  private var todos: TodoProgress? = null
  private var mainAgentId: String = "Main"
  private var phase = SessionPhase.CONNECTING

  private val activeToolNames = LinkedHashMap<String, String>()
  private var uiRequest: UiRequest? = null
  private val uiQueue = ArrayDeque<UiRequest>()
  private val notices = ArrayDeque<SessionNotice>()
  private var noticeSequence = 0L

  fun start() {
    if (started) return
    started = true
    connectSocket()
  }

  fun reconnect() {
    scope.launch {
      if (closedByUser) return@launch
      retryJob?.cancel()
      retryJob = null
      reconnectAttempt = 0
      welcomed = false
      socket?.close()
      socket = null
      phase = SessionPhase.CONNECTING
      endedReason = null
      publish()
      connectSocket()
    }
  }

  fun close() {
    closedByUser = true
    retryJob?.cancel()
    welcomeJob?.cancel()
    snapshotJob?.cancel()
    socket?.close()
    socket = null
    scope.cancel()
  }

  fun sendPrompt(text: String): Result<Unit> {
    if (text.isBlank()) return Result.success(Unit)
    if (readOnly) return Result.failure(IllegalStateException("This link is read-only"))
    if (phase != SessionPhase.LIVE) return Result.failure(IllegalStateException("Session is not connected"))
    val sent = socket?.send(Wire.prompt(text)) == true
    return if (sent) Result.success(Unit) else Result.failure(IllegalStateException("Not connected"))
  }

  /**
   * Send a slash command through the host session's Main agent.
   *
   * Collab's `prompt` frame wraps text as a custom message and bypasses
   * slash-command expansion. `agent-cmd chat` to the main agent calls
   * `session.prompt(...)` on the host, which runs extension/file/custom
   * command expansion and prompt templates.
   */
  fun sendCommand(text: String): Result<Unit> {
    if (readOnly) return Result.failure(IllegalStateException("This link is read-only"))
    if (phase != SessionPhase.LIVE) return Result.failure(IllegalStateException("Session is not connected"))
    val frame = buildJsonObject {
      put("t", "agent-cmd")
      put("cmd", "chat")
      put("agentId", mainAgentId)
      put("text", text)
    }
    val sent = socket?.send(frame) == true
    return if (sent) Result.success(Unit) else Result.failure(IllegalStateException("Not connected"))
  }

  fun abort() {
    if (readOnly) return
    socket?.send(Wire.abort())
  }

  fun respondToUi(requestId: Int, value: String?) {
    scope.launch {
      if (readOnly) return@launch
      socket?.send(Wire.uiResponse(requestId, value))
      if (uiRequest?.requestId == requestId) {
        uiRequest = uiQueue.pollFirst()
        publish()
      } else {
        uiQueue.removeAll { it.requestId == requestId }
      }
    }
  }

  private fun connectSocket() {
    if (closedByUser) return
    socket?.close()
    val next = CollabSocket(link, client, SocketCallbacks())
    socket = next
    phase = if (welcomed) SessionPhase.RECONNECTING else SessionPhase.CONNECTING
    publish()
    next.connect()
    armWelcomeTimeout()
  }

  private fun armWelcomeTimeout() {
    welcomeJob?.cancel()
    welcomeJob = scope.launch {
      delay(WELCOME_TIMEOUT_MS)
      if (!welcomed && !closedByUser) {
        scheduleReconnect("Timed out waiting for the host")
      }
    }
  }

  private fun armSnapshotTimeout() {
    snapshotJob?.cancel()
    snapshotJob = scope.launch {
      delay(SNAPSHOT_TIMEOUT_MS)
      if (pendingBuilder != null && !closedByUser) {
        scheduleReconnect("Timed out while syncing the transcript")
      }
    }
  }

  private inner class SocketCallbacks : CollabSocket.Listener {
    override fun onOpen() {
      // Send hello synchronously so it precedes any queued frame on this socket.
      socket?.send(
        Wire.hello(displayName, link.writeToken?.let(CollabLinkParser::encodeBase64Url)),
      )
      armWelcomeTimeout()
    }

    override fun onFrame(frame: JsonObject) {
      scope.launch { handleFrame(frame) }
    }

    override fun onClosed(code: Int, reason: String) {
      scope.launch { handleSocketClosed(code, reason) }
    }

    override fun onFailure(message: String) {
      scope.launch {
        if (message.startsWith("Bad room key")) fail(message) else scheduleReconnect(message)
      }
    }
  }

  private fun handleFrame(frame: JsonObject) {
    if (closedByUser) return
    when (frame.string("t")) {
      "welcome" -> handleWelcome(frame)
      "snapshot-chunk" -> handleSnapshotChunk(frame)
      "entry" -> handleEntry(frame)
      "event" -> handleEvent(frame.obj("event") ?: return)
      "state" -> {
        val state = RemoteParsers.parseState(frame)
        applyState(state)
        if (!state.isStreaming && streamDone) {
          streamingText = null
          streamingThinking = null
          streamDone = false
          activeToolNames.clear()
        }
        publish()
      }
      "agents" -> {
        val agents = frame.array("agents").orEmpty().mapNotNull { it as? JsonObject }
        activeAgents = agents.count { it.string("status") == "running" }
        agents.firstOrNull { it.string("kind") == "main" }
          ?.string("id")
          ?.takeIf { it.isNotBlank() }
          ?.let { mainAgentId = it }
        publish()
      }
      "ui-request" -> {
        val request = RemoteParsers.parseUiRequest(frame) ?: return
        if (uiRequest == null) uiRequest = request else uiQueue.addLast(request)
        publish()
      }
      "ui-request-end" -> {
        val requestId = frame.int("reqId") ?: return
        if (uiRequest?.requestId == requestId) {
          uiRequest = uiQueue.pollFirst()
        } else {
          uiQueue.removeAll { it.requestId == requestId }
        }
        publish()
      }
      "bye" -> {
        endedReason = frame.string("reason") ?: "Session ended"
        phase = SessionPhase.ENDED
        welcomeJob?.cancel()
        snapshotJob?.cancel()
        socket?.close()
        publish()
      }
      "error" -> {
        val message = frame.string("message") ?: "Host error"
        if (frame.boolean("resync") == true) {
          socket?.send(
            Wire.hello(displayName, link.writeToken?.let(CollabLinkParser::encodeBase64Url)),
          )
        } else if (!welcomed) {
          fail(message)
        } else {
          addNotice(message, NoticeLevel.ERROR)
          publish()
        }
      }
      else -> Unit
    }
  }

  private fun handleWelcome(frame: JsonObject) {
    val proto = frame.int("proto")
    if (proto != null && proto != Wire.PROTO) {
      fail("Host protocol $proto is not supported (expected ${Wire.PROTO})")
      return
    }
    welcomed = true
    reconnectAttempt = 0
    welcomeJob?.cancel()
    retryJob?.cancel()
    retryJob = null

    val header = frame.obj("header")
    val state = RemoteParsers.parseState(frame)
    applyState(state)
    header?.string("cwd")?.let { cwd = it }
    title = header?.string("title")?.takeIf { it.isNotBlank() }
      ?: state.sessionName
      ?: title
    readOnly = frame.boolean("readOnly") ?: link.isReadOnly

    expectedEntries = (frame.int("entryCount") ?: 0).coerceAtLeast(0)
    loadingTotal = expectedEntries
    receivedEntries = 0
    pendingLive.clear()
    todos = null
    uiRequest = null
    uiQueue.clear()
    streamingText = null
    streamingThinking = null
    streamDone = false
    activeToolNames.clear()
    phase = SessionPhase.CONNECTING

    if (expectedEntries == 0) {
      builder = TranscriptBuilder()
      transcriptView = emptyList()
      pendingBuilder = null
      phase = SessionPhase.LIVE
      snapshotJob?.cancel()
    } else {
      pendingBuilder = TranscriptBuilder()
      armSnapshotTimeout()
    }
    publish()
  }

  private fun handleSnapshotChunk(frame: JsonObject) {
    val pending = pendingBuilder ?: return
    val entries = frame.array("entries").orEmpty()
      .mapNotNull { it as? JsonObject }
    entries.forEach(::updateTodosFromEntry)
    pending.applyEntries(entries)
    receivedEntries += entries.size
    val final = frame.boolean("final") == true
    if (final || receivedEntries >= expectedEntries) {
      finishSnapshot(pending)
    } else {
      armSnapshotTimeout()
      publish()
    }
  }

  private fun finishSnapshot(pending: TranscriptBuilder) {
    pending.applyEntries(pendingLive.toList())
    pendingLive.clear()
    builder = pending
    pendingBuilder = null
    transcriptView = builder.snapshot()
    // A turn that was streaming while we re-synced will start a fresh
    // message_update; the stale ghost must not survive the swap.
    streamingText = null
    streamingThinking = null
    streamDone = false
    phase = SessionPhase.LIVE
    snapshotJob?.cancel()
    publish()
  }

  private fun handleEntry(frame: JsonObject) {
    val entry = frame.obj("entry") ?: return
    updateTodosFromEntry(entry)
    val pending = pendingBuilder
    if (pending != null) {
      pendingLive.add(entry)
      return
    }
    builder.applyEntry(entry)
    if (entry.string("type") == "message") {
      val role = entry.obj("message")?.string("role")
      if (role == "assistant") {
        // The durable row supersedes the stream ghost even when a host omits
        // message_end before the entry.
        streamingText = null
        streamingThinking = null
        streamDone = false
      }
    }
    transcriptView = builder.snapshot()
    publish()
  }

  private fun handleEvent(event: JsonObject) {
    when (event.string("type")) {
      "message_start", "message_update" -> {
        val message = event.obj("message") ?: return
        if (message.string("role") == "assistant") {
          val (text, thinking) = assistantPreview(message)
          streamingText = text.ifBlank { null }
          streamingThinking = thinking.ifBlank { null }
          streamDone = false
          publish()
        }
      }
      "message_end" -> {
        val message = event.obj("message") ?: return
        if (message.string("role") == "assistant") {
          val (text, thinking) = assistantPreview(message)
          streamingText = text.ifBlank { null }
          streamingThinking = thinking.ifBlank { null }
          streamDone = true
          publish()
        }
      }
      "tool_execution_start" -> {
        val id = event.string("toolCallId") ?: return
        val name = event.string("toolName") ?: "tool"
        activeToolNames[id] = name
        builder.applyToolStart(id, name, event["args"])
        transcriptView = builder.snapshot()
        publish()
      }
      "tool_execution_update" -> {
        val id = event.string("toolCallId") ?: return
        val name = event.string("toolName") ?: activeToolNames[id] ?: "tool"
        builder.applyToolUpdate(id, name, event["args"], event["partialResult"])
        transcriptView = builder.snapshot()
        publish()
      }
      "tool_execution_end" -> {
        val id = event.string("toolCallId") ?: return
        val name = event.string("toolName") ?: activeToolNames[id] ?: "tool"
        activeToolNames.remove(id)
        if (name == "todo") {
          TodoParser.fromToolResult(event["result"])?.let { todos = it }
        }
        builder.applyToolEnd(id, name, event["result"], event.boolean("isError") == true)
        transcriptView = builder.snapshot()
        publish()
      }
      "agent_start" -> {
        working = true
        publish()
      }
      "agent_end" -> {
        working = false
        publish()
      }
      "notice" -> {
        addNotice(
          event.string("message") ?: return,
          when (event.string("level")) {
            "error" -> NoticeLevel.ERROR
            "warning" -> NoticeLevel.WARNING
            else -> NoticeLevel.INFO
          },
        )
        publish()
      }
      "auto_retry_start" -> {
        val attempt = event.int("attempt") ?: 0
        val max = event.int("maxAttempts") ?: 0
        addNotice("Retry $attempt/$max: ${event.string("errorMessage").orEmpty()}", NoticeLevel.WARNING)
        publish()
      }
      "auto_retry_end" -> {
        if (event.boolean("success") != true) {
          addNotice(event.string("finalError") ?: "Retry failed", NoticeLevel.ERROR)
          publish()
        }
      }
      "auto_compaction_start" -> {
        addNotice("Compacting context (${event.string("reason") ?: "automatic"})", NoticeLevel.INFO)
        publish()
      }
      "auto_compaction_end" -> {
        if (event.boolean("skipped") != true) {
          addNotice(
            when {
              event.boolean("aborted") == true -> "Compaction aborted"
              event.string("errorMessage") != null -> "Compaction failed: ${event.string("errorMessage")}"
              else -> "Context compacted"
            },
            if (event.string("errorMessage") != null) NoticeLevel.ERROR else NoticeLevel.INFO,
          )
          publish()
        }
      }
      else -> Unit
    }
  }

  private fun updateTodosFromEntry(entry: JsonObject) {
    TodoParser.fromEntry(entry)?.let { todos = it }
  }

  private fun applyState(state: RemoteState) {
    working = state.isStreaming
    queuedMessages = state.queuedMessages
    participantCount = state.participants.size
    modelName = state.model?.name ?: state.model?.id ?: modelName
    contextPercent = state.contextPercent
    contextTokens = state.contextTokens
    contextWindow = state.contextWindow
    state.cwd?.takeIf { it.isNotBlank() }?.let { cwd = it }
    if (title.isBlank()) title = state.sessionName ?: title
  }

  private fun handleSocketClosed(code: Int, reason: String) {
    if (closedByUser || phase == SessionPhase.FAILED) return
    when (code) {
      1000 -> {
        endedReason = reason.ifBlank { "Session closed" }
        phase = SessionPhase.ENDED
        publish()
      }
      4009 -> fail("A host is already connected for this room")
      4029 -> fail("Room is full")
      else -> scheduleReconnect(reason.ifBlank { "Connection lost (code $code)" })
    }
  }

  private fun scheduleReconnect(reason: String) {
    if (closedByUser || phase == SessionPhase.ENDED || phase == SessionPhase.FAILED) return
    if (retryJob?.isActive == true) return
    if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
      fail(reason)
      return
    }
    val delayMs = (BASE_RETRY_MS shl reconnectAttempt.coerceAtMost(5))
      .coerceAtMost(MAX_RETRY_MS)
    reconnectAttempt++
    phase = SessionPhase.RECONNECTING
    endedReason = null
    addNotice("Reconnecting: $reason", NoticeLevel.WARNING)
    publish()
    retryJob = scope.launch {
      delay(delayMs)
      if (closedByUser) return@launch
      welcomeJob?.cancel()
      snapshotJob?.cancel()
      welcomed = false
      socket?.close()
      socket = null
      connectSocket()
    }
  }

  private fun fail(reason: String) {
    if (closedByUser) return
    phase = SessionPhase.FAILED
    endedReason = reason
    addNotice(reason, NoticeLevel.ERROR)
    welcomeJob?.cancel()
    snapshotJob?.cancel()
    retryJob?.cancel()
    retryJob = null
    socket?.close()
    socket = null
    publish()
  }

  private fun addNotice(text: String, level: NoticeLevel) {
    notices.addLast(
      SessionNotice(
        id = ++noticeSequence,
        text = text,
        level = level,
        timestamp = System.currentTimeMillis(),
      ),
    )
    while (notices.size > MAX_NOTICES) notices.removeFirst()
  }

  private fun publish() {
    _snapshot.value = SessionSnapshot(
      sessionId = sessionId,
      title = title,
      roomId = link.roomId,
      cwd = cwd,
      phase = phase,
      readOnly = readOnly,
      working = working,
      modelName = modelName,
      contextPercent = contextPercent,
      contextTokens = contextTokens,
      contextWindow = contextWindow,
      queuedMessages = queuedMessages,
      participantCount = participantCount,
      activeAgents = activeAgents,
      loadingReceived = receivedEntries,
      loadingTotal = loadingTotal,
      transcript = transcriptView,
      streamingText = streamingText,
      streamingThinking = streamingThinking,
      uiRequest = uiRequest,
      notices = notices.toList(),
      todos = todos,
      endedReason = endedReason,
      lastUpdated = System.currentTimeMillis(),
    )
  }

  private fun initialSnapshot(initialTitle: String) = SessionSnapshot(
    sessionId = sessionId,
    title = initialTitle,
    roomId = link.roomId,
    cwd = null,
    phase = SessionPhase.CONNECTING,
    readOnly = link.isReadOnly,
    working = false,
    modelName = null,
    contextPercent = null,
    contextTokens = null,
    contextWindow = null,
    queuedMessages = 0,
    participantCount = 0,
    activeAgents = 0,
    loadingReceived = 0,
    loadingTotal = 0,
    transcript = emptyList(),
    streamingText = null,
    streamingThinking = null,
    uiRequest = null,
    notices = emptyList(),
    todos = null,
    endedReason = null,
    lastUpdated = System.currentTimeMillis(),
  )

  private fun assistantPreview(message: JsonObject): Pair<String, String> {
    val content = message["content"] as? JsonArray ?: return "" to ""
    val text = StringBuilder()
    val thinking = StringBuilder()
    content.forEach { block ->
      val obj = block as? JsonObject ?: return@forEach
      when (obj.string("type")) {
        "text" -> obj.string("text")?.let {
          if (text.isNotEmpty()) text.append('\n')
          text.append(it)
        }
        "thinking" -> obj.string("thinking")?.let {
          if (thinking.isNotEmpty()) thinking.append('\n')
          thinking.append(it)
        }
      }
    }
    return text.toString() to thinking.toString()
  }

  private companion object {
    const val WELCOME_TIMEOUT_MS = 30_000L
    const val SNAPSHOT_TIMEOUT_MS = 30_000L
    const val BASE_RETRY_MS = 1_000L
    const val MAX_RETRY_MS = 30_000L
    const val MAX_RECONNECT_ATTEMPTS = 5
    const val MAX_NOTICES = 50
  }
}

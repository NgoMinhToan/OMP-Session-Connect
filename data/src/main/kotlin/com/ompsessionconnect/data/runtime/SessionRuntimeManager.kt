package com.ompsessionconnect.data.runtime

import android.os.Build
import com.ompsessionconnect.data.crypto.LinkCipher
import com.ompsessionconnect.data.link.CollabLink
import com.ompsessionconnect.data.link.CollabLinkParser
import com.ompsessionconnect.data.store.SessionStore
import com.ompsessionconnect.data.store.StoredSession
import com.ompsessionconnect.domain.model.AssistantMessageItem
import com.ompsessionconnect.domain.model.SessionAlert
import com.ompsessionconnect.domain.model.NoticeLevel
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.SessionSnapshot
import com.ompsessionconnect.domain.model.ToolCallItem
import com.ompsessionconnect.domain.model.UserMessageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps one [SessionConnection] per stored room, reconnecting them whenever
 * the process starts. All room mutations stay on one dispatcher; callers only
 * see the merged, immutable snapshot map.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRuntimeManager(
  private val store: SessionStore,
  private val linkCipher: LinkCipher,
  private val client: OkHttpClient,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
  private val started = AtomicBoolean(false)

  private val lock = Any()
  private val connections = mutableMapOf<String, SessionConnection>()
  private val collectors = mutableMapOf<String, Job>()
  private val previous = mutableMapOf<String, SessionSnapshot>()
  private val lastMetaWrite = mutableMapOf<String, Long>()
  private val lastLiveAt = mutableMapOf<String, Long>()
  private val lastLiveText = mutableMapOf<String, String?>()

  private val _runtimes = MutableStateFlow<Map<String, SessionSnapshot>>(emptyMap())
  val runtimes: StateFlow<Map<String, SessionSnapshot>> = _runtimes.asStateFlow()

  private val _alerts = MutableSharedFlow<SessionAlert>(extraBufferCapacity = 64)
  val alerts: SharedFlow<SessionAlert> = _alerts.asSharedFlow()

  private val displayName: String = "Android ${Build.MODEL}".trim()

  fun start() {
    if (!started.compareAndSet(false, true)) return
    scope.launch {
      store.sessions.collect(::sync)
    }
  }

  suspend fun connectNow(sessionId: String) {
    connections[sessionId]?.start()
  }

  fun reconnect(sessionId: String) {
    synchronized(lock) { connections[sessionId] }?.reconnect()
  }

  fun sendPrompt(sessionId: String, text: String): Result<Unit> =
    synchronized(lock) { connections[sessionId] }?.sendPrompt(text)
      ?: Result.failure(IllegalStateException("Session is not running"))

  fun sendCommand(sessionId: String, text: String): Result<Unit> =
    synchronized(lock) { connections[sessionId] }?.sendCommand(text)
      ?: Result.failure(IllegalStateException("Session is not running"))

  fun abort(sessionId: String) {
    synchronized(lock) { connections[sessionId] }?.abort()
  }

  fun respondToUi(sessionId: String, requestId: Int, value: String?) {
    synchronized(lock) { connections[sessionId] }?.respondToUi(requestId, value)
  }

  fun close(sessionId: String) {
    val connection = synchronized(lock) {
      previous.remove(sessionId)
      lastMetaWrite.remove(sessionId)
      lastLiveAt.remove(sessionId)
      lastLiveText.remove(sessionId)
      collectors.remove(sessionId)?.cancel()
      connections.remove(sessionId)
    }
    connection?.close()
    _runtimes.update { it - sessionId }
  }

  private fun sync(records: List<StoredSession>) {
    val liveIds = records.mapTo(mutableSetOf()) { it.id }
    synchronized(lock) {
      connections.keys.filterNot { it in liveIds }.forEach { id ->
        connections.remove(id)?.close()
        collectors.remove(id)?.cancel()
        previous.remove(id)
        lastMetaWrite.remove(id)
        lastLiveAt.remove(id)
        lastLiveText.remove(id)
        _runtimes.update { current -> current - id }
      }
    }

    records.forEach { record ->
      val existing = synchronized(lock) { connections.containsKey(record.id) }
      if (existing) return@forEach
      val link = linkCipher.decrypt(record.encryptedLink)
        ?.let { CollabLinkParser.parse(it).getOrNull() }
      if (link == null) {
        publishFailure(record, "Saved link cannot be decrypted on this device")
        return@forEach
      }
      val connection = SessionConnection(
        sessionId = record.id,
        link = link,
        initialTitle = record.title,
        displayName = displayName,
        client = client,
      )
      val added = synchronized(lock) {
        if (connections.containsKey(record.id)) false else {
          connections[record.id] = connection
          true
        }
      }
      if (!added) {
        connection.close()
        return@forEach
      }
      val collector = scope.launch {
        connection.snapshot.collect { snapshot ->
          if (synchronized(lock) { connections[record.id] === connection }) {
            onSnapshot(record, snapshot)
          }
        }
      }
      synchronized(lock) {
        if (connections[record.id] !== connection) {
          collector.cancel()
        } else {
          collectors[record.id]?.cancel()
          collectors[record.id] = collector
        }
      }
      connection.start()
    }
  }

  private fun onSnapshot(record: StoredSession, snapshot: SessionSnapshot) {
    val prior = synchronized(lock) {
      previous.put(record.id, snapshot)
    }
    _runtimes.update { it + (record.id to snapshot) }
    emitAlerts(prior, snapshot)
    maybePersistMeta(record, snapshot)
  }

  private fun emitAlerts(prior: SessionSnapshot?, next: SessionSnapshot) {
    if (prior == null) return
    val nextRequest = next.uiRequest
    if (prior.uiRequest?.requestId != nextRequest?.requestId && nextRequest != null) {
      _alerts.tryEmit(
        SessionAlert.WaitingForInput(
          sessionId = next.sessionId,
          title = next.title,
          requestTitle = nextRequest.title,
        ),
      )
    }
    if (prior.working && !next.working && next.phase == SessionPhase.LIVE && nextRequest == null) {
      _alerts.tryEmit(SessionAlert.TurnFinished(next.sessionId, next.title))
    }
    if (prior.phase != SessionPhase.FAILED && next.phase == SessionPhase.FAILED) {
      _alerts.tryEmit(
        SessionAlert.ConnectionLost(
          sessionId = next.sessionId,
          title = next.title,
          reason = next.endedReason ?: "Connection lost",
        ),
      )
    }
    if (prior.phase != SessionPhase.ENDED && next.phase == SessionPhase.ENDED) {
      _alerts.tryEmit(
        SessionAlert.Ended(
          sessionId = next.sessionId,
          title = next.title,
          reason = next.endedReason ?: "Session ended",
        ),
      )
    }

    val lastNoticeId = prior.notices.maxOfOrNull { it.id } ?: 0L
    next.notices.firstOrNull { it.id > lastNoticeId && it.level == NoticeLevel.ERROR }?.let { notice ->
      _alerts.tryEmit(SessionAlert.AgentError(next.sessionId, next.title, notice.text))
    }

    val progress = next.todos
    val startedWorking = !prior.working && next.working
    val liveText = latestLiveText(next)
    val textChanged = liveText != lastLiveText[next.sessionId]
    val todoChanged = progress != prior.todos
    if (next.working && (startedWorking || textChanged || todoChanged)) {
      val now = System.currentTimeMillis()
      val due = now - (lastLiveAt[next.sessionId] ?: 0L) >= LIVE_NOTIFICATION_INTERVAL_MS
      if (due) {
        lastLiveAt[next.sessionId] = now
        lastLiveText[next.sessionId] = liveText
        _alerts.tryEmit(
          SessionAlert.Live(
            sessionId = next.sessionId,
            title = next.title,
            message = liveText,
            completed = progress?.completed ?: 0,
            total = progress?.total ?: 0,
            current = progress?.next?.content,
          ),
        )
      }
    }
  }

  private fun latestLiveText(snapshot: SessionSnapshot): String? {
    snapshot.streamingText?.takeIf { it.isNotBlank() }?.let { return compactLive(it) }
    for (item in snapshot.transcript.asReversed()) {
      when (item) {
        is AssistantMessageItem -> if (item.text.isNotBlank()) return compactLive(item.text)
        is UserMessageItem -> if (item.text.isNotBlank()) return compactLive(item.text)
        is ToolCallItem -> return compactLive("${item.toolName} · ${item.arguments}")
        else -> Unit
      }
    }
    return null
  }

  private fun compactLive(text: String): String {
    val singleLine = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
    return if (singleLine.length <= 220) singleLine else singleLine.take(219) + "…"
  }

  private fun maybePersistMeta(record: StoredSession, snapshot: SessionSnapshot) {
    if (snapshot.phase != SessionPhase.LIVE && snapshot.phase != SessionPhase.ENDED) return
    val now = System.currentTimeMillis()
    synchronized(lock) {
      val previous = lastMetaWrite[record.id] ?: 0L
      if (now - previous < 30_000L && snapshot.title == record.title && snapshot.cwd == record.cwd) {
        return
      }
      lastMetaWrite[record.id] = now
    }
    scope.launch {
      store.updateMeta(
        id = record.id,
        title = snapshot.title,
        cwd = snapshot.cwd,
        lastActiveAt = snapshot.lastUpdated,
      )
    }
  }

  private fun publishFailure(record: StoredSession, reason: String) {
    val snapshot = SessionSnapshot(
      sessionId = record.id,
      title = record.title,
      roomId = record.roomId,
      cwd = record.cwd,
      phase = SessionPhase.FAILED,
      readOnly = record.readOnly,
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
      endedReason = reason,
      lastUpdated = System.currentTimeMillis(),
    )
    synchronized(lock) { previous[record.id] = snapshot }
    _runtimes.update { it + (record.id to snapshot) }
  }

  private companion object {
    const val LIVE_NOTIFICATION_INTERVAL_MS = 700L
  }
}

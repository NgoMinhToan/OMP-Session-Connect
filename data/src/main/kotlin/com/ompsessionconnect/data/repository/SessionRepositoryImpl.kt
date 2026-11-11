package com.ompsessionconnect.data.repository

import com.ompsessionconnect.data.crypto.LinkCipher
import com.ompsessionconnect.data.link.CollabLinkParser
import com.ompsessionconnect.data.runtime.SessionRuntimeManager
import com.ompsessionconnect.data.store.SessionStore
import com.ompsessionconnect.data.store.StoredSession
import com.ompsessionconnect.domain.model.AgentTranscriptPage
import com.ompsessionconnect.domain.model.SessionAlert
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionSummary
import com.ompsessionconnect.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
  private val store: SessionStore,
  private val manager: SessionRuntimeManager,
  private val linkCipher: LinkCipher,
) : SessionRepository {

  override fun observeSessions(): Flow<List<SessionOverview>> =
    combine(store.sessions, manager.runtimes) { records, runtimes ->
      records.map { record ->
        SessionOverview(record.toSummary(), runtimes[record.id])
      }.sortedWith(
        compareByDescending<SessionOverview> { it.needsAttention }
          .thenByDescending { it.snapshot?.lastUpdated ?: it.summary.lastActiveAt },
      )
    }

  override fun observeSession(sessionId: String): Flow<SessionOverview?> =
    observeSessions().map { sessions -> sessions.firstOrNull { it.id == sessionId } }

  override val alerts: SharedFlow<SessionAlert> get() = manager.alerts

  override suspend fun start() {
    manager.start()
  }

  override suspend fun addSession(link: String): Result<SessionSummary> =
    withContext(Dispatchers.IO) {
      val parsed = CollabLinkParser.parse(link).getOrElse { return@withContext Result.failure(it) }
      val now = System.currentTimeMillis()
      val record = StoredSession(
        id = UUID.randomUUID().toString(),
        encryptedLink = linkCipher.encrypt(link.trim()),
        fingerprint = parsed.fingerprint(),
        title = "OMP ${parsed.roomId.take(6)}",
        roomId = parsed.roomId,
        readOnly = parsed.isReadOnly,
        createdAt = now,
        lastActiveAt = now,
      )
      runCatching {
        val outcome = store.add(record)
        if (outcome.duplicate) {
          throw IllegalStateException("This session is already added")
        }
        outcome.record.toSummary()
      }
    }

  override suspend fun closeSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      manager.close(sessionId)
      store.delete(setOf(sessionId))
    }
  }

  override suspend fun closeSessions(sessionIds: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      sessionIds.forEach(manager::close)
      store.delete(sessionIds)
    }
  }

  override suspend fun reconnect(sessionId: String) {
    manager.reconnect(sessionId)
  }

  override suspend fun sendPrompt(sessionId: String, text: String): Result<Unit> =
    manager.sendPrompt(sessionId, text)

  override suspend fun sendCommand(sessionId: String, text: String): Result<Unit> =
    manager.sendCommand(sessionId, text)

  override suspend fun abort(sessionId: String) {
    manager.abort(sessionId)
  }

  override suspend fun respondToUi(sessionId: String, requestId: Int, value: String?) {
    manager.respondToUi(sessionId, requestId, value)
  }

  override suspend fun fetchAgentTranscript(
    sessionId: String,
    agentId: String,
    fromOffset: Long,
  ): Result<AgentTranscriptPage> = manager.fetchAgentTranscript(sessionId, agentId, fromOffset)
}

private fun StoredSession.toSummary() = SessionSummary(
  id = id,
  title = title,
  roomId = roomId,
  cwd = cwd,
  readOnly = readOnly,
  createdAt = createdAt,
  lastActiveAt = lastActiveAt,
)

package com.ompsessionconnect.domain.repository

import com.ompsessionconnect.domain.model.SessionAlert
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Single entry point for stored sessions and their live collab connections.
 *
 * Implementations own the sockets; callers only observe immutable snapshots.
 * Links are secrets and never leave the data layer in plain text.
 */
interface SessionRepository {
  /** Stored sessions merged with their live runtime snapshots, newest activity first. */
  fun observeSessions(): Flow<List<SessionOverview>>

  fun observeSession(sessionId: String): Flow<SessionOverview?>

  /** Transient status transitions worth a notification. */
  val alerts: SharedFlow<SessionAlert>

  suspend fun start()

  suspend fun addSession(link: String): Result<SessionSummary>

  suspend fun closeSession(sessionId: String): Result<Unit>

  suspend fun closeSessions(sessionIds: Set<String>): Result<Unit>

  suspend fun reconnect(sessionId: String)

  suspend fun sendPrompt(sessionId: String, text: String): Result<Unit>

  /** Send a slash command through the host session's Main-agent prompt pipeline. */
  suspend fun sendCommand(sessionId: String, text: String): Result<Unit>

  suspend fun abort(sessionId: String)

  suspend fun respondToUi(sessionId: String, requestId: Int, value: String?)
}

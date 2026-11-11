package com.ompsessionconnect.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.sessionDataStore by preferencesDataStore(name = "omp_sessions")

@Serializable
data class StoredSession(
  val id: String,
  val encryptedLink: String,
  val fingerprint: String,
  val title: String,
  val roomId: String,
  val cwd: String? = null,
  val readOnly: Boolean,
  val createdAt: Long,
  val lastActiveAt: Long,
)

@Serializable
private data class SessionDatabase(
  val version: Int = 1,
  val sessions: List<StoredSession> = emptyList(),
)

data class AddSessionOutcome(
  val record: StoredSession,
  val duplicate: Boolean,
)

/**
 * Small, encrypted session index. A single JSON document is enough for the
 * handful of rooms a developer keeps around; collab links never reach disk in
 * plaintext.
 */
class SessionStore(private val context: Context) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  val sessions: Flow<List<StoredSession>> = context.sessionDataStore.data
    .catch { error ->
      if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
      else throw error
    }
    .map { preferences -> decode(preferences[KEY_SESSIONS]) }

  suspend fun add(record: StoredSession): AddSessionOutcome {
    var outcome = AddSessionOutcome(record, duplicate = false)
    context.sessionDataStore.edit { preferences ->
      val current = decode(preferences[KEY_SESSIONS])
      val existing = current.firstOrNull { it.fingerprint == record.fingerprint }
      outcome = if (existing != null) {
        AddSessionOutcome(existing, duplicate = true)
      } else {
        preferences[KEY_SESSIONS] = json.encodeToString(SessionDatabase(sessions = current + record))
        AddSessionOutcome(record, duplicate = false)
      }
    }
    return outcome
  }

  suspend fun delete(ids: Set<String>) {
    if (ids.isEmpty()) return
    context.sessionDataStore.edit { preferences ->
      val current = decode(preferences[KEY_SESSIONS])
      val kept = current.filterNot { it.id in ids }
      if (kept.size != current.size) {
        preferences[KEY_SESSIONS] = json.encodeToString(SessionDatabase(sessions = kept))
      }
    }
  }

  suspend fun updateMeta(
    id: String,
    title: String,
    cwd: String?,
    lastActiveAt: Long,
  ) {
    context.sessionDataStore.edit { preferences ->
      val current = decode(preferences[KEY_SESSIONS])
      val index = current.indexOfFirst { it.id == id }
      if (index < 0) return@edit
      val old = current[index]
      if (old.title == title && old.cwd == cwd && lastActiveAt - old.lastActiveAt < META_UPDATE_INTERVAL_MS) {
        return@edit
      }
      val updated = current.toMutableList()
      updated[index] = old.copy(title = title, cwd = cwd, lastActiveAt = lastActiveAt)
      preferences[KEY_SESSIONS] = json.encodeToString(SessionDatabase(sessions = updated))
    }
  }

  private fun decode(raw: String?): List<StoredSession> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      json.decodeFromString<SessionDatabase>(raw).sessions
    }.getOrDefault(emptyList())
  }

  private companion object {
    val KEY_SESSIONS = stringPreferencesKey("sessions_v1")
    const val META_UPDATE_INTERVAL_MS = 30_000L
  }
}

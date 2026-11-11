package com.ompsessionconnect.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "omp_settings")

@Singleton
class SettingsStore @Inject constructor(
  private val context: Context,
) {
  val settings: Flow<AppSettings> = context.settingsDataStore.data
    .catch { error ->
      if (error is IOException) emit(emptyPreferences()) else throw error
    }
    .map { preferences ->
      AppSettings(
        themeMode = when (preferences[KEY_THEME]) {
          ThemeMode.LIGHT.name -> ThemeMode.LIGHT
          ThemeMode.DARK.name -> ThemeMode.DARK
          else -> ThemeMode.SYSTEM
        },
        notificationsEnabled = preferences[KEY_NOTIFICATIONS] ?: true,
        notifyTurnFinished = preferences[KEY_FINISHED] ?: true,
        notifyErrors = preferences[KEY_ERRORS] ?: true,
        notifyLiveProgress = preferences[KEY_PROGRESS] ?: true,
        notifyLiveMessages = preferences[KEY_LIVE_MESSAGES] ?: true,
      )
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
  }

  suspend fun setNotifyTurnFinished(enabled: Boolean) {
    context.settingsDataStore.edit { it[KEY_FINISHED] = enabled }
  }

  suspend fun setNotifyErrors(enabled: Boolean) {
    context.settingsDataStore.edit { it[KEY_ERRORS] = enabled }
  }

  suspend fun setNotifyLiveProgress(enabled: Boolean) {
    context.settingsDataStore.edit { it[KEY_PROGRESS] = enabled }
  }

  suspend fun setNotifyLiveMessages(enabled: Boolean) {
    context.settingsDataStore.edit { it[KEY_LIVE_MESSAGES] = enabled }
  }

  private companion object {
    val KEY_THEME = stringPreferencesKey("theme_mode")
    val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    val KEY_FINISHED = booleanPreferencesKey("notify_finished")
    val KEY_ERRORS = booleanPreferencesKey("notify_errors")
    val KEY_PROGRESS = booleanPreferencesKey("notify_progress")
    val KEY_LIVE_MESSAGES = booleanPreferencesKey("notify_live_messages")
  }
}

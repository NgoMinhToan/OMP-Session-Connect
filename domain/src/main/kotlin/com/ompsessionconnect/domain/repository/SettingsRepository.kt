package com.ompsessionconnect.domain.repository

import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun setThemeMode(mode: ThemeMode)

  suspend fun setDynamicColor(enabled: Boolean)

  suspend fun setNotificationsEnabled(enabled: Boolean)

  suspend fun setNotifyTurnFinished(enabled: Boolean)

  suspend fun setNotifyErrors(enabled: Boolean)

  suspend fun setNotifyLiveProgress(enabled: Boolean)

  suspend fun setNotifyLiveMessages(enabled: Boolean)
}

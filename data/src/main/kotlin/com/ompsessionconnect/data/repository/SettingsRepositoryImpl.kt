package com.ompsessionconnect.data.repository

import com.ompsessionconnect.data.store.SettingsStore
import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.model.ThemeMode
import com.ompsessionconnect.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
  private val store: SettingsStore,
) : SettingsRepository {
  override val settings: Flow<AppSettings> get() = store.settings

  override suspend fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)

  override suspend fun setNotificationsEnabled(enabled: Boolean) = store.setNotificationsEnabled(enabled)

  override suspend fun setNotifyTurnFinished(enabled: Boolean) = store.setNotifyTurnFinished(enabled)

  override suspend fun setNotifyErrors(enabled: Boolean) = store.setNotifyErrors(enabled)

  override suspend fun setNotifyLiveProgress(enabled: Boolean) = store.setNotifyLiveProgress(enabled)

  override suspend fun setNotifyLiveMessages(enabled: Boolean) = store.setNotifyLiveMessages(enabled)
}

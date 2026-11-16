package com.ompsessionconnect.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.model.ThemeMode
import com.ompsessionconnect.domain.model.UpdateRelease
import com.ompsessionconnect.domain.model.UpdateState
import com.ompsessionconnect.domain.repository.SettingsRepository
import com.ompsessionconnect.domain.usecase.ObserveSettingsUseCase
import com.ompsessionconnect.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
  observeSettings: ObserveSettingsUseCase,
  private val repository: SettingsRepository,
  private val updates: UpdateManager,
) : ViewModel() {
  val settings = observeSettings()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

  val updateState: StateFlow<UpdateState> = updates.state
  val currentVersionName: String = updates.currentVersionName
  val currentVersionCode: Int = updates.currentVersionCode
  val buildType: String = updates.buildType

  fun checkForUpdates() = updates.checkForUpdate()

  fun downloadUpdate(release: UpdateRelease) = updates.startDownload(release)

  fun installUpdate(): Boolean = updates.installDownloaded()

  fun setTheme(mode: ThemeMode) {
    viewModelScope.launch { repository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { repository.setDynamicColor(enabled) }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
  }

  fun setNotifyTurnFinished(enabled: Boolean) {
    viewModelScope.launch { repository.setNotifyTurnFinished(enabled) }
  }

  fun setNotifyErrors(enabled: Boolean) {
    viewModelScope.launch { repository.setNotifyErrors(enabled) }
  }

  fun setNotifyLiveProgress(enabled: Boolean) {
    viewModelScope.launch { repository.setNotifyLiveProgress(enabled) }
  }

  fun setNotifyLiveMessages(enabled: Boolean) {
    viewModelScope.launch { repository.setNotifyLiveMessages(enabled) }
  }
}

package com.ompsessionconnect.update

import android.content.Context
import android.os.Build
import com.ompsessionconnect.AppVisibility
import com.ompsessionconnect.BuildConfig
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.AppVersion
import com.ompsessionconnect.domain.model.UpdateRelease
import com.ompsessionconnect.domain.model.UpdateState
import com.ompsessionconnect.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the update state machine: check GitHub, hand downloads to [UpdateService],
 * then install locally or leave a notification that opens the installer.
 */
@Singleton
class UpdateManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val repository: UpdateRepository,
  private val notifier: UpdateNotifier,
  private val installer: ApkInstaller,
  private val visibility: AppVisibility,
  private val prefs: UpdatePrefs,
) {
  val currentVersionName: String = BuildConfig.VERSION_NAME
  val currentVersionCode: Int = BuildConfig.VERSION_CODE
  val buildType: String = BuildConfig.BUILD_TYPE

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val _state = MutableStateFlow(restoredState())
  val state: StateFlow<UpdateState> = _state.asStateFlow()

  private var checkJob: Job? = null

  /** Checks the newest GitHub release; called once when the app process starts. */
  fun checkForUpdate() {
    if (checkJob?.isActive == true) return
    if (_state.value is UpdateState.Downloading) return
    checkJob = scope.launch {
      _state.value = UpdateState.Checking
      repository.latestRelease(currentVersionName, Build.SUPPORTED_ABIS.toList())
        .onSuccess { release ->
          // A download owns the state until it finishes or fails.
          if (_state.value is UpdateState.Downloading) return@onSuccess
          if (release == null) {
            prefs.clear()
            _state.value = UpdateState.UpToDate(currentVersionName)
            return@onSuccess
          }
          val stored = prefs.release()
          val path = prefs.downloadedPath()
          if (stored != null &&
            path != null &&
            stored.versionName == release.versionName &&
            File(path).isFile
          ) {
            _state.value = UpdateState.ReadyToInstall(stored, path)
            if (!visibility.visible.value) notifier.notifyReady(stored, path)
            return@onSuccess
          }
          prefs.clear()
          prefs.saveRelease(release)
          _state.value = UpdateState.Available(release)
          if (!visibility.visible.value) notifier.notifyAvailable(release)
        }
        .onFailure { error ->
          val current = _state.value
          if (current is UpdateState.Downloading || current is UpdateState.ReadyToInstall) {
            return@onFailure
          }
          _state.value = UpdateState.Failed(
            release = null,
            message = error.message,
            fromDownload = false,
          )
        }
    }
  }

  fun startDownload(release: UpdateRelease) {
    notifier.cancelAvailable()
    notifier.cancelReady()
    prefs.saveRelease(release)
    prefs.clearDownloadedPath()
    _state.value = UpdateState.Downloading(release, 0, release.sizeBytes)
    if (!UpdateService.start(context, release)) {
      _state.value = UpdateState.Failed(
        release = release,
        message = context.getString(R.string.update_error_start),
        fromDownload = true,
      )
    }
  }

  /** Opens the system installer. Returns false when it could not start. */
  fun installDownloaded(): Boolean {
    val ready = _state.value as? UpdateState.ReadyToInstall ?: return false
    if (!File(ready.apkPath).isFile) {
      prefs.clearDownloadedPath()
      _state.value = UpdateState.Failed(
        release = ready.release,
        message = context.getString(R.string.update_error_missing_file),
        fromDownload = true,
      )
      return false
    }
    val launched = installer.install(ready.apkPath)
    if (launched) notifier.cancelReady()
    return launched
  }

  fun onDownloadStarted(release: UpdateRelease) {
    if (_state.value is UpdateState.Downloading) return
    _state.value = UpdateState.Downloading(release, 0, release.sizeBytes)
  }

  fun onDownloadProgress(release: UpdateRelease, downloadedBytes: Long, totalBytes: Long) {
    val total = if (totalBytes > 0) totalBytes else release.sizeBytes
    _state.value = UpdateState.Downloading(release, downloadedBytes, total)
  }

  fun onDownloadComplete(release: UpdateRelease, apkPath: String) {
    prefs.saveRelease(release)
    prefs.saveDownloadedPath(apkPath)
    _state.value = UpdateState.ReadyToInstall(release, apkPath)
    if (!visibility.visible.value) notifier.notifyReady(release, apkPath)
  }

  fun onDownloadFailed(release: UpdateRelease, message: String?) {
    _state.value = UpdateState.Failed(release, message, fromDownload = true)
    if (!visibility.visible.value) notifier.notifyFailed(release, message)
  }

  private fun restoredState(): UpdateState {
    val release = prefs.release()
    if (release != null && AppVersion.isNewer(release.versionName, currentVersionName)) {
      val path = prefs.downloadedPath()
      if (path != null && File(path).isFile) {
        return UpdateState.ReadyToInstall(release, path)
      }
      prefs.clearDownloadedPath()
      return UpdateState.Available(release)
    }
    if (release != null) prefs.clear()
    return UpdateState.Idle
  }
}

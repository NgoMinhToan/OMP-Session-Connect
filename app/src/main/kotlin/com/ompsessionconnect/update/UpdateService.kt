package com.ompsessionconnect.update

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ompsessionconnect.domain.model.UpdateRelease
import com.ompsessionconnect.domain.repository.UpdateRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Downloads an update in the foreground so it keeps running with the app in the
 * background or swiped away. The ongoing notification doubles as progress.
 */
@AndroidEntryPoint
class UpdateService : Service() {
  @Inject lateinit var repository: UpdateRepository
  @Inject lateinit var manager: UpdateManager
  @Inject lateinit var notifier: UpdateNotifier

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var job: Job? = null

  override fun onCreate() {
    super.onCreate()
    notifier.createChannels()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val release = intent?.let(::releaseFrom)
    if (release == null) {
      stopSelf(startId)
      return START_NOT_STICKY
    }
    startAsForeground(release, downloaded = 0, total = release.sizeBytes)
    manager.onDownloadStarted(release)
    if (job?.isActive == true) return START_REDELIVER_INTENT
    job = scope.launch {
      var lastPercent = -1
      repository.download(release) { downloaded, total ->
        manager.onDownloadProgress(release, downloaded, total)
        val percent = if (total > 0) {
          ((downloaded * 100) / total).toInt().coerceIn(0, 100)
        } else {
          0
        }
        if (percent != lastPercent) {
          lastPercent = percent
          notifier.updateProgress(release, downloaded, total)
        }
      }
        .onSuccess { path -> manager.onDownloadComplete(release, path) }
        .onFailure { error -> manager.onDownloadFailed(release, error.message) }
      stopForegroundAndSelf(startId)
    }
    // Redeliver after a process death so the download resumes from its .part file.
    return START_REDELIVER_INTENT
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private fun startAsForeground(release: UpdateRelease, downloaded: Long, total: Long) {
    ServiceCompat.startForeground(
      this,
      FOREGROUND_ID,
      notifier.progressNotification(release, downloaded, total),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      } else {
        0
      },
    )
  }

  private fun stopForegroundAndSelf(startId: Int) {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    notifier.cancelProgress()
    stopSelf(startId)
  }

  private fun releaseFrom(intent: Intent): UpdateRelease? {
    val version = intent.getStringExtra(EXTRA_VERSION) ?: return null
    val url = intent.getStringExtra(EXTRA_URL) ?: return null
    val apkName = intent.getStringExtra(EXTRA_APK_NAME) ?: return null
    return UpdateRelease(
      versionName = version,
      notes = intent.getStringExtra(EXTRA_NOTES).orEmpty(),
      apkUrl = url,
      apkName = apkName,
      sizeBytes = intent.getLongExtra(EXTRA_SIZE, 0L),
    )
  }

  companion object {
    private const val FOREGROUND_ID = UpdateNotifier.PROGRESS_NOTIFICATION_ID
    private const val ACTION_DOWNLOAD = "com.ompsessionconnect.action.DOWNLOAD_UPDATE"
    private const val EXTRA_VERSION = "version"
    private const val EXTRA_NOTES = "notes"
    private const val EXTRA_URL = "url"
    private const val EXTRA_APK_NAME = "apk_name"
    private const val EXTRA_SIZE = "size"

    fun start(context: Context, release: UpdateRelease): Boolean {
      val started = runCatching {
        ContextCompat.startForegroundService(context, intent(context, release))
      }.isSuccess
      return started
    }

    fun downloadPendingIntent(context: Context, release: UpdateRelease): PendingIntent =
      PendingIntent.getForegroundService(
        context,
        release.versionName.hashCode(),
        intent(context, release),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    private fun intent(context: Context, release: UpdateRelease): Intent =
      Intent(context, UpdateService::class.java).apply {
        action = ACTION_DOWNLOAD
        putExtra(EXTRA_VERSION, release.versionName)
        putExtra(EXTRA_NOTES, release.notes)
        putExtra(EXTRA_URL, release.apkUrl)
        putExtra(EXTRA_APK_NAME, release.apkName)
        putExtra(EXTRA_SIZE, release.sizeBytes)
      }
  }
}

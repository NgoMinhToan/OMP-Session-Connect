package com.ompsessionconnect.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ompsessionconnect.MainActivity
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.UpdateRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotifier @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val installer: ApkInstaller,
) {
  private val manager = NotificationManagerCompat.from(context)

  fun createChannels() {
    val updates = NotificationChannel(
      CHANNEL_UPDATES,
      context.getString(R.string.notif_channel_update),
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = context.getString(R.string.notif_channel_update_desc)
    }
    val progress = NotificationChannel(
      CHANNEL_PROGRESS,
      context.getString(R.string.notif_channel_update_progress),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = context.getString(R.string.notif_channel_update_progress_desc)
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
    }
    context.getSystemService(NotificationManager::class.java)
      ?.createNotificationChannels(listOf(updates, progress))
  }

  @SuppressLint("MissingPermission")
  fun notifyAvailable(release: UpdateRelease) {
    if (!canNotify()) return
    val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(context.getString(R.string.notif_update_available_title))
      .setContentText(context.getString(R.string.notif_update_available_text, release.versionName))
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(context.getString(R.string.notif_update_available_text, release.versionName)),
      )
      .setContentIntent(openAppIntent())
      .addAction(
        0,
        context.getString(R.string.notif_update_action_download),
        UpdateService.downloadPendingIntent(context, release),
      )
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
      .build()
    manager.notify(NOTIFICATION_AVAILABLE, notification)
  }

  fun progressNotification(
    release: UpdateRelease,
    downloadedBytes: Long,
    totalBytes: Long,
  ): Notification {
    val percent = if (totalBytes > 0) {
      ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
    } else {
      0
    }
    val text = if (totalBytes > 0) {
      context.getString(
        R.string.notif_update_downloading_text,
        percent,
        formatBytes(downloadedBytes),
        formatBytes(totalBytes),
      )
    } else {
      context.getString(R.string.notif_update_downloading_unknown, formatBytes(downloadedBytes))
    }
    return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(
        context.getString(R.string.notif_update_downloading_title, release.versionName),
      )
      .setContentText(text)
      .setContentIntent(openAppIntent())
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setProgress(if (totalBytes > 0) 100 else 0, percent, totalBytes <= 0)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .build()
  }

  @SuppressLint("MissingPermission")
  fun updateProgress(
    release: UpdateRelease,
    downloadedBytes: Long,
    totalBytes: Long,
  ) {
    if (!canNotify()) return
    manager.notify(
      NOTIFICATION_PROGRESS,
      progressNotification(release, downloadedBytes, totalBytes),
    )
  }

  @SuppressLint("MissingPermission")
  fun notifyReady(release: UpdateRelease, apkPath: String) {
    if (!canNotify()) return
    val install = installer.installPendingIntent(apkPath)
    val text = context.getString(R.string.notif_update_ready_text, release.versionName)
    val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(context.getString(R.string.notif_update_ready_title))
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setContentIntent(install ?: openAppIntent())
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
      .build()
    manager.notify(NOTIFICATION_READY, notification)
  }

  @SuppressLint("MissingPermission")
  fun notifyFailed(release: UpdateRelease, message: String?) {
    if (!canNotify()) return
    val text = message ?: context.getString(R.string.error_generic)
    val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(
        context.getString(R.string.notif_update_failed_title, release.versionName),
      )
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setContentIntent(openAppIntent())
      .setAutoCancel(true)
      .setCategory(NotificationCompat.CATEGORY_ERROR)
      .build()
    manager.notify(NOTIFICATION_FAILED, notification)
  }

  fun cancelProgress() {
    manager.cancel(NOTIFICATION_PROGRESS)
  }

  fun cancelReady() {
    manager.cancel(NOTIFICATION_READY)
  }

  fun cancelAvailable() {
    manager.cancel(NOTIFICATION_AVAILABLE)
  }

  private fun canNotify(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED

  private fun openAppIntent(): PendingIntent =
    PendingIntent.getActivity(
      context,
      0,
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  companion object {
    /** Also the service's foreground id, so progress updates replace one notification. */
    const val PROGRESS_NOTIFICATION_ID = 2102
    private const val CHANNEL_UPDATES = "app_updates"
    private const val CHANNEL_PROGRESS = "app_update_progress"
    private const val NOTIFICATION_AVAILABLE = 2101
    private const val NOTIFICATION_PROGRESS = PROGRESS_NOTIFICATION_ID
    private const val NOTIFICATION_READY = 2103
    private const val NOTIFICATION_FAILED = 2104
  }
}

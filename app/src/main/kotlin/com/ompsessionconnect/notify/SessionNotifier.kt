package com.ompsessionconnect.notify

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
import com.ompsessionconnect.domain.model.SessionAlert
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionNotifier @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {
  private val manager = NotificationManagerCompat.from(context)

  fun createChannels() {
    val status = NotificationChannel(
      CHANNEL_STATUS,
      context.getString(R.string.notif_channel_status),
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = context.getString(R.string.notif_channel_status_desc)
    }
    val service = NotificationChannel(
      CHANNEL_SERVICE,
      context.getString(R.string.notif_channel_service),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = context.getString(R.string.notif_channel_service_desc)
      setShowBadge(false)
    }
    val live = NotificationChannel(
      CHANNEL_LIVE,
      context.getString(R.string.notif_channel_live),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = context.getString(R.string.notif_channel_live_desc)
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
    }
    context.getSystemService(NotificationManager::class.java)
      ?.createNotificationChannels(listOf(status, service, live))
  }

  fun foregroundNotification(sessions: List<SessionOverview>): Notification {
    val live = sessions.count {
      it.status == SessionStatus.LIVE ||
        it.status == SessionStatus.WORKING ||
        it.status == SessionStatus.WAITING_INPUT ||
        it.status == SessionStatus.WATCHING
    }
    val attention = sessions.count { it.needsAttention }
    return NotificationCompat.Builder(context, CHANNEL_SERVICE)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(context.getString(R.string.notif_service_title))
      .setContentText(context.getString(R.string.notif_service_text, live, attention))
      .setContentIntent(openAppIntent())
      .setOngoing(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      .build()
  }

  @SuppressLint("MissingPermission")
  fun notify(alert: SessionAlert) {
    if (!canNotify()) return
    val (title, text) = when (alert) {
      is SessionAlert.WaitingForInput ->
        context.getString(R.string.notif_waiting_title) to
          context.getString(R.string.notif_waiting_text, alert.title, alert.requestTitle)
      is SessionAlert.TurnFinished ->
        context.getString(R.string.notif_finished_title) to
          context.getString(R.string.notif_finished_text, alert.title)
      is SessionAlert.ConnectionLost ->
        context.getString(R.string.notif_lost_title) to
          context.getString(R.string.notif_lost_text, alert.title, alert.reason)
      is SessionAlert.Ended ->
        context.getString(R.string.notif_ended_title) to
          context.getString(R.string.notif_ended_text, alert.title, alert.reason)
      is SessionAlert.AgentError ->
        context.getString(R.string.notif_error_title) to
          context.getString(R.string.notif_error_text, alert.title, alert.message)
      is SessionAlert.Live -> return
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(title)
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setContentIntent(openSessionIntent(alert.sessionId))
      .setAutoCancel(true)
      .setOnlyAlertOnce(false)
      .setGroup(GROUP_SESSIONS)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
      .build()
    manager.notify(alert.sessionId.notificationId(), notification)
  }

  @SuppressLint("MissingPermission")
  fun notifyLive(
    alert: SessionAlert.Live,
    showMessage: Boolean,
    showProgress: Boolean,
  ) {
    if (!canNotify()) return
    if (!showMessage && !showProgress) return
    val progressText = when {
      alert.total <= 0 -> context.getString(R.string.notif_progress_working)
      alert.completed >= alert.total -> context.getString(R.string.notif_progress_complete)
      else -> context.getString(
        R.string.notif_progress_text,
        alert.completed,
        alert.total,
        alert.current ?: context.getString(R.string.notif_progress_working),
      )
    }
    val message = alert.message?.takeIf { it.isNotBlank() }
    val content = if (showMessage && message != null) message else progressText
    val subText = if (showMessage && showProgress && message != null && alert.total > 0) progressText else null
    val expanded = buildString {
      if (showMessage && message != null) append(message)
      if (showProgress && alert.total > 0) {
        if (isNotEmpty()) append("\n\n")
        append(progressText)
      }
      if (isEmpty()) append(progressText)
    }
    val builder = NotificationCompat.Builder(context, CHANNEL_LIVE)
      .setSmallIcon(R.drawable.ic_stat_omp)
      .setContentTitle(alert.title)
      .setContentText(content)
      .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
      .setContentIntent(openSessionIntent(alert.sessionId))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setGroup(GROUP_SESSIONS)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
    if (subText != null) builder.setSubText(subText)
    if (showProgress && alert.total > 0) {
      builder.setProgress(alert.total, alert.completed, false)
    }
    manager.notify(alert.sessionId.notificationId(), builder.build())
  }

  fun cancel(sessionId: String) {
    manager.cancel(sessionId.notificationId())
  }

  fun cancelSessions(sessions: List<SessionOverview>) {
    sessions.forEach { manager.cancel(it.id.notificationId()) }
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

  private fun openSessionIntent(sessionId: String): PendingIntent =
    PendingIntent.getActivity(
      context,
      sessionId.hashCode(),
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_SESSION_ID, sessionId)
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  private fun String.notificationId(): Int = hashCode() and 0x7FFFFFFF

  private companion object {
    const val CHANNEL_STATUS = "session_status"
    const val CHANNEL_SERVICE = "active_connections"
    const val CHANNEL_LIVE = "session_live"
    const val GROUP_SESSIONS = "omp_sessions"
  }
}

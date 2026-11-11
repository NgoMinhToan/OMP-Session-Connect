package com.ompsessionconnect.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ompsessionconnect.AppVisibility
import com.ompsessionconnect.domain.model.SessionAlert
import com.ompsessionconnect.domain.model.latestMessageText
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionStatus
import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.repository.SessionRepository
import com.ompsessionconnect.domain.repository.SettingsRepository
import com.ompsessionconnect.notify.SessionNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps live rooms connected while the app is in the background and turns
 * status transitions into notifications. The service stops itself once no
 * stored session remains.
 */
@AndroidEntryPoint
class SessionService : Service() {
  @Inject lateinit var repository: SessionRepository
  @Inject lateinit var notifier: SessionNotifier
  @Inject lateinit var visibility: AppVisibility
  @Inject lateinit var settingsRepository: SettingsRepository

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var lastSessions: List<SessionOverview> = emptyList()
  private var seenSessions = false
  private var lastSummary: String? = null
  @Volatile private var settings = AppSettings()

  override fun onCreate() {
    super.onCreate()
    startAsForeground(emptyList())
    scope.launch {
      repository.observeSessions().collectLatest { sessions ->
        onSessions(sessions)
      }
    }
    scope.launch {
      settingsRepository.settings.collect {
        settings = it
        if (!visibility.visible.value) postLiveForRunning()
      }
    }
    scope.launch {
      visibility.visible.collect { visible ->
        if (!visible) postLiveForRunning()
      }
    }
    scope.launch {
      repository.alerts.collect(::onAlert)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startAsForeground(lastSessions)
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private fun onAlert(alert: SessionAlert) {
    val visible = visibility.visible.value
    if (!settings.notificationsEnabled) {
      if (alert is SessionAlert.TurnFinished ||
        alert is SessionAlert.Ended ||
        alert is SessionAlert.ConnectionLost
      ) {
        notifier.cancel(alert.sessionId)
      }
      return
    }
    when (alert) {
      is SessionAlert.WaitingForInput -> if (!visible) notifier.notify(alert)
      is SessionAlert.Live -> if (!visible && (settings.notifyLiveMessages || settings.notifyLiveProgress)) {
        notifier.notifyLive(
          alert = alert,
          showMessage = settings.notifyLiveMessages,
          showProgress = settings.notifyLiveProgress,
        )
      }
      is SessionAlert.TurnFinished -> {
        if (visible || !settings.notifyTurnFinished) notifier.cancel(alert.sessionId)
        else notifier.notify(alert)
      }
      is SessionAlert.AgentError -> if (!visible && settings.notifyErrors) notifier.notify(alert)
      is SessionAlert.ConnectionLost, is SessionAlert.Ended -> {
        if (visible || !settings.notifyErrors) notifier.cancel(alert.sessionId)
        else notifier.notify(alert)
      }
    }
  }

  private fun postLiveForRunning() {
    if (visibility.visible.value || !settings.notificationsEnabled) return
    if (!settings.notifyLiveMessages && !settings.notifyLiveProgress) return
    lastSessions.forEach { overview ->
      val snapshot = overview.snapshot ?: return@forEach
      if (!snapshot.working) return@forEach
      notifier.notifyLive(
        alert = SessionAlert.Live(
          sessionId = overview.id,
          title = overview.title,
          message = snapshot.latestMessageText()?.let(::compactLive),
          completed = snapshot.todos?.completed ?: 0,
          total = snapshot.todos?.total ?: 0,
          current = snapshot.todos?.next?.content,
        ),
        showMessage = settings.notifyLiveMessages,
        showProgress = settings.notifyLiveProgress,
      )
    }
  }

  private fun compactLive(text: String): String {
    val singleLine = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
    return if (singleLine.length <= 220) singleLine else singleLine.take(219) + "…"
  }

  private fun onSessions(sessions: List<SessionOverview>) {
    val previous = lastSessions
    lastSessions = sessions
    if (sessions.isEmpty()) {
      if (seenSessions) {
        notifier.cancelSessions(previous)
        stopSelf()
      }
      return
    }
    seenSessions = true
    val remaining = sessions.mapTo(mutableSetOf()) { it.id }
    previous.filterNot { it.id in remaining }.forEach { notifier.cancel(it.id) }
    val live = sessions.count {
      it.status == SessionStatus.LIVE ||
        it.status == SessionStatus.WORKING ||
        it.status == SessionStatus.WAITING_INPUT ||
        it.status == SessionStatus.WATCHING
    }
    val attention = sessions.count { it.needsAttention }
    val summary = "$live:$attention"
    if (summary != lastSummary) {
      lastSummary = summary
      startAsForeground(sessions)
    }
  }

  private fun startAsForeground(sessions: List<SessionOverview>) {
    ServiceCompat.startForeground(
      this,
      FOREGROUND_ID,
      notifier.foregroundNotification(sessions),
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      } else {
        0
      },
    )
  }

  companion object {
    private const val FOREGROUND_ID = 1001

    fun start(context: Context) {
      runCatching {
        ContextCompat.startForegroundService(
          context,
          Intent(context, SessionService::class.java),
        )
      }
    }
  }
}

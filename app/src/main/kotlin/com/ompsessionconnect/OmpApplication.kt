package com.ompsessionconnect

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ompsessionconnect.domain.repository.SessionRepository
import com.ompsessionconnect.notify.SessionNotifier
import com.ompsessionconnect.update.UpdateManager
import com.ompsessionconnect.update.UpdateNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OmpApplication : Application() {
  @Inject lateinit var repository: SessionRepository
  @Inject lateinit var notifier: SessionNotifier
  @Inject lateinit var visibility: AppVisibility
  @Inject lateinit var updateNotifier: UpdateNotifier
  @Inject lateinit var updates: UpdateManager

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var startedActivities = 0

  override fun onCreate() {
    super.onCreate()
    notifier.createChannels()
    updateNotifier.createChannels()
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      override fun onActivityStarted(activity: Activity) {
        startedActivities++
        visibility.setVisible(true)
      }

      override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        visibility.setVisible(startedActivities > 0)
      }

      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
      override fun onActivityResumed(activity: Activity) = Unit
      override fun onActivityPaused(activity: Activity) = Unit
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
      override fun onActivityDestroyed(activity: Activity) = Unit
    })
    scope.launch { repository.start() }
    // Give the first activity time to report itself visible so a version check
    // that finds an update opens the in-app prompt instead of a notification.
    scope.launch {
      delay(2_000)
      updates.checkForUpdate()
    }
  }
}

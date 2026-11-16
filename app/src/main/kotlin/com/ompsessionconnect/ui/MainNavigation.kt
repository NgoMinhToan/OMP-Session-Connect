package com.ompsessionconnect.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ompsessionconnect.domain.model.UpdateState
import com.ompsessionconnect.ui.activity.rememberSessionActivityLog
import com.ompsessionconnect.ui.components.NotificationPermissionEffect
import com.ompsessionconnect.ui.detail.SessionDetailScreen
import com.ompsessionconnect.ui.scanner.QrScannerScreen
import com.ompsessionconnect.ui.settings.SettingsViewModel
import com.ompsessionconnect.ui.theme.LiveGlassBackground
import com.ompsessionconnect.ui.update.UpdateDialog
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey

@Serializable data class SessionRoute(val sessionId: String) : NavKey

@Serializable data object ScannerRoute : NavKey

@Composable
fun MainNavigation(
  openSessionId: String?,
  onOpenConsumed: () -> Unit,
  settingsViewModel: SettingsViewModel,
  appViewModel: AppViewModel = hiltViewModel(),
  onExit: () -> Unit = {},
) {
  NotificationPermissionEffect()
  val backStack = rememberNavBackStack(HomeRoute)
  val sessions by appViewModel.sessions.collectAsStateWithLifecycle()
  val updateState by settingsViewModel.updateState.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val activityEvents = rememberSessionActivityLog(sessions)
  var dismissedUpdate by rememberSaveable { mutableStateOf<String?>(null) }
  var autoInstalledUpdate by rememberSaveable { mutableStateOf<String?>(null) }

  // Installing needs a resumed activity; if the download finishes in the
  // background, wait for the next resume instead of launching from the cold.
  DisposableEffect(lifecycleOwner, updateState) {
    val ready = updateState as? UpdateState.ReadyToInstall
    fun installIfVisible() {
      if (ready == null) return
      if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
      if (autoInstalledUpdate == ready.release.versionName) return
      autoInstalledUpdate = ready.release.versionName
      settingsViewModel.installUpdate()
    }
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) installIfVisible()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    installIfVisible()
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SESSIONS.name) }
  val selectedTab = HomeTab.entries.firstOrNull { it.name == selectedTabName } ?: HomeTab.SESSIONS

  LaunchedEffect(openSessionId) {
    if (openSessionId != null) {
      backStack.add(SessionRoute(openSessionId))
      onOpenConsumed()
    }
  }

  NavDisplay(
    backStack = backStack,
    transitionSpec = {
      slideInHorizontally(tween(320)) { width -> width } togetherWith
        slideOutHorizontally(tween(320)) { width -> -width / 4 }
    },
    popTransitionSpec = {
      slideInHorizontally(tween(320)) { width -> -width / 4 } togetherWith
        slideOutHorizontally(tween(320)) { width -> width }
    },
    predictivePopTransitionSpec = { _ ->
      slideInHorizontally(tween(320)) { width -> -width / 4 } togetherWith
        slideOutHorizontally(tween(320)) { width -> width }
    },
    onBack = {
      if (backStack.size > 1) {
        backStack.removeLastOrNull()
      } else {
        onExit()
      }
    },
    entryProvider = entryProvider {
      entry<HomeRoute> {
        LiveGlassBackground(modifier = Modifier.fillMaxSize()) {
          HomeShell(
            sessions = sessions,
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTabName = tab.name },
            onOpenSession = { backStack.add(SessionRoute(it)) },
            onScan = { backStack.add(ScannerRoute) },
            activityEvents = activityEvents,
            appViewModel = appViewModel,
            settingsViewModel = settingsViewModel,
          )
        }
      }
      entry<ScannerRoute> {
        LiveGlassBackground(modifier = Modifier.fillMaxSize()) {
          QrScannerScreen(
            appViewModel = appViewModel,
            onBack = { backStack.removeLastOrNull() },
            onAdded = { sessionId ->
              backStack.removeLastOrNull()
              backStack.add(SessionRoute(sessionId))
            },
          )
        }
      }
      entry<SessionRoute> { route ->
        LiveGlassBackground(modifier = Modifier.fillMaxSize()) {
          SessionDetailScreen(
            sessionId = route.sessionId,
            sessions = sessions,
            appViewModel = appViewModel,
            onBack = { backStack.removeLastOrNull() },
          )
        }
      }
    },
  )

  val promptState = when (val state = updateState) {
    is UpdateState.Available -> state.takeIf { dismissedUpdate != it.release.versionName }
    is UpdateState.Downloading -> state.takeIf { dismissedUpdate != it.release.versionName }
    is UpdateState.ReadyToInstall -> state.takeIf { dismissedUpdate != it.release.versionName }
    is UpdateState.Failed -> state.takeIf {
      it.fromDownload && dismissedUpdate != it.release?.versionName
    }
    else -> null
  }
  if (promptState != null) {
    UpdateDialog(
      state = promptState,
      onDownload = {
        val release = when (promptState) {
          is UpdateState.Available -> promptState.release
          is UpdateState.Downloading -> promptState.release
          is UpdateState.ReadyToInstall -> promptState.release
          is UpdateState.Failed -> promptState.release
          else -> null
        }
        if (release != null) {
          dismissedUpdate = null
          settingsViewModel.downloadUpdate(release)
        }
      },
      onInstall = { settingsViewModel.installUpdate() },
      onCheck = { settingsViewModel.checkForUpdates() },
      onDismiss = {
        dismissedUpdate = when (promptState) {
          is UpdateState.Available -> promptState.release.versionName
          is UpdateState.Downloading -> promptState.release.versionName
          is UpdateState.ReadyToInstall -> promptState.release.versionName
          is UpdateState.Failed -> promptState.release?.versionName
          else -> null
        }
      },
    )
  }
}

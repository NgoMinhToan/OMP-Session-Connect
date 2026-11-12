package com.ompsessionconnect.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ompsessionconnect.ui.activity.rememberSessionActivityLog
import com.ompsessionconnect.ui.components.NotificationPermissionEffect
import com.ompsessionconnect.ui.detail.SessionDetailScreen
import com.ompsessionconnect.ui.scanner.QrScannerScreen
import com.ompsessionconnect.ui.settings.SettingsViewModel
import com.ompsessionconnect.ui.theme.LiveGlassBackground
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
  val activityEvents = rememberSessionActivityLog(sessions)
  var selectedTabName by rememberSaveable { mutableStateOf(HomeTab.SESSIONS.name) }
  val selectedTab = HomeTab.entries.firstOrNull { it.name == selectedTabName } ?: HomeTab.SESSIONS

  LaunchedEffect(openSessionId) {
    if (openSessionId != null) {
      backStack.add(SessionRoute(openSessionId))
      onOpenConsumed()
    }
  }

  LiveGlassBackground(modifier = Modifier.fillMaxSize()) {
    NavDisplay(
      backStack = backStack,
      onBack = {
        if (backStack.size > 1) {
          backStack.removeLastOrNull()
        } else {
          onExit()
        }
      },
      entryProvider = entryProvider {
        entry<HomeRoute> {
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
        entry<ScannerRoute> {
          QrScannerScreen(
            appViewModel = appViewModel,
            onBack = { backStack.removeLastOrNull() },
            onAdded = { sessionId ->
              backStack.removeLastOrNull()
              backStack.add(SessionRoute(sessionId))
            },
          )
        }
        entry<SessionRoute> { route ->
          SessionDetailScreen(
            sessionId = route.sessionId,
            sessions = sessions,
            appViewModel = appViewModel,
            onBack = { backStack.removeLastOrNull() },
          )
        }
      },
    )
  }
}

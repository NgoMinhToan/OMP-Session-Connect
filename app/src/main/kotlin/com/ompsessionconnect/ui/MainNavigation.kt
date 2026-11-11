package com.ompsessionconnect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ompsessionconnect.ui.detail.SessionDetailScreen
import com.ompsessionconnect.ui.scanner.QrScannerScreen
import com.ompsessionconnect.ui.sessions.SessionsScreen
import com.ompsessionconnect.ui.settings.SettingsScreen
import com.ompsessionconnect.ui.settings.SettingsViewModel
import com.ompsessionconnect.ui.components.NotificationPermissionEffect
import kotlinx.serialization.Serializable

@Serializable data object SessionsRoute : NavKey

@Serializable data class SessionRoute(val sessionId: String) : NavKey

@Serializable data object ScannerRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Composable
fun MainNavigation(
  openSessionId: String?,
  onOpenConsumed: () -> Unit,
  settingsViewModel: SettingsViewModel,
  appViewModel: AppViewModel = hiltViewModel(),
) {
  NotificationPermissionEffect()
  val backStack = rememberNavBackStack(SessionsRoute)
  val sessions by appViewModel.sessions.collectAsStateWithLifecycle()

  LaunchedEffect(openSessionId) {
    if (openSessionId != null) {
      backStack.add(SessionRoute(openSessionId))
      onOpenConsumed()
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
      entry<SessionsRoute> {
        SessionsScreen(
          sessions = sessions,
          onOpenSession = { backStack.add(SessionRoute(it)) },
          onScan = { backStack.add(ScannerRoute) },
          onSettings = { backStack.add(SettingsRoute) },
          appViewModel = appViewModel,
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
      entry<SettingsRoute> {
        SettingsScreen(
          viewModel = settingsViewModel,
          onBack = { backStack.removeLastOrNull() },
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

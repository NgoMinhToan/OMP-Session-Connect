package com.ompsessionconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ompsessionconnect.domain.repository.SessionRepository
import com.ompsessionconnect.service.SessionService
import com.ompsessionconnect.ui.MainNavigation
import com.ompsessionconnect.ui.settings.SettingsViewModel
import com.ompsessionconnect.ui.theme.OmpSessionConnectTheme
import androidx.core.view.WindowCompat
import com.ompsessionconnect.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @Inject lateinit var repository: SessionRepository
  private val settingsViewModel: SettingsViewModel by viewModels()

  private var openSessionId by mutableStateOf<String?>(null)
  private var hadSessions = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    openSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
    enableEdgeToEdge()

    setContent {
      val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }
      OmpSessionConnectTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
      ) {
        val view = LocalView.current
        SideEffect {
          WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
          }
        }
        MainNavigation(
          openSessionId = openSessionId,
          onOpenConsumed = { openSessionId = null },
          settingsViewModel = settingsViewModel,
          onExit = { finish() },
        )
      }
    }

    lifecycleScope.launch {
      repository.observeSessions().collect { sessions ->
        if (sessions.isNotEmpty() && !hadSessions) {
          hadSessions = true
          SessionService.start(this@MainActivity)
        } else if (sessions.isEmpty()) {
          hadSessions = false
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra(EXTRA_SESSION_ID)?.let { openSessionId = it }
  }

  companion object {
    const val EXTRA_SESSION_ID = "session_id"
  }
}

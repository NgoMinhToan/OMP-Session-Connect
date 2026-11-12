package com.ompsessionconnect.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.SessionStatus
import com.ompsessionconnect.ui.activity.ActivityScreen
import com.ompsessionconnect.ui.activity.SessionActivityEvent
import com.ompsessionconnect.ui.components.AddLinkDialog
import com.ompsessionconnect.ui.components.ConfirmDialog
import com.ompsessionconnect.ui.components.GlassIconButton
import com.ompsessionconnect.ui.sessions.SessionsScreen
import com.ompsessionconnect.ui.settings.SettingsScreen
import com.ompsessionconnect.ui.settings.SettingsViewModel
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors

enum class HomeTab(val labelRes: Int) {
  SESSIONS(R.string.sessions_title),
  ACTIVITY(R.string.activity_title),
  SETTINGS(R.string.settings_title),
}

@Composable
fun HomeShell(
  sessions: List<SessionOverview>?,
  selectedTab: HomeTab,
  onTabSelected: (HomeTab) -> Unit,
  onOpenSession: (String) -> Unit,
  onScan: () -> Unit,
  activityEvents: List<SessionActivityEvent>,
  appViewModel: AppViewModel,
  settingsViewModel: SettingsViewModel,
) {
  val tokens = LocalLiveGlass.current
  val resources = LocalResources.current
  val genericError = stringResource(R.string.error_generic)
  val sessionClosedText = stringResource(R.string.session_closed)
  val snackbarHostState = remember { SnackbarHostState() }
  var showAddDialog by rememberSaveable { mutableStateOf(false) }
  var menuOpen by remember { mutableStateOf(false) }
  var closeEndedConfirm by remember { mutableStateOf(false) }
  var closedEndedCount by remember { mutableIntStateOf(0) }
  val endedCount = sessions?.count {
    it.snapshot?.phase == SessionPhase.ENDED || it.snapshot?.phase == SessionPhase.FAILED
  } ?: 0

  LaunchedEffect(appViewModel) {
    appViewModel.events.collect { event ->
      val message = when (event) {
        is AppEvent.Error -> event.message ?: genericError
        AppEvent.SessionClosed -> sessionClosedText
        is AppEvent.ClosedEnded -> resources.getQuantityString(
          R.plurals.close_ended_count,
          event.count,
          event.count,
        )
      }
      snackbarHostState.showSnackbar(message)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        HomeTopBar(
          tab = selectedTab,
          sessions = sessions,
          menuOpen = menuOpen,
          onMenuOpen = { menuOpen = true },
          onMenuDismiss = { menuOpen = false },
          onSettings = { onTabSelected(HomeTab.SETTINGS) },
          onAddLink = { showAddDialog = true },
          onCloseEnded = {
            menuOpen = false
            closedEndedCount = endedCount
            closeEndedConfirm = true
          },
        )
      },
      bottomBar = {
        LiveGlassDock(
          selected = selectedTab,
          onSelected = onTabSelected,
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
      when (selectedTab) {
        HomeTab.SESSIONS -> SessionsScreen(
          sessions = sessions,
          onOpenSession = onOpenSession,
          onScan = onScan,
          onAddLink = { showAddDialog = true },
          appViewModel = appViewModel,
          modifier = Modifier.padding(padding),
        )
        HomeTab.ACTIVITY -> ActivityScreen(
          sessions = sessions,
          events = activityEvents,
          onOpenSession = onOpenSession,
          modifier = Modifier.padding(padding),
        )
        HomeTab.SETTINGS -> SettingsScreen(
          viewModel = settingsViewModel,
          modifier = Modifier.padding(padding),
        )
      }
    }

    if (selectedTab == HomeTab.SESSIONS && !showAddDialog && !closeEndedConfirm) {
      val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      FloatingActionButton(
        onClick = onScan,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 84.dp + navBottom)
          .size(56.dp),
        shape = CircleShape,
        containerColor = tokens.primary,
        contentColor = tokens.onPrimary,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
          defaultElevation = 10.dp,
          pressedElevation = 4.dp,
        ),
      ) {
        Icon(
          Icons.Outlined.Add,
          contentDescription = stringResource(R.string.action_scan_qr),
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }

  if (showAddDialog) {
    AddLinkDialog(
      onDismiss = { showAddDialog = false },
      onAdd = { link -> appViewModel.add(link) },
      onAdded = { sessionId ->
        showAddDialog = false
        onOpenSession(sessionId)
      },
    )
  }

  if (closeEndedConfirm) {
    ConfirmDialog(
      title = stringResource(R.string.detail_close_ended_confirm),
      body = stringResource(R.string.detail_close_ended_body, closedEndedCount),
      confirmLabel = stringResource(R.string.sessions_close_ended),
      onConfirm = {
        closeEndedConfirm = false
        appViewModel.closeEnded()
      },
      onDismiss = { closeEndedConfirm = false },
    )
  }
}

@Composable
private fun HomeTopBar(
  tab: HomeTab,
  sessions: List<SessionOverview>?,
  menuOpen: Boolean,
  onMenuOpen: () -> Unit,
  onMenuDismiss: () -> Unit,
  onSettings: () -> Unit,
  onAddLink: () -> Unit,
  onCloseEnded: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
  ) {
    when (tab) {
      HomeTab.SESSIONS -> {
        val list = sessions.orEmpty()
        val live = list.count {
          it.status == SessionStatus.LIVE ||
            it.status == SessionStatus.WORKING ||
            it.status == SessionStatus.WAITING_INPUT ||
            it.status == SessionStatus.WATCHING
        }
        val attention = list.count { it.needsAttention }
        Row(verticalAlignment = Alignment.Top) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.sessions_title),
              style = MaterialTheme.typography.titleLarge,
              color = tokens.ink,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (live > 0) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .background(status.live, CircleShape),
                )
              }
              Text(
                text = if (sessions == null || list.isEmpty()) {
                  stringResource(R.string.sessions_subtitle)
                } else {
                  "${pluralStringResource(R.plurals.sessions_live, live, live)} · " +
                    "${pluralStringResource(R.plurals.sessions_need_you, attention, attention)} · " +
                    stringResource(R.string.sessions_stored, list.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = tokens.muted,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(
              icon = Icons.Outlined.Settings,
              contentDescription = stringResource(R.string.settings_title),
              onClick = onSettings,
            )
            GlassIconButton(
              icon = Icons.Outlined.Add,
              contentDescription = stringResource(R.string.action_paste_link),
              onClick = onAddLink,
            )
            Box {
              GlassIconButton(
                icon = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.action_more),
                onClick = onMenuOpen,
              )
              DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                DropdownMenuItem(
                  text = { Text(stringResource(R.string.sessions_close_ended)) },
                  enabled = (sessions?.count {
                    it.snapshot?.phase == SessionPhase.ENDED || it.snapshot?.phase == SessionPhase.FAILED
                  } ?: 0) > 0,
                  onClick = onCloseEnded,
                )
              }
            }
          }
        }
      }
      HomeTab.ACTIVITY -> {
        Text(
          text = stringResource(R.string.activity_title),
          style = MaterialTheme.typography.headlineMedium,
          color = tokens.ink,
        )
        Text(
          text = stringResource(R.string.activity_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
        )
      }
      HomeTab.SETTINGS -> {
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.headlineMedium,
          color = tokens.ink,
        )
      }
    }
  }
}

@Composable
private fun LiveGlassDock(
  selected: HomeTab,
  onSelected: (HomeTab) -> Unit,
) {
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
    shape = LiveGlassShapes.Dock,
    color = tokens.glass,
    border = BorderStroke(1.dp, tokens.edge),
    shadowElevation = 18.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(62.dp)
        .padding(6.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HomeTab.entries.forEach { tab ->
        val active = tab == selected
        Column(
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(LiveGlassShapes.DockItem)
            .background(if (active) tokens.inkFill else Color.Transparent)
            .selectable(
              selected = active,
              role = Role.Tab,
              onClick = { onSelected(tab) },
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          val tint = if (active) tokens.onInk else tokens.muted
          when (tab) {
            HomeTab.SESSIONS -> Icon(
              painterResource(R.drawable.ic_sessions),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = tint,
            )
            HomeTab.ACTIVITY -> Icon(
              painterResource(R.drawable.ic_activity),
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = tint,
            )
            HomeTab.SETTINGS -> Icon(
              Icons.Outlined.Settings,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = tint,
            )
          }
          Spacer(Modifier.height(3.dp))
          Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            letterSpacing = 0.2.sp,
          )
        }
      }
    }
  }
}

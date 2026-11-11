@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.SessionStatus
import com.ompsessionconnect.ui.AppEvent
import com.ompsessionconnect.ui.AppViewModel
import com.ompsessionconnect.ui.components.AddLinkDialog
import com.ompsessionconnect.ui.components.ConfirmDialog
import com.ompsessionconnect.ui.components.EmptySessions
import com.ompsessionconnect.ui.components.SessionCard
import com.ompsessionconnect.ui.components.SessionCardState
import com.ompsessionconnect.ui.components.toCardState

@Composable
fun SessionsScreen(
  sessions: List<SessionOverview>?,
  onOpenSession: (String) -> Unit,
  onScan: () -> Unit,
  onSettings: () -> Unit,
  appViewModel: AppViewModel,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showAddDialog by rememberSaveable { mutableStateOf(false) }
  var menuOpen by remember { mutableStateOf(false) }
  var closeTarget by remember { mutableStateOf<SessionOverview?>(null) }
  var closeEndedConfirm by remember { mutableStateOf(false) }
  var closedEndedCount by remember { mutableIntStateOf(0) }

  val cards: List<SessionCardState> = sessions?.map { it.toCardState() }.orEmpty()
  val endedCount = sessions?.count {
    it.snapshot?.phase == SessionPhase.ENDED || it.snapshot?.phase == SessionPhase.FAILED
  } ?: 0

  LaunchedEffect(appViewModel) {
    appViewModel.events.collect { event ->
      val message = when (event) {
        is AppEvent.Error -> event.message ?: context.getString(R.string.error_generic)
        AppEvent.SessionClosed -> context.getString(R.string.session_closed)
        is AppEvent.ClosedEnded -> context.resources.getQuantityString(
          R.plurals.close_ended_count,
          event.count,
          event.count,
        )
      }
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.sessions_title))
            Text(
              text = sessions?.let { list ->
                val live = list.count {
                  it.status == SessionStatus.LIVE ||
                    it.status == SessionStatus.WORKING ||
                    it.status == SessionStatus.WAITING_INPUT ||
                    it.status == SessionStatus.WATCHING
                }
                val attention = list.count { it.needsAttention }
                if (list.isEmpty()) {
                  stringResource(R.string.sessions_subtitle)
                } else {
                  "${context.resources.getQuantityString(R.plurals.sessions_live, live, live)} · " +
                    context.resources.getQuantityString(R.plurals.sessions_need_you, attention, attention)
                }
              } ?: stringResource(R.string.sessions_subtitle),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
          }
          IconButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_paste_link))
          }
          Box {
            IconButton(onClick = { menuOpen = true }) {
              Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.sessions_close_ended)) },
                enabled = endedCount > 0,
                onClick = {
                  menuOpen = false
                  closedEndedCount = endedCount
                  closeEndedConfirm = true
                },
              )
            }
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onScan,
        icon = { Icon(painterResource(R.drawable.ic_qr_code), contentDescription = null) },
        text = { Text(stringResource(R.string.action_scan_qr)) },
      )
    },
  ) { innerPadding ->
    when {
      sessions == null -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
      sessions.isEmpty() -> {
        EmptySessions(
          onScan = onScan,
          onAddLink = { showAddDialog = true },
          modifier = Modifier.padding(innerPadding),
        )
      }
      else -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item(key = "summary", contentType = "summary") {
            SessionsSummaryCard(
              live = sessions.count {
                it.status == SessionStatus.LIVE ||
                  it.status == SessionStatus.WORKING ||
                  it.status == SessionStatus.WAITING_INPUT ||
                  it.status == SessionStatus.WATCHING
              },
              attention = sessions.count { it.needsAttention },
              stored = sessions.size,
            )
          }
          items(
            items = cards,
            key = { it.id },
            contentType = { "session" },
          ) { card ->
            SessionCard(
              state = card,
              onClick = { onOpenSession(card.id) },
              onReconnect = { appViewModel.reconnect(card.id) },
              onClose = { closeTarget = sessions.firstOrNull { it.id == card.id } },
              modifier = Modifier.animateItem(),
            )
          }
        }
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

  closeTarget?.let { target ->
    ConfirmDialog(
      title = stringResource(R.string.detail_close_confirm_title),
      body = stringResource(R.string.detail_close_confirm_body),
      confirmLabel = stringResource(R.string.action_close_session),
      onConfirm = {
        closeTarget = null
        appViewModel.close(target.id)
      },
      onDismiss = { closeTarget = null },
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
private fun SessionsSummaryCard(live: Int, attention: Int, stored: Int) {
  val container = if (attention > 0) {
    MaterialTheme.colorScheme.errorContainer
  } else {
    MaterialTheme.colorScheme.primaryContainer
  }
  val content = if (attention > 0) {
    MaterialTheme.colorScheme.onErrorContainer
  } else {
    MaterialTheme.colorScheme.onPrimaryContainer
  }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = container,
    contentColor = content,
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text(
        text = if (attention > 0) {
          pluralStringResource(R.plurals.sessions_need_you, attention, attention)
        } else {
          stringResource(R.string.sessions_all_clear)
        },
        style = MaterialTheme.typography.titleLarge,
      )
      Spacer(Modifier.height(4.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = pluralStringResource(R.plurals.sessions_live, live, live),
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = stringResource(R.string.sessions_stored, stored),
          style = MaterialTheme.typography.bodyMedium,
          color = content.copy(alpha = 0.7f),
        )
      }
    }
  }
}

package com.ompsessionconnect.ui.sessions

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.ui.AppViewModel
import com.ompsessionconnect.ui.components.ConfirmDialog
import com.ompsessionconnect.ui.components.EmptySessions
import com.ompsessionconnect.ui.components.SessionCard
import com.ompsessionconnect.ui.components.SessionCardState
import com.ompsessionconnect.ui.components.StatusPill
import com.ompsessionconnect.ui.components.toCardState
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors

@Composable
fun SessionsScreen(
  sessions: List<SessionOverview>?,
  onOpenSession: (String) -> Unit,
  onScan: () -> Unit,
  onAddLink: () -> Unit,
  appViewModel: AppViewModel,
  listState: LazyListState,
  modifier: Modifier = Modifier,
) {
  var closeTarget by remember { mutableStateOf<SessionOverview?>(null) }
  val cards = sessions?.map { it.toCardState() }.orEmpty()
  val attentionCount = sessions?.count { it.needsAttention } ?: 0
  val hero = sessions?.firstOrNull { it.needsAttention }
  val listCards = if (hero != null) cards.filterNot { it.id == hero.id } else cards

  when {
    sessions == null -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    sessions.isEmpty() -> {
      EmptySessions(
        onScan = onScan,
        onAddLink = onAddLink,
        modifier = modifier,
      )
    }
    else -> {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 156.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        hero?.let { overview ->
          item(key = "attention", contentType = "attention") {
            AttentionCard(
              overview = overview,
              state = overview.toCardState(),
              attentionCount = attentionCount,
              onClick = { onOpenSession(overview.id) },
              onReconnect = { appViewModel.reconnect(overview.id) },
              onClose = { closeTarget = overview },
            )
          }
        }
        items(
          items = listCards,
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
}

@Composable
private fun AttentionCard(
  overview: SessionOverview,
  state: SessionCardState,
  attentionCount: Int,
  onClick: () -> Unit,
  onReconnect: () -> Unit,
  onClose: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val statusColors = LocalSessionStatusColors.current
  val context = LocalContext.current
  var menuOpen by remember { mutableStateOf(false) }
  val relative = remember(state.lastActiveAt) {
    relativeTime(context, state.lastActiveAt)
  }
  val waitingTint = statusColors.waiting
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(LiveGlassShapes.Hero)
      .clickable(onClick = onClick),
    shape = LiveGlassShapes.Hero,
    color = tokens.glass,
    border = BorderStroke(1.dp, waitingTint.copy(alpha = 0.45f)),
    shadowElevation = 14.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .drawBehind {
          drawRect(
            Brush.radialGradient(
              colors = listOf(waitingTint.copy(alpha = 0.20f), Color.Transparent),
              center = Offset(size.width * 0.02f, 0f),
              radius = size.width * 0.92f,
            ),
          )
        },
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
          Column(modifier = Modifier.width(92.dp)) {
            Text(
              text = attentionCount.toString(),
              style = MaterialTheme.typography.displaySmall,
              fontWeight = FontWeight.ExtraBold,
              color = statusColors.waitingText,
            )
            Text(
              text = stringResource(R.string.sessions_needs_you).uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.4.sp,
              color = statusColors.waitingText,
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = overview.title,
              style = MaterialTheme.typography.titleSmall,
              color = tokens.ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            StatusPill(status = overview.status)
            Spacer(Modifier.height(8.dp))
            Text(
              text = state.uiRequestTitle
                ?: state.todoCurrent
                ?: stringResource(R.string.status_waiting),
              style = MaterialTheme.typography.bodyMedium,
              color = tokens.ink,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
              Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.action_more),
                tint = tokens.muted,
              )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_reconnect)) },
                leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onReconnect()
                },
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_close_session)) },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  onClose()
                },
              )
            }
          }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.sessions_waiting, relative),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.muted,
          )
          Spacer(Modifier.weight(1f))
          Button(
            onClick = onClick,
            shape = LiveGlassShapes.Pill,
            colors = ButtonDefaults.buttonColors(
              containerColor = tokens.primary,
              contentColor = tokens.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
          ) {
            Text(stringResource(R.string.sessions_review), style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

private fun relativeTime(context: android.content.Context, timestamp: Long): String {
  val age = System.currentTimeMillis() - timestamp
  if (age < 60_000) return context.getString(R.string.sessions_just_now)
  return DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
  ).toString()
}

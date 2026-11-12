package com.ompsessionconnect.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionStatus
import com.ompsessionconnect.ui.theme.CodeTextStyle
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors

/** Flat, stable inputs so a streaming background room does not recompose every card. */
data class SessionCardState(
  val id: String,
  val title: String,
  val cwd: String?,
  val roomId: String,
  val readOnly: Boolean,
  val status: SessionStatus,
  val working: Boolean,
  val modelName: String?,
  val contextPercent: Int?,
  val queuedMessages: Int,
  val needsAttention: Boolean,
  val uiRequestTitle: String?,
  val loadingReceived: Int,
  val loadingTotal: Int,
  val lastActiveAt: Long,
  val participantCount: Int,
  val activeAgents: Int,
  val todoCompleted: Int,
  val todoTotal: Int,
  val todoCurrent: String?,
)

fun SessionOverview.toCardState() = SessionCardState(
  id = id,
  title = title,
  cwd = summary.cwd,
  roomId = summary.roomId,
  readOnly = summary.readOnly || snapshot?.readOnly == true,
  status = status,
  working = snapshot?.working == true,
  modelName = snapshot?.modelName,
  contextPercent = snapshot?.contextPercent,
  queuedMessages = snapshot?.queuedMessages ?: 0,
  needsAttention = needsAttention,
  uiRequestTitle = snapshot?.uiRequest?.title,
  loadingReceived = snapshot?.loadingReceived ?: 0,
  loadingTotal = snapshot?.loadingTotal ?: 0,
  lastActiveAt = snapshot?.lastUpdated ?: summary.lastActiveAt,
  participantCount = snapshot?.participantCount ?: 0,
  activeAgents = snapshot?.activeAgents ?: 0,
  todoCompleted = snapshot?.todos?.completed ?: 0,
  todoTotal = snapshot?.todos?.total ?: 0,
  todoCurrent = snapshot?.todos?.next?.content,
)

@Composable
fun SessionCard(
  state: SessionCardState,
  onClick: () -> Unit,
  onReconnect: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalLiveGlass.current
  val statusColors = LocalSessionStatusColors.current
  var menuOpen by remember { mutableStateOf(false) }
  val justNow = stringResource(R.string.sessions_just_now)
  val relative = remember(state.lastActiveAt, justNow) {
    val age = System.currentTimeMillis() - state.lastActiveAt
    if (age < 60_000) {
      justNow
    } else {
      DateUtils.getRelativeTimeSpanString(
        state.lastActiveAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      ).toString()
    }
  }
  val accent = statusColor(state.status)


  Surface(
    modifier = modifier
      .fillMaxWidth()
      .alpha(if (state.status == SessionStatus.ENDED) 0.82f else 1f)
      .clickable(onClick = onClick),
    shape = LiveGlassShapes.Room,
    color = tokens.glass,
    border = BorderStroke(
      1.dp,
      if (state.needsAttention) statusColors.waiting.copy(alpha = 0.55f) else tokens.edge,
    ),
    shadowElevation = 6.dp,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(accent.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initials(state.title),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = accent,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.Top) {
          Text(
            text = state.title,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            color = tokens.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Box {
            IconButton(
              onClick = { menuOpen = true },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.action_more),
                modifier = Modifier.size(18.dp),
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
        if (state.modelName != null || state.contextPercent != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            state.modelName?.let {
              Text(
                text = it,
                style = CodeTextStyle,
                color = tokens.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
            }
            state.contextPercent?.let {
              Text(
                text = stringResource(R.string.sessions_context_percent, it),
                style = CodeTextStyle,
                color = tokens.muted,
                maxLines = 1,
              )
            }
          }
        }
        Spacer(Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          StatusDot(status = state.status, color = accent)
          Text(
            text = statusLabel(state.status),
            style = MaterialTheme.typography.labelSmall,
            color = if (state.needsAttention) statusColors.waitingText else statusTextColor(state.status),
          )
          if (state.readOnly) {
            Icon(
              Icons.Outlined.Lock,
              contentDescription = stringResource(R.string.status_read_only),
              tint = tokens.muted,
              modifier = Modifier.size(13.dp),
            )
          }
          if (state.todoTotal > 0) {
            Text(
              text = stringResource(R.string.sessions_todo_meta, state.todoCompleted, state.todoTotal),
              style = MaterialTheme.typography.labelSmall,
              color = tokens.primary,
              maxLines = 1,
            )
          }
          Spacer(Modifier.weight(1f))
          Text(
            text = relative,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.muted,
            maxLines = 1,
          )
        }
        if (state.queuedMessages > 0) {
          Text(
            text = pluralStringResource(
              R.plurals.queued_messages,
              state.queuedMessages,
              state.queuedMessages,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.muted,
          )
        }
        if (state.needsAttention && !state.uiRequestTitle.isNullOrBlank()) {
          Spacer(Modifier.height(4.dp))
          Text(
            text = state.uiRequestTitle,
            style = MaterialTheme.typography.bodySmall,
            color = statusColors.waitingText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        val syncing = state.status == SessionStatus.CONNECTING && state.loadingTotal > 0
        if (syncing) {
          Spacer(Modifier.height(8.dp))
          LinearProgressIndicator(
            progress = {
              (state.loadingReceived.toFloat() / state.loadingTotal.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
            color = tokens.primary,
            trackColor = tokens.hairline,
          )
        } else {
          AnimatedVisibility(visible = state.working) {
            Column {
              Spacer(Modifier.height(8.dp))
              LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = tokens.hairline,
              )
            }
          }
        }
      }
    }
  }
}

private fun initials(title: String): String {
  val parts = title.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
  if (parts.size >= 2) return (parts[0].take(1) + parts[1].take(1)).uppercase()
  return title.take(2).uppercase()
}

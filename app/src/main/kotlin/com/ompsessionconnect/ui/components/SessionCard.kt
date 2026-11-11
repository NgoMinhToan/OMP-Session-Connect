package com.ompsessionconnect.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionStatus

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
)

@Composable
fun SessionCard(
  state: SessionCardState,
  onClick: () -> Unit,
  onReconnect: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var menuOpen by remember { mutableStateOf(false) }
  val status = state.status
  val attention = state.needsAttention
  val context = LocalContext.current
  val relative = remember(state.lastActiveAt) {
    val age = System.currentTimeMillis() - state.lastActiveAt
    if (age < 60_000) {
      context.getString(R.string.sessions_just_now)
    } else {
      DateUtils.getRelativeTimeSpanString(
        state.lastActiveAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      ).toString()
    }
  }

  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    border = if (attention) {
      BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    } else {
      null
    },
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = state.cwd ?: state.roomId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(onClick = { menuOpen = true }) {
          Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
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

      Spacer(Modifier.height(12.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatusPill(status = status)
        if (state.readOnly) {
          Icon(
            Icons.Outlined.Lock,
            contentDescription = stringResource(R.string.status_read_only),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = stringResource(R.string.status_read_only),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.weight(1f))
        Text(
          text = stringResource(R.string.sessions_last_active, relative),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (state.modelName != null || state.contextPercent != null || (state.queuedMessages) > 0) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          state.modelName?.let {
            Text(
              text = it,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
          }
          state.contextPercent?.let {
            Text(
              text = "$it%",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if ((state.queuedMessages) > 0) {
            Text(
              text = pluralStringResource(R.plurals.queued_messages, state.queuedMessages, state.queuedMessages),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
            )
          }
        }
      }

      val syncing = status == SessionStatus.CONNECTING && state.loadingTotal > 0
      if (syncing) {
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
          progress = {
            (state.loadingReceived.toFloat() / state.loadingTotal.toFloat()).coerceIn(0f, 1f)
          },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = stringResource(
            R.string.status_syncing,
            state.loadingReceived,
            state.loadingTotal,
          ),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        AnimatedVisibility(visible = state.working) {
          Column {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }
        }
      }
      if (status == SessionStatus.WAITING_INPUT) {
        Spacer(Modifier.height(8.dp))
        Text(
          text = state.uiRequestTitle ?: stringResource(R.string.status_waiting),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

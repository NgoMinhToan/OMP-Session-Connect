package com.ompsessionconnect.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionStatus

fun statusLabelRes(status: SessionStatus): Int = when (status) {
  SessionStatus.CONNECTING -> R.string.status_connecting
  SessionStatus.LIVE -> R.string.status_live
  SessionStatus.WORKING -> R.string.status_working
  SessionStatus.WAITING_INPUT -> R.string.status_waiting
  SessionStatus.WATCHING -> R.string.status_watching
  SessionStatus.RECONNECTING -> R.string.status_reconnecting
  SessionStatus.ENDED -> R.string.status_ended
  SessionStatus.ERROR -> R.string.status_error
}

@Composable
fun statusLabel(status: SessionStatus): String = stringResource(statusLabelRes(status))

@Composable
fun statusColor(status: SessionStatus): Color = when (status) {
  SessionStatus.CONNECTING, SessionStatus.RECONNECTING -> MaterialTheme.colorScheme.tertiary
  SessionStatus.LIVE -> MaterialTheme.colorScheme.primary
  SessionStatus.WORKING -> MaterialTheme.colorScheme.tertiary
  SessionStatus.WAITING_INPUT -> MaterialTheme.colorScheme.error
  SessionStatus.WATCHING -> MaterialTheme.colorScheme.secondary
  SessionStatus.ENDED -> MaterialTheme.colorScheme.outline
  SessionStatus.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
fun StatusPill(
  status: SessionStatus,
  modifier: Modifier = Modifier,
) {
  val color = statusColor(status)
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = color.copy(alpha = 0.14f),
    contentColor = color,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatusDot(status = status, color = color)
      Text(text = statusLabel(status), style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
fun StatusDot(
  status: SessionStatus,
  modifier: Modifier = Modifier,
  color: Color = statusColor(status),
) {
  val pulsing = status == SessionStatus.WORKING ||
    status == SessionStatus.CONNECTING ||
    status == SessionStatus.RECONNECTING ||
    status == SessionStatus.WAITING_INPUT
  if (pulsing) {
    val transition = rememberInfiniteTransition(label = "status-pulse")
    val alpha by transition.animateFloat(
      initialValue = 0.35f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 900),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "status-pulse-alpha",
    )
    Dot(
      modifier = modifier.graphicsLayer { this.alpha = alpha },
      color = color,
    )
  } else {
    Dot(modifier = modifier, color = color)
  }
}

@Composable
private fun Dot(modifier: Modifier, color: Color) {
  Box(
    modifier = modifier
      .size(8.dp)
      .clip(CircleShape)
      .background(color),
  )
}

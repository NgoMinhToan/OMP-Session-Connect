package com.ompsessionconnect.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass

@Composable
fun EmptySessions(
  onScan: () -> Unit,
  onAddLink: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalLiveGlass.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = LiveGlassShapes.Dialog,
      color = tokens.glass,
      border = BorderStroke(1.dp, tokens.edge),
      shadowElevation = 0.dp,
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .background(tokens.primary.copy(alpha = 0.13f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            tint = tokens.primary,
          )
        }
        Spacer(Modifier.height(18.dp))
        Text(
          text = stringResource(R.string.empty_title),
          style = MaterialTheme.typography.headlineSmall,
          color = tokens.ink,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.empty_body),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
          onClick = onScan,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = LiveGlassShapes.Pill,
          colors = ButtonDefaults.buttonColors(
            containerColor = tokens.primary,
            contentColor = tokens.onPrimary,
          ),
        ) {
          Icon(painterResource(R.drawable.ic_qr_code), contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text(stringResource(R.string.action_scan_qr))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
          onClick = onAddLink,
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = LiveGlassShapes.Pill,
          border = BorderStroke(1.dp, tokens.edge),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.primary),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text(stringResource(R.string.action_paste_link))
        }
      }
    }
  }
}

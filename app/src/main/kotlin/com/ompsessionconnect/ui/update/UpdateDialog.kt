package com.ompsessionconnect.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.UpdateState
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass

/**
 * In-app update prompt. Downloading and install states stay here as well so the
 * user can watch progress or finish installing without opening Settings.
 */
@Composable
fun UpdateDialog(
  state: UpdateState,
  onDownload: () -> Unit,
  onInstall: () -> Unit,
  onCheck: () -> Unit,
  onDismiss: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val title: String
  val body: @Composable () -> Unit
  val confirmLabel: String?
  val confirm: (() -> Unit)?
  val dismissLabel: String

  when (state) {
    is UpdateState.Available -> {
      title = stringResource(R.string.update_dialog_available_title)
      body = {
        Column {
          Text(
            stringResource(R.string.update_dialog_available_body, state.release.versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
          )
        }
      }
      confirmLabel = stringResource(R.string.update_dialog_download)
      confirm = onDownload
      dismissLabel = stringResource(R.string.update_dialog_later)
    }
    is UpdateState.Downloading -> {
      title = stringResource(R.string.update_dialog_downloading_title)
      body = {
        Column {
          Text(
            stringResource(
              R.string.update_dialog_downloading_body,
              state.release.versionName,
              state.percent,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
          )
          Spacer(Modifier.height(10.dp))
          LinearProgressIndicator(
            progress = { state.fraction },
            modifier = Modifier.fillMaxWidth(),
            color = tokens.primary,
            trackColor = tokens.hairline,
          )
        }
      }
      confirmLabel = null
      confirm = null
      dismissLabel = stringResource(R.string.update_dialog_hide)
    }
    is UpdateState.ReadyToInstall -> {
      title = stringResource(R.string.update_dialog_ready_title)
      body = {
        Text(
          stringResource(R.string.update_dialog_ready_body, state.release.versionName),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
        )
      }
      confirmLabel = stringResource(R.string.update_dialog_install)
      confirm = onInstall
      dismissLabel = stringResource(R.string.update_dialog_later)
    }
    is UpdateState.Failed -> {
      title = stringResource(R.string.update_dialog_failed_title)
      body = {
        Text(
          state.message ?: stringResource(R.string.error_generic),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
        )
      }
      confirmLabel = if (state.release != null) {
        stringResource(R.string.update_dialog_retry)
      } else {
        stringResource(R.string.settings_update_check)
      }
      confirm = if (state.release != null) onDownload else onCheck
      dismissLabel = stringResource(R.string.action_close)
    }
    else -> return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = LiveGlassShapes.Dialog,
    containerColor = tokens.sheetOpaque,
    titleContentColor = tokens.ink,
    textContentColor = tokens.muted,
    title = {
      Text(title, style = MaterialTheme.typography.titleMedium)
    },
    text = body,
    confirmButton = {
      if (confirm != null && confirmLabel != null) {
        TextButton(onClick = confirm) {
          Text(confirmLabel, color = tokens.primary)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(dismissLabel)
      }
    },
  )
}

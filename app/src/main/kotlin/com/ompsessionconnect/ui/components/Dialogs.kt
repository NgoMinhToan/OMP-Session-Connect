package com.ompsessionconnect.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionSummary
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import kotlinx.coroutines.launch

@Composable
fun AddLinkDialog(
  onDismiss: () -> Unit,
  onAdd: suspend (String) -> Result<SessionSummary>,
  onAdded: (String) -> Unit,
) {
  var link by rememberSaveable { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val duplicateMessage = stringResource(R.string.add_link_duplicate)
  val invalidMessage = stringResource(R.string.add_link_error)

  fun submit() {
    val value = link.trim()
    if (value.isEmpty() || loading) return
    loading = true
    error = null
    scope.launch {
      onAdd(value)
        .onSuccess { summary ->
          loading = false
          onAdded(summary.id)
        }
        .onFailure { throwable ->
          loading = false
          val message = throwable.message.orEmpty()
          error = if (message.contains("already", ignoreCase = true)) {
            duplicateMessage
          } else {
            message.ifBlank { invalidMessage }
          }
        }
    }
  }

  val tokens = LocalLiveGlass.current
  AlertDialog(
    onDismissRequest = { if (!loading) onDismiss() },
    shape = LiveGlassShapes.Dialog,
    containerColor = tokens.sheetOpaque,
    titleContentColor = tokens.ink,
    textContentColor = tokens.muted,
    title = {
      Text(
        stringResource(R.string.add_link_title),
        style = MaterialTheme.typography.titleMedium,
      )
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = stringResource(R.string.empty_body),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
          value = link,
          onValueChange = {
            link = it
            error = null
          },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.add_link_label)) },
          placeholder = { Text(stringResource(R.string.add_link_hint)) },
          singleLine = false,
          minLines = 2,
          maxLines = 4,
          isError = error != null,
          supportingText = if (error != null) {
            { Text(error!!) }
          } else {
            null
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          shape = com.ompsessionconnect.ui.theme.LiveGlassShapes.Room,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = tokens.primary,
            unfocusedBorderColor = tokens.muted.copy(alpha = 0.45f),
            cursorColor = tokens.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
          ),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = ::submit, enabled = link.isNotBlank() && !loading) {
        if (loading) {
          CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
        } else {
          Text(stringResource(R.string.add_link_button))
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !loading) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
fun ConfirmDialog(
  title: String,
  body: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = LiveGlassShapes.Dialog,
    containerColor = tokens.sheetOpaque,
    titleContentColor = tokens.ink,
    textContentColor = tokens.muted,
    title = { Text(title, style = MaterialTheme.typography.titleMedium) },
    text = { Text(body) },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(confirmLabel) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
    },
  )
}


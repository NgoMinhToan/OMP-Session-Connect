@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.ThemeMode

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onBack: () -> Unit,
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
  val lifecycleOwner = LocalLifecycleOwner.current
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { notificationsAllowed = it || notificationsAllowed(context) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) notificationsAllowed = notificationsAllowed(context)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
      )
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
    ) {
      item(key = "appearance", contentType = "header") {
        SettingsHeader(stringResource(R.string.settings_appearance))
      }
      items(ThemeMode.entries, key = { it.name }, contentType = { "theme" }) { mode ->
        ThemeRow(
          mode = mode,
          selected = settings.themeMode == mode,
          onSelect = { viewModel.setTheme(mode) },
        )
      }

      item(key = "notif-header", contentType = "header") {
        SettingsHeader(stringResource(R.string.settings_notifications))
      }
      item(key = "master", contentType = "toggle") {
        ToggleRow(
          title = stringResource(R.string.settings_notifications_master),
          description = stringResource(R.string.settings_notifications_master_desc),
          checked = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotificationsEnabled,
        )
      }
      item(key = "finished", contentType = "toggle") {
        ToggleRow(
          title = stringResource(R.string.settings_notify_finished),
          description = stringResource(R.string.settings_notify_finished_desc),
          checked = settings.notifyTurnFinished,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyTurnFinished,
        )
      }
      item(key = "errors", contentType = "toggle") {
        ToggleRow(
          title = stringResource(R.string.settings_notify_errors),
          description = stringResource(R.string.settings_notify_errors_desc),
          checked = settings.notifyErrors,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyErrors,
        )
      }
      item(key = "progress", contentType = "toggle") {
        ToggleRow(
          title = stringResource(R.string.settings_notify_progress),
          description = stringResource(R.string.settings_notify_progress_desc),
          checked = settings.notifyLiveProgress,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyLiveProgress,
        )
      }

      item(key = "liveMessages", contentType = "toggle") {
        ToggleRow(
          title = stringResource(R.string.settings_notify_live_messages),
          description = stringResource(R.string.settings_notify_live_messages_desc),
          checked = settings.notifyLiveMessages,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyLiveMessages,
        )
      }

      item(key = "permission", contentType = "permission") {
        NotificationPermissionRow(
          allowed = notificationsAllowed,
          onRequest = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                  .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
              )
            }
          },
          onOpenSettings = {
            context.startActivity(
              Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun SettingsHeader(text: String) {
  Text(
    text = text,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
}

@Composable
private fun ThemeRow(
  mode: ThemeMode,
  selected: Boolean,
  onSelect: () -> Unit,
) {
  val (title, description) = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system to R.string.settings_theme_system_desc
    ThemeMode.LIGHT -> R.string.settings_theme_light to R.string.settings_theme_light_desc
    ThemeMode.DARK -> R.string.settings_theme_dark to R.string.settings_theme_dark_desc
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectable(
        selected = selected,
        role = Role.RadioButton,
        onClick = onSelect,
      )
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    Column(modifier = Modifier.padding(start = 8.dp)) {
      Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
      Text(
        stringResource(description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ToggleRow(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Switch,
        onValueChange = onCheckedChange,
      )
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled,
    )
  }
}

@Composable
private fun NotificationPermissionRow(
  allowed: Boolean,
  onRequest: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Text(
      stringResource(R.string.settings_permission_title),
      style = MaterialTheme.typography.titleSmall,
    )
    Text(
      text = stringResource(
        if (allowed) R.string.settings_permission_granted else R.string.settings_permission_denied,
      ),
      style = MaterialTheme.typography.bodySmall,
      color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    if (!allowed) {
      Spacer(Modifier.height(4.dp))
      Text(
        stringResource(R.string.settings_permission_body),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRequest) {
          Text(stringResource(R.string.settings_permission_allow))
        }
        TextButton(onClick = onOpenSettings) {
          Text(stringResource(R.string.settings_permission_open))
        }
      }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
  }
}

private fun notificationsAllowed(context: android.content.Context): Boolean {
  if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
  return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
}

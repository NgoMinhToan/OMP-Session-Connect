@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.ThemeMode
import com.ompsessionconnect.domain.model.UpdateRelease
import com.ompsessionconnect.domain.model.UpdateState
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors
import com.ompsessionconnect.update.formatBytes

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  listState: LazyListState,
  topInset: Dp = 0.dp,
  bottomInset: Dp = 0.dp,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalLiveGlass.current
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val updateState by viewModel.updateState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
  val lifecycleOwner = LocalLifecycleOwner.current
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { notificationsAllowed = it || notificationsAllowed(context) }
  val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) notificationsAllowed = notificationsAllowed(context)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    state = listState,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset, bottom = bottomInset),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item(key = "appearance-header", contentType = "header") {
      SettingsSectionLabel(stringResource(R.string.settings_appearance))
    }
    item(key = "appearance-panel", contentType = "panel") {
      SettingsPanel {
        ThemeMode.entries.forEach { mode ->
          ThemeRow(
            mode = mode,
            selected = settings.themeMode == mode,
            onSelect = { viewModel.setTheme(mode) },
          )
          GlassDivider()
        }
        ToggleRow(
          title = stringResource(R.string.settings_dynamic_color),
          description = if (dynamicSupported) {
            stringResource(R.string.settings_dynamic_color_desc)
          } else {
            stringResource(R.string.settings_dynamic_color_unsupported)
          },
          checked = settings.dynamicColor && dynamicSupported,
          enabled = dynamicSupported,
          onCheckedChange = viewModel::setDynamicColor,
        )
      }
    }

    item(key = "notifications-header", contentType = "header") {
      SettingsSectionLabel(stringResource(R.string.settings_notifications))
    }
    item(key = "notifications-panel", contentType = "panel") {
      SettingsPanel {
        ToggleRow(
          title = stringResource(R.string.settings_notifications_master),
          description = stringResource(R.string.settings_notifications_master_desc),
          checked = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotificationsEnabled,
        )
        GlassDivider()
        ToggleRow(
          title = stringResource(R.string.settings_notify_finished),
          description = stringResource(R.string.settings_notify_finished_desc),
          checked = settings.notifyTurnFinished,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyTurnFinished,
        )
        GlassDivider()
        ToggleRow(
          title = stringResource(R.string.settings_notify_errors),
          description = stringResource(R.string.settings_notify_errors_desc),
          checked = settings.notifyErrors,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyErrors,
        )
        GlassDivider()
        ToggleRow(
          title = stringResource(R.string.settings_notify_live_messages),
          description = stringResource(R.string.settings_notify_live_messages_desc),
          checked = settings.notifyLiveMessages,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyLiveMessages,
        )
        GlassDivider()
        ToggleRow(
          title = stringResource(R.string.settings_notify_progress),
          description = stringResource(R.string.settings_notify_progress_desc),
          checked = settings.notifyLiveProgress,
          enabled = settings.notificationsEnabled,
          onCheckedChange = viewModel::setNotifyLiveProgress,
        )
      }
    }

    item(key = "permission-header", contentType = "header") {
      SettingsSectionLabel(stringResource(R.string.settings_permission_section))
    }
    item(key = "permission-panel", contentType = "panel") {
      SettingsPanel {
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

    item(key = "update-header", contentType = "header") {
      SettingsSectionLabel(stringResource(R.string.settings_update))
    }
    item(key = "update-panel", contentType = "panel") {
      SettingsPanel {
        UpdatePanel(
          currentVersionName = viewModel.currentVersionName,
          currentVersionCode = viewModel.currentVersionCode,
          buildType = viewModel.buildType,
          state = updateState,
          onCheck = viewModel::checkForUpdates,
          onDownload = viewModel::downloadUpdate,
          onInstall = viewModel::installUpdate,
        )
      }
    }
  }
}

@Composable
private fun SettingsSectionLabel(text: String) {
  Text(
    text = text.uppercase(),
    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 0.dp),
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
    color = LocalLiveGlass.current.muted,
  )
}

@Composable
private fun SettingsPanel(content: @Composable () -> Unit) {
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = LiveGlassShapes.Panel,
    color = tokens.glass,
    border = BorderStroke(1.dp, tokens.edge),
    shadowElevation = 0.dp,
  ) {
    Column { content() }
  }
}

@Composable
private fun GlassDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(start = 16.dp),
    thickness = 1.dp,
    color = LocalLiveGlass.current.hairline,
  )
}

@Composable
private fun ThemeRow(
  mode: ThemeMode,
  selected: Boolean,
  onSelect: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val (title, description) = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system to R.string.settings_theme_system_desc
    ThemeMode.LIGHT -> R.string.settings_theme_light to R.string.settings_theme_light_desc
    ThemeMode.DARK -> R.string.settings_theme_dark to R.string.settings_theme_dark_desc
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(
      selected = selected,
      onClick = null,
      colors = RadioButtonDefaults.colors(
        selectedColor = tokens.primary,
        unselectedColor = tokens.muted,
      ),
    )
    Column(modifier = Modifier.padding(start = 10.dp)) {
      Text(
        stringResource(title),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = tokens.ink,
      )
      Text(
        stringResource(description),
        style = MaterialTheme.typography.bodySmall,
        color = tokens.muted,
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
  val tokens = LocalLiveGlass.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Switch,
        onValueChange = onCheckedChange,
      )
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = tokens.ink,
      )
      Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = tokens.muted,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled,
      colors = SwitchDefaults.colors(
        checkedThumbColor = tokens.onPrimary,
        checkedTrackColor = tokens.primary,
        checkedBorderColor = tokens.primary,
        uncheckedThumbColor = tokens.muted,
        uncheckedTrackColor = tokens.hairline,
        uncheckedBorderColor = tokens.muted.copy(alpha = 0.5f),
      ),
    )
  }
}

@Composable
private fun NotificationPermissionRow(
  allowed: Boolean,
  onRequest: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
    Text(
      stringResource(R.string.settings_permission_title),
      style = MaterialTheme.typography.titleSmall,
      color = tokens.ink,
    )
    Text(
      text = stringResource(
        if (allowed) R.string.settings_permission_granted else R.string.settings_permission_denied,
      ),
      style = MaterialTheme.typography.bodySmall,
      color = if (allowed) status.liveText else status.errorText,
    )
    if (!allowed) {
      Spacer(Modifier.height(4.dp))
      Text(
        stringResource(R.string.settings_permission_body),
        style = MaterialTheme.typography.bodySmall,
        color = tokens.muted,
      )
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRequest, shape = LiveGlassShapes.Pill) {
          Text(stringResource(R.string.settings_permission_allow))
        }
        TextButton(onClick = onOpenSettings) {
          Text(stringResource(R.string.settings_permission_open))
        }
      }
    } else {
      TextButton(
        onClick = onOpenSettings,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
      ) {
        Text(stringResource(R.string.settings_permission_open))
      }
    }
  }
}

@Composable
private fun UpdatePanel(
  currentVersionName: String,
  currentVersionCode: Int,
  buildType: String,
  state: UpdateState,
  onCheck: () -> Unit,
  onDownload: (UpdateRelease) -> Unit,
  onInstall: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          stringResource(R.string.settings_version_title),
          style = MaterialTheme.typography.titleSmall,
          color = tokens.ink,
        )
        Text(
          stringResource(
            R.string.settings_version_value,
            currentVersionName,
            currentVersionCode,
          ),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
        )
      }
      Text(
        stringResource(R.string.settings_version_build, buildType),
        style = MaterialTheme.typography.labelMedium,
        color = tokens.muted,
      )
    }
    Spacer(Modifier.height(10.dp))
    when (state) {
      UpdateState.Idle -> {
        Text(
          stringResource(R.string.settings_update_idle),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCheck, shape = LiveGlassShapes.Pill) {
          Text(stringResource(R.string.settings_update_check))
        }
      }
      UpdateState.Checking -> Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = tokens.primary,
        )
        Text(
          stringResource(R.string.settings_update_checking),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
        )
      }
      is UpdateState.UpToDate -> {
        Text(
          stringResource(R.string.settings_update_up_to_date),
          style = MaterialTheme.typography.bodySmall,
          color = status.liveText,
        )
        TextButton(onClick = onCheck, contentPadding = PaddingValues(0.dp)) {
          Text(stringResource(R.string.settings_update_check))
        }
      }
      is UpdateState.Available -> {
        Text(
          stringResource(R.string.settings_update_available, state.release.versionName),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.ink,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(onClick = { onDownload(state.release) }, shape = LiveGlassShapes.Pill) {
            Text(stringResource(R.string.settings_update_download))
          }
          TextButton(onClick = onCheck) {
            Text(stringResource(R.string.settings_update_check))
          }
        }
      }
      is UpdateState.Downloading -> {
        Text(
          stringResource(R.string.settings_update_downloading, state.percent),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.ink,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { state.fraction },
          modifier = Modifier.fillMaxWidth(),
          color = tokens.primary,
          trackColor = tokens.hairline,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          stringResource(
            R.string.settings_update_downloading_size,
            formatBytes(state.downloadedBytes),
            formatBytes(state.totalBytes),
          ),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
        )
      }
      is UpdateState.ReadyToInstall -> {
        Text(
          stringResource(R.string.settings_update_ready, state.release.versionName),
          style = MaterialTheme.typography.bodySmall,
          color = status.liveText,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onInstall, shape = LiveGlassShapes.Pill) {
          Text(stringResource(R.string.settings_update_install))
        }
      }
      is UpdateState.Failed -> {
        Text(
          stringResource(
            R.string.settings_update_failed,
            state.message ?: stringResource(R.string.error_generic),
          ),
          style = MaterialTheme.typography.bodySmall,
          color = status.errorText,
        )
        Spacer(Modifier.height(8.dp))
        val retryRelease = state.release
        if (retryRelease != null) {
          Button(
            onClick = { onDownload(retryRelease) },
            shape = LiveGlassShapes.Pill,
          ) {
            Text(stringResource(R.string.action_retry))
          }
        } else {
          Button(onClick = onCheck, shape = LiveGlassShapes.Pill) {
            Text(stringResource(R.string.settings_update_check))
          }
        }
      }
    }
  }
}

private fun notificationsAllowed(context: android.content.Context): Boolean {
  if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
  return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED
}

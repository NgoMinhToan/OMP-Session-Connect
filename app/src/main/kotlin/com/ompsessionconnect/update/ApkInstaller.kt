package com.ompsessionconnect.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Hands a downloaded APK to the system package installer. */
@Singleton
class ApkInstaller @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {
  /**
   * Opens the installer. Returns false when the APK is missing or when the
   * user must first allow installs from this app (system settings are opened).
   */
  fun install(apkPath: String): Boolean {
    val intent = installIntent(apkPath) ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      !context.packageManager.canRequestPackageInstalls()
    ) {
      openInstallPermissionSettings()
      return false
    }
    return runCatching {
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }

  /** PendingIntent that opens the installer directly from a notification tap. */
  fun installPendingIntent(apkPath: String): PendingIntent? {
    val intent = installIntent(apkPath) ?: return null
    return PendingIntent.getActivity(
      context,
      apkPath.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun installIntent(apkPath: String): Intent? {
    val apk = File(apkPath)
    if (!apk.isFile || apk.length() <= 0) return null
    val uri: Uri = runCatching {
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
    }.getOrNull() ?: return null
    return Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, APK_MIME_TYPE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  private fun openInstallPermissionSettings() {
    runCatching {
      context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
      )
    }
  }

  private companion object {
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
  }
}

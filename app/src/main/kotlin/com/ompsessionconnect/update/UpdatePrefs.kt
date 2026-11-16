package com.ompsessionconnect.update

import android.content.Context
import com.ompsessionconnect.domain.model.UpdateRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Small synchronous store so a pending or downloaded update survives process death. */
@Singleton
class UpdatePrefs @Inject constructor(
  @ApplicationContext context: Context,
) {
  private val prefs = context.getSharedPreferences("omp_update", Context.MODE_PRIVATE)

  fun saveRelease(release: UpdateRelease) {
    prefs.edit()
      .putString(KEY_VERSION, release.versionName)
      .putString(KEY_NOTES, release.notes)
      .putString(KEY_URL, release.apkUrl)
      .putString(KEY_APK_NAME, release.apkName)
      .putLong(KEY_SIZE, release.sizeBytes)
      .apply()
  }

  fun release(): UpdateRelease? {
    val version = prefs.getString(KEY_VERSION, null) ?: return null
    val url = prefs.getString(KEY_URL, null) ?: return null
    val apkName = prefs.getString(KEY_APK_NAME, null) ?: return null
    return UpdateRelease(
      versionName = version,
      notes = prefs.getString(KEY_NOTES, "").orEmpty(),
      apkUrl = url,
      apkName = apkName,
      sizeBytes = prefs.getLong(KEY_SIZE, 0L),
    )
  }

  fun saveDownloadedPath(apkPath: String) {
    prefs.edit().putString(KEY_APK_PATH, apkPath).apply()
  }

  fun downloadedPath(): String? = prefs.getString(KEY_APK_PATH, null)

  fun clearDownloadedPath() {
    prefs.edit().remove(KEY_APK_PATH).apply()
  }

  fun clear() {
    prefs.edit()
      .remove(KEY_VERSION)
      .remove(KEY_NOTES)
      .remove(KEY_URL)
      .remove(KEY_APK_NAME)
      .remove(KEY_SIZE)
      .remove(KEY_APK_PATH)
      .apply()
  }

  private companion object {
    const val KEY_VERSION = "release_version"
    const val KEY_NOTES = "release_notes"
    const val KEY_URL = "release_url"
    const val KEY_APK_NAME = "release_apk_name"
    const val KEY_SIZE = "release_size"
    const val KEY_APK_PATH = "downloaded_apk_path"
  }
}

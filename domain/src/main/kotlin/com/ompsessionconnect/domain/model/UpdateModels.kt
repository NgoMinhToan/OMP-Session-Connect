package com.ompsessionconnect.domain.model

/** A GitHub release that contains an APK built for this device. */
data class UpdateRelease(
  val versionName: String,
  val notes: String,
  val apkUrl: String,
  val apkName: String,
  val sizeBytes: Long,
)

/** Everything the UI needs to describe the updater. */
sealed interface UpdateState {
  data object Idle : UpdateState

  data object Checking : UpdateState

  data class UpToDate(val versionName: String) : UpdateState

  data class Available(val release: UpdateRelease) : UpdateState

  data class Downloading(
    val release: UpdateRelease,
    val downloadedBytes: Long,
    val totalBytes: Long,
  ) : UpdateState {
    val fraction: Float
      get() = if (totalBytes <= 0) 0f else (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

    val percent: Int get() = (fraction * 100).toInt()
  }

  data class ReadyToInstall(
    val release: UpdateRelease,
    val apkPath: String,
  ) : UpdateState

  data class Failed(
    /** Present when a download failed so the UI can offer a retry. */
    val release: UpdateRelease?,
    val message: String?,
    /** True only for download failures; check failures stay quiet in dialogs. */
    val fromDownload: Boolean,
  ) : UpdateState
}

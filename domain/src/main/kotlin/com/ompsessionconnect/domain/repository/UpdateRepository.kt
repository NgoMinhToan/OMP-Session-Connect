package com.ompsessionconnect.domain.repository

import com.ompsessionconnect.domain.model.UpdateRelease

interface UpdateRepository {
  /**
   * Newest published release newer than [currentVersion], or null when the
   * installed build is current, no release is publishable, or nothing matches
   * one of [deviceAbis].
   */
  suspend fun latestRelease(
    currentVersion: String,
    deviceAbis: List<String>,
  ): Result<UpdateRelease?>

  /** Streams [release] to local storage, resuming a previous partial download. */
  suspend fun download(
    release: UpdateRelease,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
  ): Result<String>

}

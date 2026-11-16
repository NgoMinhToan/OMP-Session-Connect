package com.ompsessionconnect.data.update

import android.content.Context
import com.ompsessionconnect.domain.model.UpdateRelease
import com.ompsessionconnect.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class UpdateRepositoryImpl @Inject constructor(
  client: OkHttpClient,
  @param:ApplicationContext private val context: Context,
) : UpdateRepository {
  private val api = GithubReleasesApi(client)
  private val downloadClient = client

  override suspend fun latestRelease(
    currentVersion: String,
    deviceAbis: List<String>,
  ): Result<UpdateRelease?> = api.latestRelease(currentVersion, deviceAbis)

  override suspend fun download(
    release: UpdateRelease,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
  ): Result<String> = withContext(Dispatchers.IO) {
    runCatching { downloadApk(release, onProgress) }
  }


  private fun updateDir(): File = File(context.filesDir, UPDATE_DIR).apply { mkdirs() }

  private fun downloadApk(
    release: UpdateRelease,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
  ): String {
    val dir = updateDir()
    val safeName = File(release.apkName).name
    require(safeName.endsWith(".apk", ignoreCase = true)) { "Release asset is not an APK" }
    val target = File(dir, safeName)
    if (target.isFile && target.length() > 0) {
      onProgress(target.length(), target.length())
      return target.absolutePath
    }
    val part = File(dir, "$safeName.part")
    var existing = part.length()
    var response = execute(release, existing)
    // The server may reject a stale range; start over once.
    if (response.code == 416 && existing > 0) {
      response.close()
      part.delete()
      existing = 0
      response = execute(release, 0)
    }
    return response.use { result ->
      if (!result.isSuccessful) error("Download failed (HTTP ${result.code})")
      val body = result.body ?: error("Download failed: empty response")
      val append = result.code == 206 && existing > 0
      val length = body.contentLength()
      val total = when {
        append && length >= 0 -> existing + length
        length >= 0 -> length
        else -> release.sizeBytes
      }
      if (!append) part.delete()
      var written = if (append) existing else 0L
      var lastReport = 0L
      body.byteStream().use { input ->
        FileOutputStream(part, append).use { output ->
          val buffer = ByteArray(64 * 1024)
          while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            written += read
            val now = System.nanoTime()
            if (now - lastReport >= PROGRESS_INTERVAL_NANOS) {
              lastReport = now
              onProgress(written, total)
            }
          }
        }
      }
      if (written <= 0) error("Downloaded file is empty")
      if (total > 0 && written < total) error("Download incomplete ($written of $total bytes)")
      if (!isZip(part)) {
        part.delete()
        error("Downloaded file is not an APK")
      }
      target.delete()
      if (!part.renameTo(target)) error("Could not store the update")
      dir.listFiles()?.forEach { file ->
        if (file != target && file.name != part.name) file.delete()
      }
      onProgress(target.length(), target.length())
      target.absolutePath
    }
  }

  private fun execute(release: UpdateRelease, offset: Long) = downloadClient.newCall(
    Request.Builder()
      .url(release.apkUrl)
      .header("User-Agent", "OMP-Session-Connect")
      .apply { if (offset > 0) header("Range", "bytes=$offset-") }
      .build(),
  ).execute()

  private fun isZip(file: File): Boolean = runCatching {
    file.inputStream().use { input ->
      input.read() == 'P'.code && input.read() == 'K'.code
    }
  }.getOrDefault(false)

  private companion object {
    const val UPDATE_DIR = "updates"
    const val PROGRESS_INTERVAL_NANOS = 150_000_000L
  }
}

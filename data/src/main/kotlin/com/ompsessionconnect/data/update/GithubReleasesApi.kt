package com.ompsessionconnect.data.update

import com.ompsessionconnect.domain.model.AppVersion
import com.ompsessionconnect.domain.model.UpdateRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
internal data class ReleaseAssetDto(
  val name: String = "",
  @SerialName("browser_download_url") val url: String = "",
  val size: Long = 0,
)

@Serializable
internal data class ReleaseDto(
  @SerialName("tag_name") val tagName: String = "",
  val name: String = "",
  val body: String = "",
  val draft: Boolean = false,
  val prerelease: Boolean = false,
  val assets: List<ReleaseAssetDto> = emptyList(),
)

/**
 * Reads the newest public release of [REPOSITORY] and picks the APK matching
 * this device's ABI. Bitmap: minified `release` builds are preferred over
 * `release-nonminify`, then ABI order, then the universal APK.
 */
internal class GithubReleasesApi(
  private val client: OkHttpClient,
  private val apiBase: HttpUrl = "https://api.github.com/".toHttpUrl(),
) {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun latestRelease(
    currentVersion: String,
    deviceAbis: List<String>,
  ): Result<UpdateRelease?> = withContext(Dispatchers.IO) {
    runCatching {
      val url = apiBase.newBuilder()
        .addPathSegments("repos/$REPOSITORY/releases/latest")
        .build()
      val request = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "OMP-Session-Connect/$currentVersion")
        .build()
      client.newCall(request).execute().use { response ->
        // A fresh repository has no published release yet; that is not an error.
        if (response.code == 404) return@use null
        if (!response.isSuccessful) {
          error("Update check failed (HTTP ${response.code})")
        }
        val body = response.body?.string().orEmpty()
        val release = toRelease(json.decodeFromString<ReleaseDto>(body), deviceAbis)
        release?.takeIf { AppVersion.isNewer(it.versionName, currentVersion) }
      }
    }
  }

  companion object {
    const val REPOSITORY = "NgoMinhToan/OMP-Session-Connect"
  }
}

internal fun toRelease(dto: ReleaseDto, deviceAbis: List<String>): UpdateRelease? {
  if (dto.draft || dto.prerelease) return null
  val version = AppVersion.normalize(dto.tagName) ?: AppVersion.normalize(dto.name) ?: return null
  val asset = selectApk(dto.assets, deviceAbis) ?: return null
  return UpdateRelease(
    versionName = version,
    notes = dto.body.trim(),
    apkUrl = asset.url,
    apkName = asset.name,
    sizeBytes = asset.size,
  )
}

internal fun selectApk(assets: List<ReleaseAssetDto>, deviceAbis: List<String>): ReleaseAssetDto? {
  val apks = assets.filter { asset ->
    val name = asset.name
    name.endsWith(".apk", ignoreCase = true) &&
      name.contains("-release-", ignoreCase = true) &&
      !name.contains("-release-nonminify-", ignoreCase = true) &&
      asset.url.startsWith("https://") &&
      File(name).name == name
  }
  deviceAbis.forEach { abi ->
    apks.firstOrNull { it.name.endsWith("-release-$abi.apk", ignoreCase = true) }
      ?.let { return it }
  }
  return apks.firstOrNull { it.name.endsWith("-release-universal.apk", ignoreCase = true) }
}

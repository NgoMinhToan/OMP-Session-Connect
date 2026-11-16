package com.ompsessionconnect.data.update

import com.ompsessionconnect.domain.model.AppVersion
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSelectionTest {
  private fun asset(name: String, size: Long = 1_000): ReleaseAssetDto = ReleaseAssetDto(
    name = name,
    url = "https://github.com/NgoMinhToan/OMP-Session-Connect/releases/download/v1.0.42/$name",
    size = size,
  )

  @Test
  fun `normalizes tags and ignores prerelease suffixes`() {
    assertEquals("1.0.42", AppVersion.normalize("v1.0.42"))
    assertEquals("1.2.0", AppVersion.normalize("V1.2.0-rc.1"))
    assertEquals("1.0.0", AppVersion.normalize("v1.0.0+build.7"))
    assertNull(AppVersion.normalize("nightly"))
    assertNull(AppVersion.normalize("v1.x.3"))
  }

  @Test
  fun `compares numeric versions component by component`() {
    assertTrue(AppVersion.isNewer("1.0.42", "1.0.41"))
    assertTrue(AppVersion.isNewer("1.2.0", "1.1.99"))
    assertTrue(AppVersion.isNewer("2.0", "1.9.9"))
    assertFalse(AppVersion.isNewer("1.0.42", "1.0.42"))
    assertFalse(AppVersion.isNewer("1.0.41", "1.0.42"))
    assertFalse(AppVersion.isNewer("nonsense", "1.0.0"))
    assertFalse(AppVersion.isNewer("1.0.42", "dev"))
  }

  @Test
  fun `prefers the device abi and the minified release variant`() {
    val assets = listOf(
      asset("OMP-Session-Connect-1.0.42-release-nonminify-arm64-v8a.apk"),
      asset("OMP-Session-Connect-1.0.42-release-universal.apk"),
      asset("OMP-Session-Connect-1.0.42-release-x86_64.apk"),
      asset("OMP-Session-Connect-1.0.42-release-arm64-v8a.apk"),
    )
    val selected = selectApk(assets, listOf("arm64-v8a", "armeabi-v7a"))
    assertEquals("OMP-Session-Connect-1.0.42-release-arm64-v8a.apk", selected?.name)
  }

  @Test
  fun `falls back to the second abi then universal`() {
    val assets = listOf(
      asset("OMP-Session-Connect-1.0.42-release-universal.apk"),
      asset("OMP-Session-Connect-1.0.42-release-armeabi-v7a.apk"),
    )
    assertEquals(
      "OMP-Session-Connect-1.0.42-release-armeabi-v7a.apk",
      selectApk(assets, listOf("arm64-v8a", "armeabi-v7a"))?.name,
    )
    assertEquals(
      "OMP-Session-Connect-1.0.42-release-universal.apk",
      selectApk(assets, listOf("x86"))?.name,
    )
  }

  @Test
  fun `reads the newest release from the github api`() = runBlocking {
    val body = """
      {
        "tag_name": "v1.0.42",
        "name": "OMP Session Connect 1.0.42",
        "body": "Fixes and polish.",
        "draft": false,
        "prerelease": false,
        "assets": [
          {"name": "OMP-Session-Connect-1.0.42-release-nonminify-arm64-v8a.apk",
           "browser_download_url": "https://example.com/nonminify.apk", "size": 10},
          {"name": "OMP-Session-Connect-1.0.42-release-arm64-v8a.apk",
           "browser_download_url": "https://example.com/arm64.apk", "size": 2048},
          {"name": "OMP-Session-Connect-1.0.42-release-universal.apk",
           "browser_download_url": "https://example.com/universal.apk", "size": 4096}
        ]
      }
    """.trimIndent()
    MockWebServer().use { server ->
      server.enqueue(MockResponse().setBody(body).setHeader("Content-Type", "application/json"))
      server.start()
      val api = GithubReleasesApi(OkHttpClient(), server.url("/"))
      val release = api.latestRelease("1.0.41", listOf("arm64-v8a")).getOrThrow()
      assertEquals("1.0.42", release?.versionName)
      assertEquals("OMP-Session-Connect-1.0.42-release-arm64-v8a.apk", release?.apkName)
      assertEquals("https://example.com/arm64.apk", release?.apkUrl)
      assertEquals(2048L, release?.sizeBytes)
      assertEquals("Fixes and polish.", release?.notes)
      val path = server.takeRequest().path
      assertEquals("/repos/NgoMinhToan/OMP-Session-Connect/releases/latest", path)
    }
  }

  @Test
  fun `reports no update when the installed build is current`() = runBlocking {
    val body = """
      {
        "tag_name": "v1.0.42",
        "assets": [
          {"name": "OMP-Session-Connect-1.0.42-release-arm64-v8a.apk",
           "browser_download_url": "https://example.com/arm64.apk", "size": 2048}
        ]
      }
    """.trimIndent()
    MockWebServer().use { server ->
      server.enqueue(MockResponse().setBody(body).setHeader("Content-Type", "application/json"))
      server.enqueue(MockResponse().setResponseCode(404))
      server.start()
      val api = GithubReleasesApi(OkHttpClient(), server.url("/"))
      assertNull(api.latestRelease("1.0.42", listOf("arm64-v8a")).getOrThrow())
      assertNull(api.latestRelease("1.0.42", listOf("arm64-v8a")).getOrThrow())
    }
  }

  @Test
  fun `skips drafts and releases without a matching apk`() {
    val draft = ReleaseDto(
      tagName = "v1.1.0",
      draft = true,
      assets = listOf(asset("OMP-Session-Connect-1.1.0-release-universal.apk")),
    )
    assertNull(toRelease(draft, listOf("arm64-v8a")))
    val noApk = ReleaseDto(tagName = "v1.1.0", assets = listOf(asset("notes.txt")))
    assertNull(toRelease(noApk, listOf("arm64-v8a")))
  }
}

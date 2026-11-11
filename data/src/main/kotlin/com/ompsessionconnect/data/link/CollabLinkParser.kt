package com.ompsessionconnect.data.link

import java.net.URI
import java.util.Base64

/**
 * Lenient parser for every link form emitted by `omp /collab`.
 *
 * Accepted inputs include the compact room id form, scheme-less relay links,
 * `wss://…/r/<room>.<key>`, and browser deep links whose fragment carries the
 * room link (`https://my.omp.sh/#<link>`).
 */
object CollabLinkParser {
  const val DEFAULT_RELAY = "wss://my.omp.sh"

  private val ROOM_PATH = Regex("^/r/([A-Za-z0-9_-]{10,64})(?:\\.([A-Za-z0-9_-]+))?$")
  private val BARE_LINK = Regex("^([A-Za-z0-9_-]{10,64})[#.]([A-Za-z0-9_-]+)$")
  private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

  fun parse(input: String): Result<CollabLink> = runCatching { parseOrThrow(input) }

  fun decodeBase64Url(text: String): ByteArray? = try {
    Base64.getUrlDecoder().decode(text)
  } catch (_: IllegalArgumentException) {
    null
  }

  fun encodeBase64Url(bytes: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

  private fun parseOrThrow(raw: String): CollabLink {
    var text = raw.trim()
    require(text.isNotEmpty()) { "Collab link is empty" }
    text = text.replace(Regex("%23", RegexOption.IGNORE_CASE), "#")

    BARE_LINK.matchEntire(text)?.let { match ->
      text = "$DEFAULT_RELAY/r/${match.groupValues[1]}.${match.groupValues[2]}"
    }
    if (!text.contains("://")) text = "wss://$text"

    val uri = try {
      URI(text)
    } catch (_: Exception) {
      throw IllegalArgumentException("Invalid collab link")
    }

    // Browser wrappers keep the real relay link in the fragment. A direct
    // relay link may also carry its legacy key there; recursion fails then
    // and the key is handled by the path/fragment fallback below.
    val fragment = uri.rawFragment?.takeIf { it.isNotBlank() }
    if (fragment != null) {
      parse(fragment).getOrNull()?.let { return it }
    }

    val origin = normalizeOrigin(uri)
      ?: throw IllegalArgumentException("Relay must use wss:// (plain ws:// is only allowed for localhost)")
    val path = uri.rawPath ?: ""
    val match = ROOM_PATH.matchEntire(path)
      ?: throw IllegalArgumentException("Collab link must contain a /r/<roomId> path")

    val roomId = match.groupValues[1]
    val pathSecret = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
    val secretText = pathSecret ?: fragment
      ?: throw IllegalArgumentException("Collab link is missing its key")
    val secret = decodeBase64Url(secretText)
      ?: throw IllegalArgumentException("Collab link key is not valid base64url")
    require(secret.size == 32 || secret.size == 48) {
      "Collab link key must be 32 (view) or 48 (full) bytes"
    }
    val key = secret.copyOfRange(0, 32)
    val writeToken = if (secret.size > 32) secret.copyOfRange(32, secret.size) else null
    return CollabLink("$origin/r/$roomId", roomId, key, writeToken)
  }

  private fun normalizeOrigin(uri: URI): String? {
    val scheme = when (uri.scheme?.lowercase()) {
      "ws", "http" -> "ws"
      "wss", "https" -> "wss"
      else -> return null
    }
    val host = uri.host ?: run {
      // java.net.URI leaves IPv6 literals in the authority bracketed.
      uri.rawAuthority?.substringBefore(':')?.takeIf { it.startsWith("[") } ?: return null
    }
    if (scheme == "ws" && host !in LOCAL_HOSTS) return null
    val port = if (uri.port == -1) "" else ":${uri.port}"
    return "$scheme://$host$port"
  }
}

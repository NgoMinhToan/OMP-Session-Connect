package com.ompsessionconnect.data.link

import java.net.URI
import java.security.MessageDigest
import java.util.Base64

/**
 * A parsed omp collab link.
 *
 * [key] is the 32-byte AES-256-GCM room key. [writeToken] exists only for
 * full-control links; a bare key link is read-only.
 */
class CollabLink(
  val wsUrl: String,
  val roomId: String,
  val key: ByteArray,
  val writeToken: ByteArray?,
) {
  val isReadOnly: Boolean get() = writeToken == null

  /** Stable identity of the room + secret, used to reject duplicate links. */
  fun fingerprint(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(wsUrl.toByteArray(Charsets.UTF_8))
    digest.update(0)
    digest.update(roomId.toByteArray(Charsets.UTF_8))
    digest.update(0)
    // Access level is deliberately excluded: a view link and a control link
    // to the same room must not become two sessions.
    digest.update(key)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest())
  }

  /** Never include the key or token in logs. */
  override fun toString(): String = "CollabLink(wsUrl=$wsUrl, roomId=$roomId, readOnly=$isReadOnly)"
}

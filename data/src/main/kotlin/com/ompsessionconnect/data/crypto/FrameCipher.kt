package com.ompsessionconnect.data.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM sealing for collab frames. Wire layout is `[12-byte IV][ciphertext+tag]`.
 *
 * This mirrors `packages/coding-agent/src/collab/crypto.ts` from the omp host,
 * so frames produced here are byte-compatible with the official clients.
 */
object FrameCipher {
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val IV_BYTES = 12
  private const val TAG_BITS = 128
  private val random = SecureRandom()

  fun seal(plaintext: String, key: ByteArray): ByteArray {
    require(key.size == 32) { "Room key must be 32 bytes" }
    val iv = ByteArray(IV_BYTES).also(random::nextBytes)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(
      Cipher.ENCRYPT_MODE,
      SecretKeySpec(key, "AES"),
      GCMParameterSpec(TAG_BITS, iv),
    )
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return iv + ciphertext
  }

  fun open(sealed: ByteArray, key: ByteArray): String {
    require(sealed.size > IV_BYTES) { "Sealed frame is too short" }
    require(key.size == 32) { "Room key must be 32 bytes" }
    val iv = sealed.copyOfRange(0, IV_BYTES)
    val ciphertext = sealed.copyOfRange(IV_BYTES, sealed.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(
      Cipher.DECRYPT_MODE,
      SecretKeySpec(key, "AES"),
      GCMParameterSpec(TAG_BITS, iv),
    )
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }
}

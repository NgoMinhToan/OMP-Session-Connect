package com.ompsessionconnect.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameCipherTest {
  private val key = ByteArray(32) { it.toByte() }

  @Test
  fun `seal and open round trip`() {
    val plaintext = """{"t":"hello","proto":3}"""
    val sealed = FrameCipher.seal(plaintext, key)
    assertTrue(sealed.size > 12)
    assertEquals(plaintext, FrameCipher.open(sealed, key))
  }

  @Test
  fun `tampered frame fails authentication`() {
    val sealed = FrameCipher.seal("payload", key)
    sealed[sealed.lastIndex] = (sealed.last() + 1).toByte()
    assertTrue(runCatching { FrameCipher.open(sealed, key) }.isFailure)
  }

  @Test
  fun `wrong key fails authentication`() {
    val sealed = FrameCipher.seal("payload", key)
    assertTrue(runCatching { FrameCipher.open(sealed, ByteArray(32) { 9 }) }.isFailure)
  }
}

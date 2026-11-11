package com.ompsessionconnect.data.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollabLinkParserTest {
  private val room = "abcdefghij1234"

  private fun secret(viewOnly: Boolean): String {
    val bytes = ByteArray(if (viewOnly) 32 else 48) { (it + 1).toByte() }
    return CollabLinkParser.encodeBase64Url(bytes)
  }

  @Test
  fun `parses compact bare link with full control`() {
    val link = CollabLinkParser.parse("$room.${secret(viewOnly = false)}").getOrThrow()
    assertEquals("wss://my.omp.sh/r/$room", link.wsUrl)
    assertEquals(room, link.roomId)
    assertEquals(32, link.key.size)
    assertEquals(16, link.writeToken?.size)
    assertTrue(!link.isReadOnly)
  }

  @Test
  fun `bare link with 32 byte secret is read only`() {
    val link = CollabLinkParser.parse("$room.${secret(viewOnly = true)}").getOrThrow()
    assertTrue(link.isReadOnly)
    assertNull(link.writeToken)
  }

  @Test
  fun `browser deep link fragment wins over web host`() {
    val link = CollabLinkParser.parse(
      "https://my.omp.sh/#$room.${secret(viewOnly = false)}",
    ).getOrThrow()
    assertEquals("wss://my.omp.sh/r/$room", link.wsUrl)
    assertNotNull(link.writeToken)
  }

  @Test
  fun `scheme-less browser link resolves its fragment`() {
    val link = CollabLinkParser.parse(
      "my.omp.sh/#$room.${secret(viewOnly = false)}",
    ).getOrThrow()
    assertEquals("wss://my.omp.sh/r/$room", link.wsUrl)
  }

  @Test
  fun `browser wrapper with separate relay host`() {
    val link = CollabLinkParser.parse(
      "https://web.example/collab/#relay.example.com/r/$room.${secret(viewOnly = true)}",
    ).getOrThrow()
    assertEquals("wss://relay.example.com/r/$room", link.wsUrl)
    assertTrue(link.isReadOnly)
  }

  @Test
  fun `scheme-less relay and legacy hash form`() {
    val link = CollabLinkParser.parse(
      "relay.example.com/r/$room#${secret(viewOnly = false)}",
    ).getOrThrow()
    assertEquals("wss://relay.example.com/r/$room", link.wsUrl)
  }

  @Test
  fun `localhost plain websocket is accepted`() {
    val link = CollabLinkParser.parse(
      "ws://localhost:7475/r/$room.${secret(viewOnly = true)}",
    ).getOrThrow()
    assertEquals("ws://localhost:7475/r/$room", link.wsUrl)
  }

  @Test
  fun `plain websocket is rejected for remote hosts`() {
    val result = CollabLinkParser.parse("ws://relay.example.com/r/$room.${secret(true)}")
    assertTrue(result.isFailure)
  }

  @Test
  fun `wrong secret length is rejected`() {
    val short = CollabLinkParser.encodeBase64Url(ByteArray(16))
    assertTrue(CollabLinkParser.parse("$room.$short").isFailure)
  }

  @Test
  fun `invalid path is rejected`() {
    assertTrue(CollabLinkParser.parse("https://my.omp.sh/not-a-room").isFailure)
  }
}

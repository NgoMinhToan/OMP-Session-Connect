package com.ompsessionconnect.data.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WireTest {
  @Test
  fun `envelope round trip preserves peer id`() {
    val payload = byteArrayOf(1, 2, 3, 4)
    val packed = Wire.pack(7, payload)
    val unpacked = Wire.unpack(packed)!!
    assertEquals(7, unpacked.first)
    assertTrue(payload.contentEquals(unpacked.second))
  }

  @Test
  fun `truncated envelope is rejected`() {
    assertNull(Wire.unpack(byteArrayOf(0, 0)))
  }

  @Test
  fun `state parser derives percent when absent`() {
    val frame = Wire.parseObject(
      """{"t":"state","state":{"isStreaming":true,"queuedMessageCount":2,
         "cwd":"/tmp","model":{"id":"m","name":"Model","provider":"p","contextWindow":1000},
         "contextUsage":{"tokens":250,"contextWindow":1000},
         "participants":[{"name":"me","role":"host"}]}}""",
    )!!
    val state = RemoteParsers.parseState(frame)
    assertTrue(state.isStreaming)
    assertEquals(25, state.contextPercent)
    assertEquals(1, state.participants.size)
    assertEquals("Model", state.model?.name)
  }

  @Test
  fun `ui request parses labels and descriptions`() {
    val frame = Wire.parseObject(
      """{"t":"ui-request","request":{"kind":"select","reqId":9,"title":"Pick",
         "options":["A",{"label":"B","description":"second"}],
         "selectionMarker":"radio","checkedIndices":[0]}}""",
    )!!
    val request = RemoteParsers.parseUiRequest(frame)!!
    assertEquals(9, request.requestId)
    assertEquals("A", request.options[0].label)
    assertEquals("second", request.options[1].description)
    assertEquals(setOf(0), request.checkedIndices)
  }
}

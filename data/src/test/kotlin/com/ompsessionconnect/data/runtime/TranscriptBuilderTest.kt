package com.ompsessionconnect.data.runtime

import com.ompsessionconnect.data.protocol.Wire
import com.ompsessionconnect.domain.model.AssistantMessageItem
import com.ompsessionconnect.domain.model.ToolCallItem
import com.ompsessionconnect.domain.model.ToolStatus
import com.ompsessionconnect.domain.model.UserMessageItem
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptBuilderTest {
  private fun json(text: String): JsonObject = Wire.parseObject(text)!!

  @Test
  fun `assistant tool result folds into the tool card`() {
    val builder = TranscriptBuilder()
    builder.applyEntry(
      json(
        """{"type":"message","id":"u1","timestamp":"2026-01-01T00:00:00Z",
           "message":{"role":"user","content":"list files"}}""",
      ),
    )
    builder.applyEntry(
      json(
        """{"type":"message","id":"a1","timestamp":"2026-01-01T00:00:01Z",
           "message":{"role":"assistant","model":"gpt","content":[
             {"type":"text","text":"Sure."},
             {"type":"toolCall","id":"call-1","name":"bash","arguments":{"cmd":"ls"}}
           ]}}""",
      ),
    )
    builder.applyEntry(
      json(
        """{"type":"message","id":"r1","timestamp":"2026-01-01T00:00:02Z",
           "message":{"role":"toolResult","toolCallId":"call-1","toolName":"bash",
             "content":[{"type":"text","text":"a.txt"}],"isError":false}}""",
      ),
    )

    val items = builder.items
    assertEquals(3, items.size)
    assertTrue(items[0] is UserMessageItem)
    assertTrue(items[1] is AssistantMessageItem)
    val tool = items[2] as ToolCallItem
    assertEquals("bash", tool.toolName)
    assertEquals(ToolStatus.DONE, tool.status)
    assertEquals("a.txt", tool.output)
    assertEquals("{\"cmd\":\"ls\"}", tool.arguments)
  }

  @Test
  fun `collab prompt keeps the guest name`() {
    val builder = TranscriptBuilder()
    builder.applyEntry(
      json(
        """{"type":"custom_message","id":"c1","timestamp":"2026-01-01T00:00:00Z",
           "customType":"collab-prompt","content":"hi","display":true,
           "details":{"from":"Pixel"}}""",
      ),
    )
    val item = builder.items.single() as UserMessageItem
    assertEquals("Pixel", item.from)
    assertEquals("hi", item.text)
  }

  @Test
  fun `live tool start and end are idempotent`() {
    val builder = TranscriptBuilder()
    builder.applyToolStart("t1", "grep", buildJsonObject { put("q", "foo") })
    builder.applyToolEnd("t1", "grep", buildJsonObject { put("text", "match") }, isError = false)
    assertEquals(1, builder.items.size)
    val item = builder.items.single() as ToolCallItem
    assertEquals(ToolStatus.DONE, item.status)
    assertTrue(item.output!!.contains("match"))
  }
}

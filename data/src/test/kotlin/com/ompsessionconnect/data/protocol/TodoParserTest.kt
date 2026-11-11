package com.ompsessionconnect.data.protocol

import com.ompsessionconnect.domain.model.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoParserTest {
  @Test
  fun `parses phases and computes progress`() {
    val details = Wire.parseObject(
      """{"phases":[
           {"name":"Build","tasks":[
             {"content":"Wire settings","status":"completed"},
             {"content":"Add voice","status":"in_progress"},
             {"content":"Ship","status":"blocked","blocker":"review"}
           ]}
         ],"storage":"session"}""",
    )!!
    val progress = TodoParser.fromDetails(details)!!
    assertEquals(3, progress.total)
    assertEquals(1, progress.completed)
    assertEquals("Add voice", progress.active?.content)
    assertEquals("review", progress.tasks[2].blocker)
    assertEquals(TodoStatus.BLOCKED, progress.tasks[2].status)
    assertTrue(!progress.isComplete)
  }

  @Test
  fun `tool result details are accepted`() {
    val result = Wire.parseObject(
      """{"content":[{"type":"text","text":"ok"}],
          "details":{"phases":[{"name":"Tasks","tasks":[
            {"content":"Only task","status":"completed"}]}]}}""",
    )!!
    val progress = TodoParser.fromToolResult(result)
    assertNotNull(progress)
    assertEquals(1, progress!!.completed)
    assertTrue(progress.isComplete)
  }

  @Test
  fun `missing phases does not replace progress`() {
    val result = Wire.parseObject("""{"content":[{"type":"text","text":"error"}],"isError":true}""")!!
    assertEquals(null, TodoParser.fromToolResult(result))
  }
}

package com.ompsessionconnect.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

  @Test
  fun fencedCodeKeepsLanguageAndBlankLines() {
    val blocks = parseMarkdown("Intro\n\n```kotlin\nval x = 1\n\nval y = 2\n```\nOutro")

    assertEquals(3, blocks.size)
    assertEquals("Intro", (blocks[0] as MdBlock.Paragraph).text)

    val code = blocks[1] as MdBlock.CodeBlock
    assertEquals("kotlin", code.language)
    assertEquals("val x = 1\n\nval y = 2", code.code)

    assertEquals("Outro", (blocks[2] as MdBlock.Paragraph).text)
  }

  @Test
  fun tildeFenceAndUnclosedFence() {
    assertEquals(
      "ls -la\nb",
      (parseMarkdown("~~~sh\nls -la\nb\n~~~").single() as MdBlock.CodeBlock).code,
    )

    val unclosed = parseMarkdown("text\n```\nline 1\nline 2")
    val code = unclosed.last() as MdBlock.CodeBlock
    assertEquals("line 1\nline 2", code.code)
    assertEquals("", code.language)
  }

  @Test
  fun tableParsesAlignmentAndRaggedRows() {
    val blocks = parseMarkdown(
      """
      | Name | Qty |
      |:-----|----:|
      | a    | 1   |
      | b |
      """.trimIndent(),
    )

    val table = blocks.single() as MdBlock.Table
    assertEquals(listOf("Name", "Qty"), table.header)
    assertEquals(listOf(MdAlign.START, MdAlign.END), table.aligns)
    assertEquals(listOf("a", "1"), table.rows[0])
    assertEquals(listOf("b"), table.rows[1])
  }

  @Test
  fun tableKeepsEscapedPipeInsideCell() {
    val blocks = parseMarkdown("| a | b |\n|---|---|\n| x \\| y | z |")
    val table = blocks.single() as MdBlock.Table
    assertEquals(listOf("x | y", "z"), table.rows.single())
  }

  @Test
  fun bulletListUsesIndentForDepth() {
    val list = parseMarkdown("- one\n  - nested\n- two").single() as MdBlock.BulletList
    assertEquals(listOf(0, 1, 0), list.items.map { it.depth })
    assertEquals(listOf("one", "nested", "two"), list.items.map { it.text })
  }

  @Test
  fun orderedListKeepsStartNumber() {
    val list = parseMarkdown("3. first\n4. second").single() as MdBlock.OrderedList
    assertEquals(3, list.start)
    assertEquals(listOf("first", "second"), list.items.map { it.text })
  }

  @Test
  fun paragraphsJoinSoftWrappedLines() {
    val paragraph = parseMarkdown("line one\nline two").single() as MdBlock.Paragraph
    assertEquals("line one line two", paragraph.text)
  }

  @Test
  fun headingsQuoteAndRule() {
    val blocks = parseMarkdown("# Title\n\n### Small ###\n\n> quoted\n> lines\n\n---")
    assertEquals(MdBlock.Heading(1, "Title"), blocks[0])
    assertEquals(MdBlock.Heading(3, "Small"), blocks[1])
    assertEquals(MdBlock.Quote("quoted\nlines"), blocks[2])
    assertEquals(MdBlock.Rule, blocks[3])
  }

  @Test
  fun listStopsAtFenceAndCodeFollows() {
    val blocks = parseMarkdown("- item\n```\ncode\n```")
    assertEquals(1, (blocks[0] as MdBlock.BulletList).items.size)
    assertEquals("code", (blocks[1] as MdBlock.CodeBlock).code)
  }

  @Test
  fun blankInputYieldsNoBlocks() {
    assertTrue(parseMarkdown("   \n\n").isEmpty())
  }

  @Test
  fun headingMarkerWithoutSpaceIsParagraph() {
    // "#tag" is not a heading; hashtags in prose must survive.
    val paragraph = parseMarkdown("#tag is not a heading").single() as MdBlock.Paragraph
    assertEquals("#tag is not a heading", paragraph.text)
  }
}

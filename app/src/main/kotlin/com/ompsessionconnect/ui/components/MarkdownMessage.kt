package com.ompsessionconnect.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ompsessionconnect.R
import com.ompsessionconnect.ui.theme.CodeTextStyle
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import kotlinx.coroutines.delay

/**
 * Renders model output as rich content: headings, paragraphs with inline styles, fenced code
 * (monospace, terminal style, horizontal scroll, copy), lists, quotes and pipe tables.
 */
@Composable
fun MarkdownMessage(
  markdown: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = LocalLiveGlass.current.ink,
  spacing: Dp = 10.dp,
) {
  val blocks = remember(markdown) { parseMarkdown(markdown) }
  if (blocks.isEmpty()) return
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    blocks.forEach { block -> MarkdownBlock(block, style, color) }
  }
}

@Composable
private fun MarkdownBlock(
  block: MdBlock,
  style: TextStyle,
  color: Color,
) {
  val tokens = LocalLiveGlass.current
  when (block) {
    is MdBlock.Heading -> {
      val headingStyle = when (block.level) {
        1 -> style.copy(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
        2 -> style.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        3 -> style.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
        else -> style.copy(fontWeight = FontWeight.SemiBold)
      }
      Text(
        text = remember(block.text) {
          inlineMarkdown(block.text, tokens.control, tokens.primary)
        },
        style = headingStyle,
        color = tokens.ink,
      )
    }

    is MdBlock.Paragraph -> {
      Text(
        text = remember(block.text) {
          inlineMarkdown(block.text, tokens.control, tokens.primary)
        },
        style = style,
        color = color,
      )
    }

    is MdBlock.CodeBlock -> MarkdownCodeBlock(block)

    is MdBlock.BulletList -> MarkdownList(
      items = block.items,
      orderedStart = null,
      style = style,
      color = color,
    )

    is MdBlock.OrderedList -> MarkdownList(
      items = block.items,
      orderedStart = block.start,
      style = style,
      color = color,
    )

    is MdBlock.Quote -> {
      Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
          Modifier
            .width(3.dp)
            .fillMaxHeight()
            .background(tokens.edge),
        )
        Text(
          text = remember(block.text) {
            inlineMarkdown(block.text, tokens.control, tokens.primary)
          },
          modifier = Modifier.padding(start = 10.dp),
          style = style,
          color = tokens.muted,
        )
      }
    }

    is MdBlock.Table -> MarkdownTable(block, style, color)

    MdBlock.Rule -> HorizontalDivider(color = tokens.hairline)
  }
}

@Composable
private fun MarkdownList(
  items: List<MdListItem>,
  orderedStart: Int?,
  style: TextStyle,
  color: Color,
) {
  val tokens = LocalLiveGlass.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    items.forEachIndexed { index, item ->
      Row(modifier = Modifier.padding(start = (item.depth * 16).dp)) {
        Text(
          text = if (orderedStart != null) "${orderedStart + index}." else "•",
          modifier = Modifier.widthIn(min = 16.dp).padding(end = 6.dp),
          style = style,
          color = tokens.muted,
        )
        Text(
          text = remember(item.text) {
            inlineMarkdown(item.text, tokens.control, tokens.primary)
          },
          style = style,
          color = color,
        )
      }
    }
  }
}

@Composable
private fun MarkdownCodeBlock(block: MdBlock.CodeBlock) {
  val tokens = LocalLiveGlass.current
  val palette = remember(tokens) {
    CodePalette(
      keyword = tokens.primary,
      string = if (tokens.dark) Color(0xFF8FD9A8) else Color(0xFF1B7F4B),
      comment = tokens.muted,
      number = if (tokens.dark) Color(0xFFF0B26B) else Color(0xFFAA4B00),
    )
  }
  val highlighted = remember(block.code, block.language, palette) {
    highlightCode(block.code, block.language, palette)
  }
  val context = LocalContext.current
  var copied by remember(block.code) { mutableStateOf(false) }
  val copyLabel = stringResource(R.string.markdown_copy_code)
  val copiedLabel = stringResource(R.string.markdown_copied)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = tokens.control,
    contentColor = tokens.ink,
    border = BorderStroke(1.dp, tokens.hairline),
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 12.dp, top = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = block.language.ifBlank { stringResource(R.string.markdown_code) },
          modifier = Modifier.weight(1f),
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted,
          maxLines = 1,
        )
        TextButton(
          onClick = {
            copyToClipboard(context, block.code)
            copied = true
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text(
            text = if (copied) copiedLabel else copyLabel,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
      ) {
        Text(
          text = highlighted,
          modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
          style = CodeTextStyle,
          color = tokens.ink,
          softWrap = false,
        )
      }
    }
  }

  LaunchedEffect(copied) {
    if (copied) {
      delay(1500)
      copied = false
    }
  }
}

@Composable
private fun MarkdownTable(
  block: MdBlock.Table,
  messageStyle: TextStyle,
  messageColor: Color,
) {
  val tokens = LocalLiveGlass.current
  val density = LocalDensity.current
  val measurer = rememberTextMeasurer()
  val cellStyle = messageStyle.copy(fontSize = 13.sp, lineHeight = 18.sp)
  val columns = maxOf(
    block.header.size,
    block.rows.maxOfOrNull { it.size } ?: 0,
    1,
  )
  val headerCells = remember(block.header, tokens.control, tokens.primary) {
    List(columns) { column ->
      inlineMarkdown(block.header.getOrElse(column) { "" }, tokens.control, tokens.primary)
    }
  }
  val bodyCells = remember(block.rows, tokens.control, tokens.primary) {
    block.rows.map { row ->
      List(columns) { column ->
        inlineMarkdown(row.getOrElse(column) { "" }, tokens.control, tokens.primary)
      }
    }
  }
  val padHorizontal = 10.dp
  val padVertical = 7.dp

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val available = constraints.maxWidth.toFloat()
    val paddingPx = with(density) { padHorizontal.toPx() } * 2f
    val minColumnPx = with(density) { 56.dp.toPx() }
    val maxColumnPx = available * 0.62f
    val widths = remember(headerCells, bodyCells, available, cellStyle) {
      val raw = FloatArray(columns) { column ->
        var widest = measurer.measure(headerCells[column], cellStyle).size.width.toFloat()
        bodyCells.forEach { row ->
          val cellWidth = measurer.measure(row[column], cellStyle).size.width.toFloat()
          if (cellWidth > widest) widest = cellWidth
        }
        (widest + paddingPx).coerceIn(minColumnPx, maxOf(minColumnPx, maxColumnPx))
      }
      val total = raw.sum()
      if (total > 0f && total < available) {
        val scale = available / total
        FloatArray(columns) { raw[it] * scale }
      } else {
        raw
      }
    }
    val columnWidths = widths.map { with(density) { it.toDp() } }
    val totalWidth = with(density) { widths.sum().toDp() }
    val scrollable = widths.sum() > available + 0.5f

    Column(
      modifier = if (scrollable) {
        Modifier
          .horizontalScroll(rememberScrollState())
          .width(totalWidth)
      } else {
        Modifier.fillMaxWidth()
      },
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        headerCells.forEachIndexed { column, cell ->
          TableCell(
            text = cell,
            width = columnWidths[column],
            align = block.aligns.getOrElse(column) { MdAlign.START },
            style = cellStyle,
            color = tokens.ink,
            header = true,
            padHorizontal = padHorizontal,
            padVertical = padVertical,
          )
        }
      }
      HorizontalDivider(color = tokens.edge)
      bodyCells.forEachIndexed { index, row ->
        Row(modifier = Modifier.fillMaxWidth()) {
          row.forEachIndexed { column, cell ->
            TableCell(
              text = cell,
              width = columnWidths[column],
              align = block.aligns.getOrElse(column) { MdAlign.START },
              style = cellStyle,
              color = messageColor,
              header = false,
              padHorizontal = padHorizontal,
              padVertical = padVertical,
            )
          }
        }
        if (index != bodyCells.lastIndex) HorizontalDivider(color = tokens.hairline)
      }
    }
  }
}

@Composable
private fun TableCell(
  text: AnnotatedString,
  width: Dp,
  align: MdAlign,
  style: TextStyle,
  color: Color,
  header: Boolean,
  padHorizontal: Dp,
  padVertical: Dp,
) {
  Text(
    text = text,
    modifier = Modifier
      .width(width)
      .padding(horizontal = padHorizontal, vertical = padVertical),
    style = if (header) style.copy(fontWeight = FontWeight.SemiBold) else style,
    color = color,
    textAlign = when (align) {
      MdAlign.CENTER -> TextAlign.Center
      MdAlign.END -> TextAlign.End
      MdAlign.START -> TextAlign.Start
    },
  )
}

private fun copyToClipboard(context: Context, text: String) {
  val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  manager.setPrimaryClip(ClipData.newPlainText("markdown", text))
}

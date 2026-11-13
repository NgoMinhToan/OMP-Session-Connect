package com.ompsessionconnect.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.ompsessionconnect.ui.theme.JetBrainsMonoFamily
import java.util.concurrent.ConcurrentHashMap

/** Block level node produced by [parseMarkdown]. Plain data so it can be unit tested. */
sealed interface MdBlock {
  data class Heading(val level: Int, val text: String) : MdBlock

  data class Paragraph(val text: String) : MdBlock

  data class CodeBlock(val language: String, val code: String) : MdBlock

  data class BulletList(val items: List<MdListItem>) : MdBlock

  data class OrderedList(val start: Int, val items: List<MdListItem>) : MdBlock

  data class Quote(val text: String) : MdBlock

  data class Table(
    val header: List<String>,
    val aligns: List<MdAlign>,
    val rows: List<List<String>>,
  ) : MdBlock

  data object Rule : MdBlock
}

data class MdListItem(val depth: Int, val text: String)

enum class MdAlign { START, CENTER, END }

/**
 * Minimal CommonMark/GitHub flavoured parser covering what chat replies actually use:
 * fenced code, headings, pipe tables, bullet/ordered lists, quotes, rules and paragraphs.
 */
fun parseMarkdown(source: String): List<MdBlock> {
  if (source.isBlank()) return emptyList()
  val lines = source.replace("\r\n", "\n").replace('\r', '\n').lines()
  val blocks = mutableListOf<MdBlock>()
  val paragraph = StringBuilder()
  var index = 0

  fun flushParagraph() {
    val text = paragraph.toString().trim()
    paragraph.clear()
    if (text.isNotEmpty()) blocks += MdBlock.Paragraph(text)
  }

  while (index < lines.size) {
    val line = lines[index].trimEnd()
    val trimmed = line.trimStart()

    if (trimmed.isEmpty()) {
      flushParagraph()
      index++
      continue
    }

    if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
      flushParagraph()
      val fence = trimmed.take(3)
      val language = trimmed.drop(3).trim().substringBefore(' ').lowercase()
      val code = StringBuilder()
      index++
      while (index < lines.size && !lines[index].trimStart().startsWith(fence)) {
        code.append(lines[index]).append('\n')
        index++
      }
      if (index < lines.size) index++
      blocks += MdBlock.CodeBlock(language, code.toString().trimEnd('\n'))
      continue
    }

    val hashes = trimmed.takeWhile { it == '#' }.length
    if (hashes in 1..6 && trimmed.length > hashes && trimmed[hashes] == ' ') {
      flushParagraph()
      blocks += MdBlock.Heading(hashes, trimmed.drop(hashes).trim().trimEnd('#').trim())
      index++
      continue
    }

    if (isHorizontalRule(trimmed)) {
      flushParagraph()
      blocks += MdBlock.Rule
      index++
      continue
    }

    if (trimmed.contains('|') && index + 1 < lines.size && isTableDelimiter(lines[index + 1])) {
      flushParagraph()
      val header = splitTableRow(trimmed)
      val aligns = splitTableRow(lines[index + 1].trim()).map(::tableAlign)
      index += 2
      val rows = mutableListOf<List<String>>()
      while (index < lines.size) {
        val rowLine = lines[index].trim()
        if (rowLine.isEmpty() || !rowLine.contains('|')) break
        if (rowLine.startsWith("```") || rowLine.startsWith("~~~")) break
        rows += splitTableRow(rowLine)
        index++
      }
      blocks += MdBlock.Table(header, aligns, rows)
      continue
    }

    if (trimmed.startsWith(">")) {
      flushParagraph()
      val quote = StringBuilder()
      while (index < lines.size && lines[index].trimStart().startsWith(">")) {
        if (quote.isNotEmpty()) quote.append('\n')
        quote.append(lines[index].trimStart().removePrefix(">").removePrefix(" "))
        index++
      }
      blocks += MdBlock.Quote(quote.toString().trim())
      continue
    }

    val bullet = bulletMarkerLength(trimmed)
    if (bullet > 0) {
      flushParagraph()
      val items = mutableListOf<MdListItem>()
      while (index < lines.size) {
        val raw = lines[index].trimEnd()
        val itemText = raw.trimStart()
        val indent = raw.length - itemText.length
        val marker = bulletMarkerLength(itemText)
        if (marker > 0) {
          items += MdListItem(
            depth = (indent / 2).coerceAtMost(3),
            text = itemText.drop(marker).trim(),
          )
          index++
          continue
        }
        if (items.isNotEmpty() && itemText.isNotEmpty() && indent > 0 && !startsBlock(itemText)) {
          items[items.lastIndex] = items.last().copy(text = items.last().text + " " + itemText)
          index++
          continue
        }
        break
      }
      blocks += MdBlock.BulletList(items)
      continue
    }

    val ordered = orderedMarker(trimmed)
    if (ordered != null) {
      flushParagraph()
      val items = mutableListOf<MdListItem>()
      var start = ordered.first
      while (index < lines.size) {
        val raw = lines[index].trimEnd()
        val itemText = raw.trimStart()
        val indent = raw.length - itemText.length
        val current = orderedMarker(itemText)
        if (current != null) {
          if (items.isEmpty()) start = current.first
          items += MdListItem(
            depth = (indent / 2).coerceAtMost(3),
            text = itemText.drop(current.second).trim(),
          )
          index++
          continue
        }
        if (items.isNotEmpty() && itemText.isNotEmpty() && indent > 0 && !startsBlock(itemText)) {
          items[items.lastIndex] = items.last().copy(text = items.last().text + " " + itemText)
          index++
          continue
        }
        break
      }
      blocks += MdBlock.OrderedList(start, items)
      continue
    }

    if (paragraph.isNotEmpty()) paragraph.append(' ')
    paragraph.append(trimmed)
    index++
  }

  flushParagraph()
  return blocks
}

private fun bulletMarkerLength(line: String): Int = when {
  line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> 2
  else -> 0
}

private fun orderedMarker(line: String): Pair<Int, Int>? {
  val digits = line.takeWhile { it.isDigit() }
  if (digits.isEmpty() || digits.length > 9) return null
  val rest = line.drop(digits.length)
  if (rest.length < 2) return null
  if ((rest[0] == '.' || rest[0] == ')') && rest[1] == ' ') {
    return digits.toInt() to digits.length + 2
  }
  return null
}

private fun startsBlock(line: String): Boolean =
  line.startsWith("```") ||
    line.startsWith("~~~") ||
    line.startsWith(">") ||
    bulletMarkerLength(line) > 0 ||
    orderedMarker(line) != null ||
    line.takeWhile { it == '#' }.let { it.isNotEmpty() && line.getOrNull(it.length) == ' ' }

private fun isHorizontalRule(line: String): Boolean {
  var sawMarker = false
  for (char in line) {
    when (char) {
      '-', '*', '_' -> sawMarker = true
      ' ' -> Unit
      else -> return false
    }
  }
  return sawMarker
}

private fun isTableDelimiter(line: String): Boolean {
  val trimmed = line.trim()
  if (trimmed.isEmpty() || !trimmed.contains('|')) return false
  val cells = splitTableRow(trimmed)
  if (cells.size < 2) return false
  return cells.all { cell -> cell.isNotEmpty() && cell.all { it == '-' || it == ':' } && cell.contains('-') }
}

private fun tableAlign(cell: String): MdAlign {
  val left = cell.startsWith(":")
  val right = cell.endsWith(":")
  return when {
    left && right -> MdAlign.CENTER
    right -> MdAlign.END
    else -> MdAlign.START
  }
}

private fun splitTableRow(line: String): List<String> {
  val body = line.trim().removePrefix("|").removeSuffix("|")
  val cells = mutableListOf<String>()
  val cell = StringBuilder()
  var index = 0
  while (index < body.length) {
    val char = body[index]
    if (char == '\\' && index + 1 < body.length && body[index + 1] == '|') {
      cell.append('|')
      index += 2
      continue
    }
    if (char == '|') {
      cells += cell.toString().trim()
      cell.clear()
      index++
      continue
    }
    cell.append(char)
    index++
  }
  cells += cell.toString().trim()
  return cells
}

/**
 * Renders inline markdown (code spans, bold, italic, strike, links) into an [AnnotatedString].
 * Links use [LinkAnnotation.Url] so Compose handles taps without extra plumbing.
 */
internal fun inlineMarkdown(
  text: String,
  codeBackground: Color,
  linkColor: Color,
): AnnotatedString {
  val builder = AnnotatedString.Builder()
  var index = 0

  fun appendStyled(style: SpanStyle, content: String) = builder.withStyle(style) { append(content) }

  while (index < text.length) {
    val char = text[index]
    when {
      char == '`' -> {
        val close = text.indexOf('`', index + 1)
        val newline = text.indexOf('\n', index + 1)
        if (close > index + 1 && (newline == -1 || close < newline)) {
          appendStyled(
            SpanStyle(fontFamily = JetBrainsMonoFamily, background = codeBackground),
            text.substring(index + 1, close),
          )
          index = close + 1
        } else {
          builder.append(char)
          index++
        }
      }

      text.startsWith("**", index) -> {
        val close = text.indexOf("**", index + 2)
        if (close > index + 2) {
          appendStyled(SpanStyle(fontWeight = FontWeight.Bold), text.substring(index + 2, close))
          index = close + 2
        } else {
          builder.append("**")
          index += 2
        }
      }

      text.startsWith("~~", index) -> {
        val close = text.indexOf("~~", index + 2)
        if (close > index + 2) {
          appendStyled(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            text.substring(index + 2, close),
          )
          index = close + 2
        } else {
          builder.append("~~")
          index += 2
        }
      }

      char == '*' || char == '_' -> {
        val boundaryBefore = char != '_' ||
          index == 0 ||
          !text[index - 1].isLetterOrDigit()
        val close = if (boundaryBefore) text.indexOf(char, index + 1) else -1
        val inner = if (close > index + 1) text.substring(index + 1, close) else ""
        val boundaryAfter = char != '_' ||
          close + 1 >= text.length ||
          !text[close + 1].isLetterOrDigit()
        if (close > index + 1 && inner.isNotBlank() && inner.trim() == inner &&
          inner.none { it == '\n' } && boundaryAfter
        ) {
          appendStyled(SpanStyle(fontStyle = FontStyle.Italic), inner)
          index = close + 1
        } else {
          builder.append(char)
          index++
        }
      }

      char == '[' -> {
        val labelEnd = text.indexOf("](", index + 1)
        val urlEnd = if (labelEnd > index + 1) text.indexOf(')', labelEnd + 2) else -1
        if (labelEnd > index + 1 && urlEnd > labelEnd + 2) {
          val label = text.substring(index + 1, labelEnd)
          val url = text.substring(labelEnd + 2, urlEnd)
          builder.withLink(
            LinkAnnotation.Url(
              url,
              TextLinkStyles(
                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
              ),
            ),
          ) {
            append(label)
          }
          index = urlEnd + 1
        } else {
          builder.append(char)
          index++
        }
      }

      else -> {
        builder.append(char)
        index++
      }
    }
  }
  return builder.toAnnotatedString()
}

internal data class CodePalette(
  val keyword: Color,
  val string: Color,
  val comment: Color,
  val number: Color,
)

/** Terminal style colouring: comments, strings and numbers first, keywords last. */
internal fun highlightCode(
  code: String,
  language: String,
  palette: CodePalette,
): AnnotatedString {
  val builder = AnnotatedString.Builder(code)
  val claimed = BooleanArray(code.length)
  val lang = language.lowercase()

  fun applyMatches(pattern: Regex, style: SpanStyle) {
    for (match in pattern.findAll(code)) {
      val range = match.range
      if (range.isEmpty()) continue
      var overlaps = false
      for (position in range) {
        if (claimed[position]) {
          overlaps = true
          break
        }
      }
      if (overlaps) continue
      for (position in range) claimed[position] = true
      builder.addStyle(style, range.first, range.last + 1)
    }
  }

  val commentPatterns = buildList {
    if (lang in CodeSyntax.cLikeLanguages) add(CodeSyntax.cLikeComments)
    if (lang in CodeSyntax.hashLanguages || lang.isEmpty()) add(CodeSyntax.hashComments)
    if (lang in CodeSyntax.dashLanguages) add(CodeSyntax.dashComments)
  }
  commentPatterns.forEach { applyMatches(it, SpanStyle(color = palette.comment)) }
  applyMatches(CodeSyntax.strings, SpanStyle(color = palette.string))
  applyMatches(CodeSyntax.numbers, SpanStyle(color = palette.number))
  CodeSyntax.keywords(lang)?.let { applyMatches(it, SpanStyle(color = palette.keyword)) }

  return builder.toAnnotatedString()
}

private object CodeSyntax {
  val cLikeComments = Regex("/\\*[\\s\\S]*?\\*/|//[^\\n]*")
  val hashComments = Regex("#[^\\n]*")
  val dashComments = Regex("--[^\\n]*")

  val strings = Regex(
    "\"\"\"[\\s\\S]*?\"\"\"" +
      "|\"(?:\\\\.|[^\"\\\\\\n])*\"" +
      "|'(?:\\\\.|[^'\\\\\\n])*'" +
      "|`(?:\\\\.|[^`\\\\])*`",
  )
  val numbers = Regex("\\b(?:0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?[fFlLdD]?)\\b")

  val cLikeLanguages = setOf(
    "kt", "kotlin", "kts", "java", "js", "javascript", "jsx", "ts", "typescript", "tsx",
    "c", "h", "cpp", "c++", "cc", "hpp", "cs", "csharp", "go", "golang", "rust", "rs",
    "swift", "dart", "groovy", "gradle", "jsonc", "scss", "less", "php", "scala", "proto",
    "css", "json5",
  )
  val hashLanguages = setOf(
    "py", "python", "sh", "bash", "zsh", "shell", "console", "yaml", "yml", "toml",
    "rb", "ruby", "r", "ini", "conf", "dockerfile", "makefile", "make", "properties",
    "pl", "perl", "graphql", "tex", "md", "markdown",
  )
  val dashLanguages = setOf("sql", "lua", "haskell")

  private val jvmKeywords = setOf(
    "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion",
    "const", "constructor", "continue", "crossinline", "data", "do", "else", "enum", "expect",
    "external", "false", "final", "finally", "for", "fun", "get", "if", "import", "in", "infix",
    "init", "inline", "inner", "interface", "internal", "is", "lateinit", "noinline", "null",
    "object", "open", "operator", "out", "override", "package", "private", "protected", "public",
    "reified", "return", "sealed", "set", "suspend", "super", "this", "throw", "true", "try",
    "typealias", "val", "var", "vararg", "when", "where", "while", "new", "static", "void",
    "extends", "implements", "instanceof", "final", "native", "synchronized", "volatile",
  )
  private val scriptKeywords = setOf(
    "and", "as", "assert", "async", "await", "break", "case", "catch", "class", "const",
    "continue", "debugger", "def", "default", "del", "do", "elif", "else", "except", "export",
    "extends", "False", "false", "finally", "for", "from", "function", "global", "if", "import",
    "in", "instanceof", "is", "lambda", "let", "new", "None", "not", "null", "of", "or", "pass",
    "raise", "return", "static", "super", "switch", "this", "throw", "True", "true", "try",
    "typeof", "undefined", "var", "while", "with", "yield",
  )
  private val shellKeywords = setOf(
    "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
    "function", "local", "export", "return", "set", "unset", "readonly", "in",
  )
  private val sqlKeywords = setOf(
    "select", "from", "where", "join", "inner", "left", "right", "outer", "on", "group", "by",
    "order", "having", "limit", "offset", "insert", "into", "values", "update", "set", "delete",
    "create", "alter", "drop", "table", "index", "view", "as", "and", "or", "not", "null",
    "distinct", "union", "all", "case", "when", "then", "else", "end", "exists", "primary",
    "foreign", "key", "constraint", "default", "returning", "with", "asc", "desc",
  )

  private val cache = ConcurrentHashMap<String, Regex?>()

  fun keywords(language: String): Regex? = cache.getOrPut(language) {
    val words = when (language) {
      "kt", "kotlin", "kts", "java", "groovy", "gradle", "scala", "cs", "csharp" -> jvmKeywords
      "py", "python", "js", "javascript", "jsx", "ts", "typescript", "tsx", "rb", "ruby" ->
        scriptKeywords
      "sh", "bash", "zsh", "shell", "console" -> shellKeywords
      "sql" -> sqlKeywords
      "json" -> setOf("true", "false", "null")
      else -> emptySet()
    }
    if (words.isEmpty()) {
      null
    } else {
      val options = if (language == "sql") setOf(RegexOption.IGNORE_CASE) else emptySet()
      Regex("\\b(?:" + words.joinToString("|") + ")\\b", options)
    }
  }
}

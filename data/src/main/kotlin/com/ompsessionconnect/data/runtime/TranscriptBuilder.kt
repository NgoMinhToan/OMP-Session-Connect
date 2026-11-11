package com.ompsessionconnect.data.runtime

import com.ompsessionconnect.data.protocol.compact
import com.ompsessionconnect.data.protocol.textOrNull
import com.ompsessionconnect.domain.model.AssistantMessageItem
import com.ompsessionconnect.domain.model.NoticeItem
import com.ompsessionconnect.domain.model.NoticeLevel
import com.ompsessionconnect.domain.model.SummaryItem
import com.ompsessionconnect.domain.model.ToolCallItem
import com.ompsessionconnect.domain.model.ToolStatus
import com.ompsessionconnect.domain.model.TranscriptItem
import com.ompsessionconnect.domain.model.UserMessageItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Turns collab session entries and live tool events into immutable UI rows.
 *
 * Entries arrive with stable IDs, so tool results are folded into the tool
 * card created by the assistant message instead of being rendered twice.
 */
class TranscriptBuilder {
  private val rows = mutableListOf<TranscriptItem>()
  private val toolIndices = mutableMapOf<String, Int>()
  private var sequence = 0

  val items: List<TranscriptItem> get() = rows

  fun reset() {
    rows.clear()
    toolIndices.clear()
    sequence = 0
  }

  fun snapshot(): List<TranscriptItem> = rows.toList()

  fun applyEntries(entries: List<JsonObject>) {
    entries.forEach(::applyEntry)
  }

  fun applyEntry(entry: JsonObject) {
    val timestamp = parseTimestamp(entry["timestamp"]?.textOrNull()) ?: System.currentTimeMillis()
    val entryId = entry.stringLocal("id") ?: "entry-${sequence++}"
    when (entry.stringLocal("type")) {
      "message" -> applyMessage(entry, entryId, timestamp)
      "custom_message" -> applyCustomMessage(entry, entryId, timestamp)
      "compaction" -> {
        val summary = entry.stringLocal("shortSummary") ?: entry.stringLocal("summary")
        val tokens = entry.intLocal("tokensBefore")
        val text = buildString {
          append("Context compacted")
          if (tokens != null) append(" ($tokens tokens)")
          if (!summary.isNullOrBlank()) append(": ${summary.compact(400)}")
        }
        addSummary(entryId, text, timestamp)
      }
      "branch_summary" -> {
        addSummary(entryId, "Branch summary: ${entry.stringLocal("summary").orEmpty().compact(400)}", timestamp)
      }
      "model_change" -> {
        addNotice(entryId, "Model changed to ${entry.stringLocal("model") ?: "unknown"}", NoticeLevel.INFO, timestamp)
      }
      "thinking_level_change" -> {
        val level = entry.stringLocal("thinkingLevel") ?: "off"
        addNotice(entryId, "Thinking level: $level", NoticeLevel.INFO, timestamp)
      }
      else -> Unit // Unknown future entry type: stay tolerant.
    }
  }

  fun applyToolStart(
    toolCallId: String,
    toolName: String,
    args: JsonElement?,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    val existing = toolIndices[toolCallId]
    if (existing != null) {
      val row = rows[existing]
      if (row is ToolCallItem) {
        rows[existing] = row.copy(
          arguments = args.compact(600),
          status = ToolStatus.RUNNING,
        )
      }
      return
    }
    val row = ToolCallItem(
      id = toolRowId(toolCallId),
      toolName = toolName,
      arguments = args.compact(600),
      status = ToolStatus.RUNNING,
      output = null,
      timestamp = timestamp,
    )
    toolIndices[toolCallId] = rows.size
    rows.add(row)
  }

  fun applyToolUpdate(
    toolCallId: String,
    toolName: String,
    args: JsonElement?,
    partialResult: JsonElement?,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    val index = toolIndices[toolCallId]
    if (index == null) {
      applyToolStart(toolCallId, toolName, args, timestamp)
      return
    }
    val row = rows[index] as? ToolCallItem ?: return
    rows[index] = row.copy(
      arguments = if (args != null) args.compact(600) else row.arguments,
      output = partialResult.compact(4_000).takeIf { it.isNotBlank() } ?: row.output,
      status = ToolStatus.RUNNING,
    )
  }

  fun applyToolEnd(
    toolCallId: String,
    toolName: String,
    result: JsonElement?,
    isError: Boolean,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    val output = resultText(result).compact(4_000).takeIf { it.isNotBlank() }
    val index = toolIndices[toolCallId]
    if (index == null) {
      val row = ToolCallItem(
        id = toolRowId(toolCallId),
        toolName = toolName,
        arguments = "",
        status = if (isError) ToolStatus.ERROR else ToolStatus.DONE,
        output = output,
        timestamp = timestamp,
      )
      toolIndices[toolCallId] = rows.size
      rows.add(row)
      return
    }
    val row = rows[index] as? ToolCallItem ?: return
    rows[index] = row.copy(
      status = if (isError) ToolStatus.ERROR else ToolStatus.DONE,
      output = output ?: row.output,
    )
  }

  fun addNotice(
    id: String,
    text: String,
    level: NoticeLevel,
    timestamp: Long = System.currentTimeMillis(),
  ) {
    rows.add(NoticeItem("notice:$id:${sequence++}", text, level, timestamp))
  }

  fun addSummary(id: String, text: String, timestamp: Long = System.currentTimeMillis()) {
    rows.add(SummaryItem("summary:$id:${sequence++}", text, timestamp))
  }

  fun addLocalNotice(text: String, level: NoticeLevel = NoticeLevel.WARNING) {
    addNotice("local", text, level)
  }

  private fun applyMessage(entry: JsonObject, entryId: String, timestamp: Long) {
    val message = entry.objLocal("message") ?: return
    when (message.stringLocal("role")) {
      "user" -> {
        val content = message["content"]
        val text = contentText(content)
        val images = contentImageCount(content)
        if (text.isBlank() && images == 0) return
        rows.add(
          UserMessageItem(
            id = "msg:$entryId",
            text = text,
            imageCount = images,
            from = null,
            timestamp = timestamp,
          ),
        )
      }
      "assistant" -> {
        val content = message["content"] as? JsonArray ?: return
        val text = StringBuilder()
        val thinking = StringBuilder()
        val tools = mutableListOf<JsonObject>()
        content.forEach { block ->
          val obj = block as? JsonObject ?: return@forEach
          when (obj.stringLocal("type")) {
            "text" -> obj.stringLocal("text")?.let {
              if (text.isNotEmpty()) text.append('\n')
              text.append(it)
            }
            "thinking" -> obj.stringLocal("thinking")?.let {
              if (thinking.isNotEmpty()) thinking.append('\n')
              thinking.append(it)
            }
            "toolCall" -> tools.add(obj)
          }
        }
        if (text.isNotBlank() || thinking.isNotBlank()) {
          rows.add(
            AssistantMessageItem(
              id = "msg:$entryId",
              text = text.toString(),
              thinking = thinking.toString(),
              model = message.stringLocal("model"),
              timestamp = timestamp,
            ),
          )
        }
        tools.forEach { tool ->
          val callId = tool.stringLocal("id") ?: return@forEach
          applyToolStart(callId, tool.stringLocal("name") ?: "tool", tool["arguments"], timestamp)
        }
      }
      "toolResult" -> {
        val callId = message.stringLocal("toolCallId") ?: return
        val toolName = message.stringLocal("toolName") ?: "tool"
        val output = resultText(message["content"])
        applyToolEnd(
          toolCallId = callId,
          toolName = toolName,
          result = JsonPrimitive(output),
          isError = message.booleanLocal("isError") == true,
          timestamp = timestamp,
        )
      }
    }
  }

  private fun applyCustomMessage(entry: JsonObject, entryId: String, timestamp: Long) {
    val customType = entry.stringLocal("customType")
    val content = entry["content"]
    if (customType == "collab-prompt") {
      val from = entry.objLocal("details")?.stringLocal("from")
      rows.add(
        UserMessageItem(
          id = "msg:$entryId",
          text = contentText(content),
          imageCount = contentImageCount(content),
          from = from,
          timestamp = timestamp,
        ),
      )
      return
    }
    if (entry.booleanLocal("display") == false) return
    val text = contentText(content)
    if (text.isNotBlank()) {
      addNotice(entryId, text.compact(400), NoticeLevel.INFO, timestamp)
    }
  }

  private fun contentText(content: JsonElement?): String = when (content) {
    is JsonPrimitive -> content.textOrNull().orEmpty()
    is JsonArray -> content.mapNotNull { block ->
      val obj = block as? JsonObject ?: return@mapNotNull null
      when (obj.stringLocal("type")) {
        "text" -> obj.stringLocal("text")
        "image" -> "[image]"
        else -> null
      }
    }.joinToString("\n")
    else -> ""
  }

  private fun contentImageCount(content: JsonElement?): Int {
    val array = content as? JsonArray ?: return 0
    return array.count { (it as? JsonObject)?.stringLocal("type") == "image" }
  }

  private fun resultText(result: JsonElement?): String = when (result) {
    null -> ""
    is JsonPrimitive -> result.textOrNull().orEmpty()
    is JsonArray -> contentText(result)
    is JsonObject -> {
      val content = result["content"]
      if (content != null) contentText(content) else result.compact(4_000)
    }
  }

  private fun toolRowId(toolCallId: String): String = "tool:$toolCallId"

  private fun parseTimestamp(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    return runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
      ?: runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
  }
}

private fun JsonObject.stringLocal(key: String): String? =
  (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.intLocal(key: String): Int? =
  (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

private fun JsonObject.booleanLocal(key: String): Boolean? =
  (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

private fun JsonObject.objLocal(key: String): JsonObject? = this[key] as? JsonObject

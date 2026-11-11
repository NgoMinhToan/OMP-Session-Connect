package com.ompsessionconnect.data.protocol

import com.ompsessionconnect.domain.model.TodoProgress
import com.ompsessionconnect.domain.model.TodoStatus
import com.ompsessionconnect.domain.model.TodoTask
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Extracts omp's canonical todo phases from a `todo` tool result.
 *
 * Successful results carry `details.phases`; failed calls may carry no phases
 * and must leave the previous snapshot untouched.
 */
object TodoParser {
  fun fromEntry(entry: JsonObject): TodoProgress? {
    if (entry.string("type") != "message") return null
    val message = entry.obj("message") ?: return null
    if (message.string("role") != "toolResult") return null
    if (message.string("toolName") != "todo") return null
    val details = message.obj("details") ?: entry.obj("details") ?: return null
    return fromDetails(details)
  }

  fun fromToolResult(result: JsonElement?): TodoProgress? {
    val obj = result as? JsonObject ?: return null
    return fromDetails(obj.obj("details") ?: obj)
  }

  fun fromDetails(details: JsonObject): TodoProgress? {
    val phases = details.array("phases") ?: return null
    val tasks = buildList {
      phases.forEach { phaseElement ->
        val phase = phaseElement as? JsonObject ?: return@forEach
        val phaseName = phase.string("name") ?: "Tasks"
        val phaseTasks = phase.array("tasks") ?: JsonArray(emptyList())
        phaseTasks.forEach { taskElement ->
          val task = taskElement as? JsonObject ?: return@forEach
          val content = task.string("content") ?: return@forEach
          add(
            TodoTask(
              phase = phaseName,
              content = content,
              status = task.string("status").toTodoStatus(),
              blocker = task.string("blocker")?.takeIf { it.isNotBlank() },
            ),
          )
        }
      }
    }
    return TodoProgress(tasks)
  }

  private fun String?.toTodoStatus(): TodoStatus = when (this) {
    "in_progress" -> TodoStatus.IN_PROGRESS
    "completed" -> TodoStatus.COMPLETED
    "abandoned" -> TodoStatus.ABANDONED
    "blocked" -> TodoStatus.BLOCKED
    else -> TodoStatus.PENDING
  }
}

package com.ompsessionconnect.data.protocol

import com.ompsessionconnect.domain.model.UiOption
import com.ompsessionconnect.domain.model.UiRequest
import com.ompsessionconnect.domain.model.UiRequestKind
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class RemoteModel(
  val id: String?,
  val name: String?,
  val provider: String?,
  val contextWindow: Int?,
)

data class RemoteParticipant(
  val name: String,
  val role: String,
  val readOnly: Boolean,
)

data class RemoteState(
  val isStreaming: Boolean,
  val queuedMessages: Int,
  val sessionName: String?,
  val cwd: String?,
  val model: RemoteModel?,
  val thinkingLevel: String?,
  val contextTokens: Int?,
  val contextWindow: Int?,
  val contextPercent: Int?,
  val participants: List<RemoteParticipant>,
  val isAborting: Boolean,
)

object RemoteParsers {
  fun parseState(frame: JsonObject): RemoteState {
    val state = frame.obj("state") ?: frame
    val modelObj = state.obj("model")
    val context = state.obj("contextUsage")
    val tokens = context?.int("tokens")
    val window = context?.int("contextWindow") ?: modelObj?.int("contextWindow")
    val percent = context?.let { obj ->
      (obj["percent"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()?.toInt()
    } ?: if (tokens != null && window != null && window > 0) {
      ((tokens.toDouble() / window) * 100).toInt()
    } else {
      null
    }
    val participants = state.array("participants").orEmpty().mapNotNull { element ->
      val participant = element as? JsonObject ?: return@mapNotNull null
      RemoteParticipant(
        name = participant.string("name") ?: return@mapNotNull null,
        role = participant.string("role") ?: "guest",
        readOnly = participant.boolean("readOnly") == true,
      )
    }
    return RemoteState(
      isStreaming = state.boolean("isStreaming") == true,
      queuedMessages = state.int("queuedMessageCount") ?: 0,
      sessionName = state.string("sessionName")?.takeIf { it.isNotBlank() },
      cwd = state.string("cwd"),
      model = modelObj?.let {
        RemoteModel(
          id = it.string("id"),
          name = it.string("name"),
          provider = it.string("provider"),
          contextWindow = it.int("contextWindow"),
        )
      },
      thinkingLevel = state.string("thinkingLevel"),
      contextTokens = tokens,
      contextWindow = window,
      contextPercent = percent?.coerceIn(0, 100),
      participants = participants,
      isAborting = state.boolean("isAborting") == true,
    )
  }

  fun parseUiRequest(frame: JsonObject): UiRequest? {
    val request = frame.obj("request") ?: return null
    val requestId = request.int("reqId") ?: return null
    val kind = when (request.string("kind")) {
      "select" -> UiRequestKind.SELECT
      "editor" -> UiRequestKind.EDITOR
      else -> return null
    }
    val options = request.array("options").orEmpty().mapNotNull { element ->
      when (element) {
        is JsonPrimitive -> element.contentOrNull?.let { UiOption(it) }
        is JsonObject -> element.string("label")?.let {
          UiOption(it, element.string("description"))
        }
        else -> null
      }
    }
    val checked = request.array("checkedIndices").orEmpty()
      .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }
      .toSet()
    return UiRequest(
      requestId = requestId,
      kind = kind,
      title = request.string("title") ?: "OMP needs input",
      options = options,
      prefill = request.string("prefill"),
      helpText = request.string("helpText"),
      selectionMarker = request.string("selectionMarker"),
      checkedIndices = checked,
    )
  }
}

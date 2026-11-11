package com.ompsessionconnect.data.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.nio.ByteBuffer

/** Constants and JSON helpers for the omp collab wire protocol (proto 3). */
object Wire {
  const val PROTO = 3
  const val ENVELOPE_HEADER_LENGTH = 4
  const val ROOM_KEY_BYTES = 32
  const val WRITE_TOKEN_BYTES = 16

  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
  }

  fun parseObject(text: String): JsonObject? =
    runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()

  /** `[4-byte BE peer id][sealed payload]`; guests always send peer id 0. */
  fun pack(peerId: Int, sealed: ByteArray): ByteArray =
    ByteBuffer.allocate(ENVELOPE_HEADER_LENGTH + sealed.size)
      .putInt(peerId)
      .put(sealed)
      .array()

  fun unpack(data: ByteArray): Pair<Int, ByteArray>? {
    if (data.size < ENVELOPE_HEADER_LENGTH) return null
    val buffer = ByteBuffer.wrap(data)
    val peerId = buffer.int
    val payload = ByteArray(buffer.remaining()).also(buffer::get)
    return peerId to payload
  }

  fun hello(name: String, writeToken: String?): JsonObject = buildJsonObject {
    put("t", "hello")
    put("proto", PROTO)
    put("name", name)
    if (writeToken != null) put("writeToken", writeToken)
  }

  fun prompt(text: String): JsonObject = buildJsonObject {
    put("t", "prompt")
    put("text", text)
  }

  fun abort(): JsonObject = buildJsonObject { put("t", "abort") }

  fun uiResponse(requestId: Int, value: String?): JsonObject = buildJsonObject {
    put("t", "ui-response")
    put("reqId", requestId)
    if (value != null) put("value", value)
  }
}

fun JsonObject.string(key: String): String? =
  (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonObject.int(key: String): Int? =
  (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

fun JsonObject.long(key: String): Long? =
  (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

fun JsonObject.boolean(key: String): Boolean? =
  (this[key] as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

fun JsonElement?.textOrNull(): String? = when (this) {
  is JsonPrimitive -> contentOrNull
  else -> null
}

fun JsonElement?.compact(limit: Int = 240): String = when (this) {
  null -> ""
  is JsonPrimitive -> (contentOrNull ?: toString()).compact(limit)
  else -> toString().compact(limit)
}

fun String.compact(limit: Int = 240): String {
  val singleLine = replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
  return if (singleLine.length <= limit) singleLine else singleLine.take(limit - 1) + "…"
}

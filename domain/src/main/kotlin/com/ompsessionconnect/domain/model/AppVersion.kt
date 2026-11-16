package com.ompsessionconnect.domain.model

/** Numeric version helpers shared by the release lookup and the updater. */
object AppVersion {
  /** `v1.0.42` → `1.0.42`; null when the value is not numeric. */
  fun normalize(raw: String): String? {
    val core = raw.trim()
      .removePrefix("v")
      .removePrefix("V")
      .substringBefore('-')
      .substringBefore('+')
    val parts = core.split('.')
    if (parts.isEmpty() || parts.size > 4) return null
    val parsed = parts.map { part ->
      if (part.isEmpty() || part.length > 9) return null
      part.toIntOrNull() ?: return null
    }
    return parsed.joinToString(".")
  }

  /** True when [candidate] is strictly newer than [current]. */
  fun isNewer(candidate: String, current: String): Boolean {
    val next = parts(candidate) ?: return false
    val installed = parts(current) ?: return false
    for (index in 0 until maxOf(next.size, installed.size)) {
      val a = next.getOrElse(index) { 0 }
      val b = installed.getOrElse(index) { 0 }
      if (a != b) return a > b
    }
    return false
  }

  private fun parts(value: String): List<Int>? =
    normalize(value)?.split('.')?.map { it.toInt() }
}

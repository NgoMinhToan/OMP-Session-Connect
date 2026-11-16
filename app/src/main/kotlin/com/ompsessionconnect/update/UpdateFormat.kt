package com.ompsessionconnect.update

import java.util.Locale

/** Coarse byte size for notifications and settings, e.g. `12.4 MB`. */
fun formatBytes(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB")
  var value = bytes.toDouble()
  var unit = 0
  while (value >= 1024 && unit < units.lastIndex) {
    value /= 1024
    unit++
  }
  return if (unit == 0) {
    "${bytes} ${units[unit]}"
  } else {
    String.format(Locale.US, "%.1f %s", value, units[unit])
  }
}

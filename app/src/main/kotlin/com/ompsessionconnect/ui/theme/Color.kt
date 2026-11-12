package com.ompsessionconnect.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fallback palette for Live Glass. Dynamic color keeps these semantics but
// replaces primary/containers and tints the field from the wallpaper.
val LiveGlassLightPrimary = Color(0xFF006A60)
val LiveGlassLightOnPrimary = Color(0xFFFFFFFF)
val LiveGlassLightSecondary = Color(0xFF4A5CC9)
val LiveGlassLightInk = Color(0xFF101418)
val LiveGlassLightMuted = Color(0xFF4E5A66)

val LiveGlassDarkPrimary = Color(0xFF53DBC9)
val LiveGlassDarkOnPrimary = Color(0xFF003731)
val LiveGlassDarkInk = Color(0xFFF2F5F7)
val LiveGlassDarkMuted = Color(0xFFA9B4C0)

val LiveGlassLightFieldA = Color(0xFFE9FBF6)
val LiveGlassLightFieldB = Color(0xFFE4EDFF)
val LiveGlassLightFieldC = Color(0xFFF3E8FF)
val LiveGlassDarkFieldA = Color(0xFF071318)
val LiveGlassDarkFieldB = Color(0xFF0C1230)
val LiveGlassDarkFieldC = Color(0xFF1B1030)

val LiveGlassLightGlass = Color(0x8CFFFFFF) // 55%
val LiveGlassLightGlassStrong = Color(0xC2FFFFFF) // 76%
val LiveGlassLightSheetOpaque = Color(0xFFFFFFFF) // opaque reading sheet
val LiveGlassLightControl = Color(0xC2FFFFFF) // controls share one fill
val LiveGlassLightEdge = Color(0xBFFFFFFF) // 75%
val LiveGlassLightInset = Color(0x57FFFFFF)
val LiveGlassLightHairline = Color(0x14101418)

val LiveGlassDarkGlass = Color(0x8C141826) // rgba(20,24,38,.55)
val LiveGlassDarkGlassStrong = Color(0xCC1B2031)
val LiveGlassDarkSheetOpaque = Color(0xFF141826)
val LiveGlassDarkControl = Color(0xCC1B2031)
val LiveGlassDarkEdge = Color(0x29FFFFFF) // 16%
val LiveGlassDarkInset = Color(0x0FFFFFFF)
val LiveGlassDarkHairline = Color(0x17FFFFFF)

val LiveGlassLightShadow = Color(0x47183046)
val LiveGlassDarkShadow = Color(0x80000000)

val LiveGlassLightBloomA = Color(0x380E8F8A)
val LiveGlassLightBloomB = Color(0x2EB4457A)
val LiveGlassDarkBloomA = Color(0x2453DBC9)
val LiveGlassDarkBloomB = Color(0x38B4457A)

@Immutable
data class SessionStatusPalette(
  val live: Color,
  val liveText: Color,
  val working: Color,
  val workingText: Color,
  val waiting: Color,
  val waitingText: Color,
  val error: Color,
  val errorText: Color,
  val watching: Color,
  val ended: Color,
  val completed: Color,
  val progress: Color,
  val pending: Color,
  val blocked: Color,
)

/** Semantic status colors are intentionally independent of Material roles. */
object SessionStatusColors {
  val Light = SessionStatusPalette(
    live = Color(0xFF0F9D74),
    liveText = Color(0xFF0B7A57),
    working = Color(0xFF3B6FE0),
    workingText = Color(0xFF2C5BC4),
    waiting = Color(0xFFE8590C),
    waitingText = Color(0xFFA63E00),
    error = Color(0xFFD64545),
    errorText = Color(0xFFC23434),
    watching = Color(0xFF4E5A66),
    ended = Color(0xFF68737A),
    completed = Color(0xFF68737A),
    progress = Color(0xFF3B6FE0),
    pending = Color(0xFF68737A),
    blocked = Color(0xFFD64545),
  )

  val Dark = SessionStatusPalette(
    live = Color(0xFF34D399),
    liveText = Color(0xFF34D399),
    working = Color(0xFF7FA9FF),
    workingText = Color(0xFF7FA9FF),
    waiting = Color(0xFFFFAB70),
    waitingText = Color(0xFFFFAB70),
    error = Color(0xFFFF8A80),
    errorText = Color(0xFFFF8A80),
    watching = Color(0xFFA9B4C0),
    ended = Color(0xFF93A0AD),
    completed = Color(0xFFA9B4C0),
    progress = Color(0xFF7FA9FF),
    pending = Color(0xFFA9B4C0),
    blocked = Color(0xFFFF8A80),
  )
}

val LocalSessionStatusColors = staticCompositionLocalOf { SessionStatusColors.Light }

@Immutable
data class LiveGlassTokens(
  val dark: Boolean,
  val fieldA: Color,
  val fieldB: Color,
  val fieldC: Color,
  val bloomA: Color,
  val bloomB: Color,
  val glass: Color,
  val glassStrong: Color,
  val sheetOpaque: Color,
  val control: Color,
  val glassInset: Color,
  val edge: Color,
  val hairline: Color,
  val ink: Color,
  val muted: Color,
  val primary: Color,
  val onPrimary: Color,
  val inkFill: Color,
  val onInk: Color,
  val shadow: Color,
)

val DefaultLiveGlassTokens = LiveGlassTokens(
  dark = false,
  fieldA = LiveGlassLightFieldA,
  fieldB = LiveGlassLightFieldB,
  fieldC = LiveGlassLightFieldC,
  bloomA = LiveGlassLightBloomA,
  bloomB = LiveGlassLightBloomB,
  glass = LiveGlassLightGlass,
  glassStrong = LiveGlassLightGlassStrong,
  sheetOpaque = LiveGlassLightSheetOpaque,
  control = LiveGlassLightControl,
  glassInset = LiveGlassLightInset,
  edge = LiveGlassLightEdge,
  hairline = LiveGlassLightHairline,
  ink = LiveGlassLightInk,
  muted = LiveGlassLightMuted,
  primary = LiveGlassLightPrimary,
  onPrimary = LiveGlassLightOnPrimary,
  inkFill = LiveGlassLightInk,
  onInk = Color(0xFFFFFFFF),
  shadow = LiveGlassLightShadow,
)

val LocalLiveGlass = staticCompositionLocalOf { DefaultLiveGlassTokens }

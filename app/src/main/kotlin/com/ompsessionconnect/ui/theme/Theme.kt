package com.ompsessionconnect.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

private fun fallbackLightScheme() = lightColorScheme(
  primary = LiveGlassLightPrimary,
  onPrimary = LiveGlassLightOnPrimary,
  primaryContainer = Color(0xFFA8F2E7),
  onPrimaryContainer = Color(0xFF00201C),
  secondary = LiveGlassLightSecondary,
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFDFE2FF),
  onSecondaryContainer = Color(0xFF11194A),
  tertiary = Color(0xFF3B6FE0),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFDDE7FF),
  onTertiaryContainer = Color(0xFF001A41),
  background = Color.Transparent,
  onBackground = LiveGlassLightInk,
  surface = Color(0xFFF7FBFB),
  onSurface = LiveGlassLightInk,
  surfaceVariant = Color(0xFFE6EEF3),
  onSurfaceVariant = LiveGlassLightMuted,
  outline = Color(0xFF6F7A86),
  outlineVariant = Color(0xFFD7E0E8),
  error = Color(0xFFD64545),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  inverseSurface = Color(0xFF2A3038),
  inverseOnSurface = Color(0xFFF2F5F7),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF8FCFD),
  surfaceContainer = Color(0xFFF2F7FA),
  surfaceContainerHigh = Color(0xFFEDF3F8),
  surfaceContainerHighest = Color(0xFFE7EFF5),
)

private fun fallbackDarkScheme() = darkColorScheme(
  primary = LiveGlassDarkPrimary,
  onPrimary = LiveGlassDarkOnPrimary,
  primaryContainer = Color(0xFF005048),
  onPrimaryContainer = Color(0xFF74F8E5),
  secondary = Color(0xFFB1C4FF),
  onSecondary = Color(0xFF101638),
  secondaryContainer = Color(0xFF2A3566),
  onSecondaryContainer = Color(0xFFDFE2FF),
  tertiary = Color(0xFF7FA9FF),
  onTertiary = Color(0xFF001A41),
  tertiaryContainer = Color(0xFF17305C),
  onTertiaryContainer = Color(0xFFDDE7FF),
  background = Color.Transparent,
  onBackground = LiveGlassDarkInk,
  surface = Color(0xFF0C111B),
  onSurface = LiveGlassDarkInk,
  surfaceVariant = Color(0xFF1E2735),
  onSurfaceVariant = LiveGlassDarkMuted,
  outline = Color(0xFF7A8898),
  outlineVariant = Color(0xFF2A3542),
  error = Color(0xFFFF8A80),
  onError = Color(0xFF410002),
  errorContainer = Color(0xFF5C1A18),
  onErrorContainer = Color(0xFFFFDAD6),
  inverseSurface = Color(0xFFE6EDF4),
  inverseOnSurface = Color(0xFF101418),
  surfaceContainerLowest = Color(0xFF080D14),
  surfaceContainerLow = Color(0xFF0B111A),
  surfaceContainer = Color(0xFF0F1621),
  surfaceContainerHigh = Color(0xFF131C28),
  surfaceContainerHighest = Color(0xFF182230),
)

private fun withLiveGlassSurfaces(scheme: androidx.compose.material3.ColorScheme, dark: Boolean) =
  scheme.copy(
    background = Color.Transparent,
    onBackground = if (dark) LiveGlassDarkInk else LiveGlassLightInk,
    surface = if (dark) Color(0xFF0C111B) else Color(0xFFF7FBFB),
    onSurface = if (dark) LiveGlassDarkInk else LiveGlassLightInk,
    surfaceVariant = if (dark) Color(0xFF1E2735) else Color(0xFFE6EEF3),
    onSurfaceVariant = if (dark) LiveGlassDarkMuted else LiveGlassLightMuted,
    outline = if (dark) Color(0xFF7A8898) else Color(0xFF6F7A86),
    outlineVariant = if (dark) Color(0xFF2A3542) else Color(0xFFD7E0E8),
    error = if (dark) SessionStatusColors.Dark.error else SessionStatusColors.Light.error,
    onError = if (dark) Color(0xFF410002) else Color(0xFFFFFFFF),
    errorContainer = if (dark) Color(0xFF5C1A18) else Color(0xFFFFDAD6),
    onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002),
    inverseSurface = if (dark) Color(0xFFE6EDF4) else Color(0xFF2A3038),
    inverseOnSurface = if (dark) Color(0xFF101418) else Color(0xFFF2F5F7),
    surfaceContainerLowest = if (dark) Color(0xFF080D14) else Color(0xFFFFFFFF),
    surfaceContainerLow = if (dark) Color(0xFF0B111A) else Color(0xFFF8FCFD),
    surfaceContainer = if (dark) Color(0xFF0F1621) else Color(0xFFF2F7FA),
    surfaceContainerHigh = if (dark) Color(0xFF131C28) else Color(0xFFEDF3F8),
    surfaceContainerHighest = if (dark) Color(0xFF182230) else Color(0xFFE7EFF5),
  )

@Composable
fun OmpSessionConnectTheme(
  darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val dynamicActive = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val baseScheme = when {
    dynamicActive -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> fallbackDarkScheme()
    else -> fallbackLightScheme()
  }
  val scheme = withLiveGlassSurfaces(baseScheme, darkTheme)
  val status = if (darkTheme) SessionStatusColors.Dark else SessionStatusColors.Light
  val tokens = remember(
    darkTheme,
    dynamicActive,
    scheme.primary,
    scheme.tertiary,
    scheme.onPrimary,
  ) {
    liveGlassTokens(
      dark = darkTheme,
      dynamic = dynamicActive,
      primary = scheme.primary,
      onPrimary = scheme.onPrimary,
      tertiary = scheme.tertiary,
    )
  }
  CompositionLocalProvider(
    LocalLiveGlass provides tokens,
    LocalSessionStatusColors provides status,
  ) {
    MaterialTheme(
      colorScheme = scheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}

private fun liveGlassTokens(
  dark: Boolean,
  dynamic: Boolean,
  primary: Color,
  onPrimary: Color,
  tertiary: Color,
): LiveGlassTokens {
  val field = if (dynamic) {
    dynamicField(primary, tertiary, dark)
  } else {
    if (dark) {
      Triple(LiveGlassDarkFieldA, LiveGlassDarkFieldB, LiveGlassDarkFieldC)
    } else {
      Triple(LiveGlassLightFieldA, LiveGlassLightFieldB, LiveGlassLightFieldC)
    }
  }
  return LiveGlassTokens(
    dark = dark,
    fieldA = field.first,
    fieldB = field.second,
    fieldC = field.third,
    bloomA = if (dynamic) primary.copy(alpha = if (dark) 0.20f else 0.10f) else {
      if (dark) LiveGlassDarkBloomA else LiveGlassLightBloomA
    },
    bloomB = if (dynamic) tertiary.copy(alpha = if (dark) 0.16f else 0.08f) else {
      if (dark) LiveGlassDarkBloomB else LiveGlassLightBloomB
    },
    glass = if (dark) LiveGlassDarkGlass else LiveGlassLightGlass,
    glassStrong = if (dark) LiveGlassDarkGlassStrong else LiveGlassLightGlassStrong,
    sheetOpaque = if (dark) LiveGlassDarkSheetOpaque else LiveGlassLightSheetOpaque,
    control = if (dark) LiveGlassDarkControl else LiveGlassLightControl,
    glassInset = if (dark) LiveGlassDarkInset else LiveGlassLightInset,
    edge = if (dark) LiveGlassDarkEdge else LiveGlassLightEdge,
    hairline = if (dark) LiveGlassDarkHairline else LiveGlassLightHairline,
    ink = if (dark) LiveGlassDarkInk else LiveGlassLightInk,
    muted = if (dark) LiveGlassDarkMuted else LiveGlassLightMuted,
    primary = primary,
    onPrimary = onPrimary,
    inkFill = if (dark) Color(0xFFF2F5F7) else Color(0xFF101418),
    onInk = if (dark) Color(0xFF10141A) else Color(0xFFFFFFFF),
    shadow = if (dark) LiveGlassDarkShadow else LiveGlassLightShadow,
  )
}

private fun dynamicField(primary: Color, tertiary: Color, dark: Boolean): Triple<Color, Color, Color> {
  val primaryHsl = FloatArray(3)
  val tertiaryHsl = FloatArray(3)
  ColorUtils.colorToHSL(primary.toArgb(), primaryHsl)
  ColorUtils.colorToHSL(tertiary.toArgb(), tertiaryHsl)
  val saturation = if (dark) 0.65f else 0.75f
  val lightness = if (dark) floatArrayOf(0.07f, 0.10f, 0.13f) else floatArrayOf(0.94f, 0.93f, 0.95f)
  fun stop(hue: Float, index: Int): Color = Color(
    ColorUtils.HSLToColor(
      floatArrayOf(
        (hue % 360f + 360f) % 360f,
        saturation,
        lightness[index],
      ),
    ),
  )
  return Triple(
    stop(primaryHsl[0] + 25f, 0),
    stop(primaryHsl[0] + 210f, 1),
    stop(tertiaryHsl[0] + 300f, 2),
  )
}

/** Full-screen gradient field with two soft wallpaper-derived blooms. */
@Composable
fun LiveGlassBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val tokens = LocalLiveGlass.current
  Box(
    modifier = modifier.drawWithCache {
      val base = Brush.linearGradient(
        colors = listOf(tokens.fieldA, tokens.fieldB, tokens.fieldC),
        start = Offset.Zero,
        end = Offset(size.width, size.height * 1.15f),
      )
      val bloomA = Brush.radialGradient(
        colors = listOf(tokens.bloomA, tokens.bloomA.copy(alpha = 0f)),
        center = Offset(size.width * 0.08f, -size.height * 0.04f),
        radius = size.maxDimension * 0.82f,
      )
      val bloomB = Brush.radialGradient(
        colors = listOf(tokens.bloomB, tokens.bloomB.copy(alpha = 0f)),
        center = Offset(size.width * 1.02f, size.height * 1.04f),
        radius = size.maxDimension * 0.78f,
      )
      onDrawBehind {
        drawRect(base)
        drawRect(bloomA)
        drawRect(bloomB)
      }
    },
    content = content,
  )
}

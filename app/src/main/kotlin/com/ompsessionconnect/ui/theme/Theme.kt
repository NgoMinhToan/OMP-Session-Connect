package com.ompsessionconnect.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
  primary = TealPrimaryLight,
  onPrimary = TealOnPrimaryLight,
  primaryContainer = TealContainerLight,
  onPrimaryContainer = TealOnContainerLight,
  secondary = SlateSecondaryLight,
  onSecondary = SlateOnSecondaryLight,
  secondaryContainer = SlateContainerLight,
  onSecondaryContainer = SlateOnContainerLight,
  tertiary = SkyTertiaryLight,
  onTertiary = SkyOnTertiaryLight,
  tertiaryContainer = SkyContainerLight,
  onTertiaryContainer = SkyOnContainerLight,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
  primary = TealPrimaryDark,
  onPrimary = TealOnPrimaryDark,
  primaryContainer = TealContainerDark,
  onPrimaryContainer = TealOnContainerDark,
  secondary = SlateSecondaryDark,
  onSecondary = SlateOnSecondaryDark,
  secondaryContainer = SlateContainerDark,
  onSecondaryContainer = SlateOnContainerDark,
  tertiary = SkyTertiaryDark,
  onTertiary = SkyOnTertiaryDark,
  tertiaryContainer = SkyContainerDark,
  onTertiaryContainer = SkyOnContainerDark,
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
)

@Composable
fun OmpSessionConnectTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colors = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColors
    else -> LightColors
  }
  MaterialTheme(
    colorScheme = colors,
    typography = AppTypography,
    content = content,
  )
}

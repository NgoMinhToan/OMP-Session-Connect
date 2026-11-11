package com.ompsessionconnect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val AppTypography = Typography(
  displayLarge = base.displayLarge,
  displayMedium = base.displayMedium,
  displaySmall = base.displaySmall,
  headlineLarge = base.headlineLarge,
  headlineMedium = base.headlineMedium,
  headlineSmall = base.headlineSmall,
  titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
  titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
  titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium),
  bodyLarge = base.bodyLarge,
  bodyMedium = base.bodyMedium,
  bodySmall = base.bodySmall,
  labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
  labelMedium = base.labelMedium,
  labelSmall = base.labelSmall,
)

val CodeTextStyle = TextStyle(
  fontFamily = FontFamily.Monospace,
  fontSize = 13.sp,
  lineHeight = 18.sp,
)

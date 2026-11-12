package com.ompsessionconnect.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ompsessionconnect.R

val FigtreeFamily = FontFamily(
  Font(R.font.figtree_regular, FontWeight.Normal),
  Font(R.font.figtree_medium, FontWeight.Medium),
  Font(R.font.figtree_semibold, FontWeight.SemiBold),
  Font(R.font.figtree_bold, FontWeight.Bold),
  Font(R.font.figtree_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMonoFamily = FontFamily(
  Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
  Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

val AppTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.02).sp,
  ),
  displayMedium = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.02).sp,
  ),
  displaySmall = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.02).sp,
  ),
  headlineLarge = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.02).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.015).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.01).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.01).sp,
  ),
  titleMedium = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.01.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.01.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.01.sp,
  ),
)

val CodeTextStyle = TextStyle(
  fontFamily = JetBrainsMonoFamily,
  fontWeight = FontWeight.Normal,
  fontSize = 13.sp,
  lineHeight = 18.sp,
)

val CodeMediumTextStyle = CodeTextStyle.copy(fontWeight = FontWeight.Medium)

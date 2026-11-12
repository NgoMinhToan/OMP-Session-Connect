package com.ompsessionconnect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(12.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

object LiveGlassShapes {
  val Hero = RoundedCornerShape(24.dp)
  val Room = RoundedCornerShape(18.dp)
  val Composer = RoundedCornerShape(26.dp)
  val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  val Dialog = RoundedCornerShape(28.dp)
  val Panel = RoundedCornerShape(24.dp)
  val Pill = RoundedCornerShape(999.dp)
  val Dock = RoundedCornerShape(31.dp)
  val DockItem = RoundedCornerShape(25.dp)
  val IconButton = RoundedCornerShape(999.dp)
}

package com.ompsessionconnect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.ui.theme.LocalLiveGlass

@Composable
fun GlassIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalLiveGlass.current
  Box(
    modifier = modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(tokens.control)
      .semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
        role = Role.Button
        onClick {
          onClick()
          true
        }
      }
      .pointerInput(onClick) {
        detectTapGestures(onTap = { onClick() })
      },
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = tokens.ink,
    )
  }
}

/**
 * Fades the top and bottom of a scrolling layer into the gradient behind it.
 * Uses an offscreen layer so DstOut only removes this layer's content.
 */
fun Modifier.edgeFade(
  top: Dp = 18.dp,
  bottom: Dp = 24.dp,
): Modifier = this
  .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
  .drawWithContent {
    drawContent()
    if (top > 0.dp) {
      val height = top.toPx().coerceAtMost(size.height / 2f)
      drawRect(
        brush = Brush.verticalGradient(
          colorStops = arrayOf(0f to Color.Black, 1f to Color.Transparent),
          startY = 0f,
          endY = height,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, height),
        blendMode = BlendMode.DstOut,
      )
    }
    if (bottom > 0.dp) {
      val height = bottom.toPx().coerceAtMost(size.height / 2f)
      drawRect(
        brush = Brush.verticalGradient(
          colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black),
          startY = size.height - height,
          endY = size.height,
        ),
        topLeft = Offset(0f, size.height - height),
        size = Size(size.width, height),
        blendMode = BlendMode.DstOut,
      )
    }
  }

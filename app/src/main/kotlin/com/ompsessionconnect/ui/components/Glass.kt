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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import android.os.Build
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

/**
 * API 31+: blurs the top and bottom strips of the scrolling layer, then fades the
 * outer edge. Older APIs keep the alpha-only [edgeFade] behavior.
 */
@Composable
fun Modifier.edgeBlurFade(
  top: Dp = 42.dp,
  bottom: Dp = 42.dp,
  fadeTop: Dp = 18.dp,
  fadeBottom: Dp = 26.dp,
  blurRadius: Dp = 8.dp,
): Modifier {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
    return edgeFade(top = fadeTop, bottom = fadeBottom)
  }
  val layer = rememberGraphicsLayer()
  val density = LocalDensity.current
  val topPx = with(density) { top.toPx() }
  val bottomPx = with(density) { bottom.toPx() }
  val fadeTopPx = with(density) { fadeTop.toPx() }
  val fadeBottomPx = with(density) { fadeBottom.toPx() }
  val blurPx = with(density) { blurRadius.toPx() }
  return this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
      val width = size.width.toInt()
      val height = size.height.toInt()
      layer.record(density, layoutDirection, IntSize(width, height)) {
        this@drawWithContent.drawContent()
      }
      clipRect(
        left = 0f,
        top = topPx,
        right = size.width,
        bottom = (size.height - bottomPx).coerceAtLeast(topPx),
      ) {
        this@drawWithContent.drawContent()
      }
      layer.renderEffect = BlurEffect(blurPx, blurPx)
      clipRect(left = 0f, top = 0f, right = size.width, bottom = topPx) {
        drawLayer(layer)
      }
      clipRect(
        left = 0f,
        top = (size.height - bottomPx).coerceAtLeast(0f),
        right = size.width,
        bottom = size.height,
      ) {
        drawLayer(layer)
      }
      if (fadeTopPx > 0f) {
        drawRect(
          brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Black, 1f to Color.Transparent),
            startY = 0f,
            endY = fadeTopPx.coerceAtMost(size.height / 2f),
          ),
          topLeft = Offset.Zero,
          size = Size(size.width, fadeTopPx.coerceAtMost(size.height / 2f)),
          blendMode = BlendMode.DstOut,
        )
      }
      if (fadeBottomPx > 0f) {
        val h = fadeBottomPx.coerceAtMost(size.height / 2f)
        drawRect(
          brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black),
            startY = size.height - h,
            endY = size.height,
          ),
          topLeft = Offset(0f, size.height - h),
          size = Size(size.width, h),
          blendMode = BlendMode.DstOut,
        )
      }
    }
}

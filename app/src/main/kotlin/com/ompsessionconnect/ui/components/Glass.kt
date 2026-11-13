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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ompsessionconnect.ui.theme.LiveGlassTokens
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

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

/** Which side of a chrome box fades into the scrolling content beneath it. */
enum class ChromeEdge { Top, Bottom }

/**
 * Length of the alpha ramp at the inner edge of every chrome box. The rest of the box stays
 * fully opaque, so the glass keeps the weight it had before.
 */
val ChromeFadeBand = 20.dp

private fun lerpSrgb(a: Color, b: Color, t: Float): Color = Color(
  red = a.red + (b.red - a.red) * t,
  green = a.green + (b.green - a.green) * t,
  blue = a.blue + (b.blue - a.blue) * t,
  alpha = a.alpha + (b.alpha - a.alpha) * t,
)

/**
 * Colour of the gradient field painted by `LiveGlassBackground` at `(widthPx / 2, yPx)`.
 * The background is a linear gradient A→B→C running from `(0, 0)` to `(width, height * 1.15)`.
 */
fun fieldColorAt(
  tokens: LiveGlassTokens,
  widthPx: Float,
  heightPx: Float,
  yPx: Float,
): Color {
  val endX = widthPx
  val endY = heightPx * 1.15f
  val denominator = endX * endX + endY * endY
  if (denominator <= 0f) return tokens.fieldA
  val t = (((widthPx / 2f) * endX + yPx * endY) / denominator).coerceIn(0f, 1f)
  return if (t <= 0.5f) {
    lerpSrgb(tokens.fieldA, tokens.fieldB, t * 2f)
  } else {
    lerpSrgb(tokens.fieldB, tokens.fieldC, (t - 0.5f) * 2f)
  }
}

/**
 * Opaque across the box, ramping to alpha 0 over [bandFraction] of the box height at the edge
 * that faces the content. ShaderBrush so the ramp tracks the effect's real size.
 */
private fun chromeEdgeMask(edge: ChromeEdge, bandFraction: Float): Brush =
  object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
      val fraction = bandFraction.coerceIn(0f, 1f)
      val colors = if (edge == ChromeEdge.Top) {
        listOf(Color.White, Color.White, Color.Transparent)
      } else {
        listOf(Color.Transparent, Color.White, Color.White)
      }
      val stops = if (edge == ChromeEdge.Top) {
        listOf(0f, 1f - fraction, 1f)
      } else {
        listOf(0f, fraction, 1f)
      }
      return LinearGradientShader(
        from = Offset(0f, 0f),
        to = Offset(0f, size.height),
        colors = colors,
        colorStops = stops,
      )
    }
  }

/**
 * Same glass as before (opaque [field] fill, 24.dp blur, tint), plus a mask that fades the
 * inner [ChromeFadeBand] into the content.
 */
fun chromeBlurStyle(
  field: Color,
  dark: Boolean,
  edge: ChromeEdge,
  bandFraction: Float,
): HazeBlurStyle = HazeBlurStyle {
  blurRadius(24.dp)
  backgroundColor(field)
  colorEffects(listOf(HazeColorEffect.tint(field.copy(alpha = if (dark) 0.45f else 0.35f))))
  mask(chromeEdgeMask(edge, bandFraction))
  blurredEdgeTreatment(BlurredEdgeTreatment.Rectangle)
  noiseFactor(0f)
}

fun Modifier.chromeBlur(
  hazeState: HazeState,
  style: HazeBlurStyle,
  enabled: Boolean,
): Modifier = if (enabled) {
  hazeBlur(input = HazeInput.Sources(hazeState), style = style)
} else {
  Modifier
}

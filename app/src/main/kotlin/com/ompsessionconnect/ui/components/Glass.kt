package com.ompsessionconnect.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
  Surface(
    modifier = modifier.size(48.dp),
    shape = CircleShape,
    color = tokens.glass,
    border = BorderStroke(1.dp, tokens.edge),
    shadowElevation = 4.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable(onClick = onClick),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(20.dp),
        tint = tokens.ink,
      )
    }
  }
}

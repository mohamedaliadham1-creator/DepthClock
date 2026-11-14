package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DepthClockLogoMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
  val primary = MaterialTheme.colorScheme.primary
  val onSurface = MaterialTheme.colorScheme.onSurface
  val container = MaterialTheme.colorScheme.primaryContainer
  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(10.dp))
        .background(
          Brush.linearGradient(
            listOf(container.copy(alpha = 0.9f), MaterialTheme.colorScheme.surfaceContainerHigh),
          ),
        )
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
      val w = this.size.width
      val h = this.size.height
      // Subtle clock bars behind spire
      drawRoundRect(
        color = primary.copy(alpha = 0.78f),
        topLeft = Offset(w * 0.14f, h * 0.14f),
        size = Size(w * 0.18f, h * 0.50f),
        cornerRadius = CornerRadius(3f, 3f),
      )
      drawRoundRect(
        color = primary.copy(alpha = 0.78f),
        topLeft = Offset(w * 0.42f, h * 0.14f),
        size = Size(w * 0.34f, h * 0.16f),
        cornerRadius = CornerRadius(3f, 3f),
      )
      // Foreground architectural spire overlapping clock
      val spire =
        Path().apply {
          moveTo(w * 0.56f, h * 0.18f)
          lineTo(w * 0.88f, h * 0.90f)
          lineTo(w * 0.24f, h * 0.90f)
          close()
        }
      drawPath(spire, color = onSurface)
    }
  }
}

@Composable
fun LiquidGlassPanel(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 22.dp,
  content: @Composable BoxScope.() -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(cornerRadius),
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    tonalElevation = 4.dp,
    shadowElevation = 8.dp,
  ) {
    Box(content = content)
  }
}

@Composable
fun RealisticPhoneFrame(
  bitmap: Bitmap?,
  aspectRatio: Float = 9f / 20f,
  modifier: Modifier = Modifier,
  overlayContent: @Composable BoxScope.() -> Unit = {},
) {
  val frameShape = RoundedCornerShape(34.dp)
  val screenShape = RoundedCornerShape(28.dp)

  Box(
    modifier =
      modifier
        .aspectRatio(aspectRatio)
        .clip(frameShape)
        .background(
          Brush.linearGradient(
            listOf(Color(0xFF28344E), Color(0xFF0E1422), Color(0xFF1E2942)),
          ),
        )
        .border(1.5.dp, Color(0x4DFFFFFF), frameShape)
        .padding(6.dp),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .clip(screenShape)
          .background(MaterialTheme.colorScheme.background),
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = "Lock screen depth wallpaper preview",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }

      // Subtle punch-hole camera & top status hints like Redmi Note 11
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "5G",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.72f),
        )
        Box(
          modifier =
            Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(Color(0xFF05070D))
              .border(0.8.dp, Color(0x33FFFFFF), CircleShape),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .width(16.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(2.dp))
                .padding(1.5.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(1.dp)),
            )
          }
        }
      }

      // Bottom lock-screen gesture bar
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
            .width(64.dp)
            .height(3.5.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.55f)),
      )

      overlayContent()
    }
  }
}

@Composable
fun EditorialSectionHeader(
  title: String,
  actionText: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onBackground,
    )
    if (actionText != null && onAction != null) {
      Surface(
        onClick = onAction,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
      ) {
        Text(
          text = actionText,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
      }
    }
  }
}

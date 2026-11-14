package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

private val DepthClockDarkColorScheme =
  darkColorScheme(
    primary = LuminousIce,
    onPrimary = MidnightNavy,
    primaryContainer = DeepSapphire,
    onPrimaryContainer = IceWhite,
    secondary = Color(0xFF9EBDF7),
    onSecondary = MidnightNavy,
    secondaryContainer = ElevatedGlass,
    onSecondaryContainer = IceWhite,
    tertiary = MintSuccess,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = IceWhite,
    surface = DeepCharcoal,
    onSurface = IceWhite,
    surfaceVariant = NavySurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = MidnightNavy,
    surfaceContainerLow = DeepCharcoal,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = ElevatedGlass,
    outline = SoftBorder,
    outlineVariant = CrispBorder,
    error = SoftCoral,
    onError = MidnightNavy,
  )

private val DepthClockLightColorScheme =
  lightColorScheme(
    primary = StudioAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = StudioInk,
    secondary = Color(0xFF445A84),
    onSecondary = Color.White,
    secondaryContainer = StudioSurfaceVariant,
    onSecondaryContainer = StudioInk,
    tertiary = Color(0xFF0D8E5E),
    onTertiary = Color.White,
    background = StudioPaper,
    onBackground = StudioInk,
    surface = StudioCard,
    onSurface = StudioInk,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = StudioPaper,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioSurfaceVariant,
    outline = StudioBorder,
    outlineVariant = Color(0x4414254D),
    error = Color(0xFFC83745),
    onError = Color.White,
  )

val DepthClockShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
  )

@Composable
fun DepthClockTheme(
  themeMode: AppThemeMode = AppThemeMode.DARK,
  content: @Composable () -> Unit,
) {
  val useDark =
    when (themeMode) {
      AppThemeMode.DARK -> true
      AppThemeMode.LIGHT -> false
      AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

  val colorScheme = if (useDark) DepthClockDarkColorScheme else DepthClockLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = DepthClockTypography,
    shapes = DepthClockShapes,
    content = content,
  )
}

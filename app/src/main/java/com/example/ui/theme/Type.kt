package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))
val OutfitFontFamily = FontFamily(Font(R.font.outfit, FontWeight.Normal))
val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val NunitoFontFamily = FontFamily(Font(R.font.nunito, FontWeight.Normal))
val PlayfairDisplayFontFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val PoppinsFontFamily = FontFamily(Font(R.font.poppins, FontWeight.Normal))
val PlusJakartaSansFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

val DepthClockTypography = Typography(
  displayLarge =
    TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 36.sp,
      lineHeight = 42.sp,
      letterSpacing = (-0.8).sp,
    ),
  headlineMedium =
    TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 24.sp,
      lineHeight = 30.sp,
      letterSpacing = (-0.4).sp,
    ),
  titleLarge =
    TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 19.sp,
      lineHeight = 25.sp,
      letterSpacing = (-0.2).sp,
    ),
  titleMedium =
    TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.sp,
    ),
  bodyLarge =
    TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.1.sp,
    ),
  bodyMedium =
    TextStyle(
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.15.sp,
    ),
  labelLarge =
    TextStyle(
      fontFamily = PlusJakartaSansFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.2.sp,
    ),
  labelSmall =
    TextStyle(
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      letterSpacing = 0.4.sp,
    ),
)

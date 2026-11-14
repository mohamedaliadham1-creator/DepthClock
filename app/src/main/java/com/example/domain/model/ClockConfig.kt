package com.example.domain.model

import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontFamily
import com.example.R
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.PoppinsFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.serialization.Serializable

@Serializable
enum class ClockFontStyle(
  val displayName: String,
  @FontRes val fontRes: Int,
  val defaultWeightBold: Boolean = false,
) {
  MODERN_IOS("Modern iOS", R.font.inter, true),
  THIN_MINIMALIST("Thin Minimal", R.font.outfit, false),
  BOLD_MODERN("Bold Modern", R.font.space_grotesk, true),
  ROUNDED("Soft Rounded", R.font.nunito, true),
  ELEGANT_SERIF("Editorial Serif", R.font.playfair_display, false),
  CLEAN_GEOMETRIC("Geometric", R.font.poppins, true),
  STUDIO_DISPLAY("Studio Sans", R.font.plus_jakarta_sans, true),
  TECH_MONO("Precision Mono", R.font.jetbrains_mono, false);

  fun composeFontFamily(): FontFamily =
    when (this) {
      MODERN_IOS -> InterFontFamily
      THIN_MINIMALIST -> OutfitFontFamily
      BOLD_MODERN -> SpaceGroteskFontFamily
      ROUNDED -> NunitoFontFamily
      ELEGANT_SERIF -> PlayfairDisplayFontFamily
      CLEAN_GEOMETRIC -> PoppinsFontFamily
      STUDIO_DISPLAY -> PlusJakartaSansFontFamily
      TECH_MONO -> JetBrainsMonoFontFamily
    }
}

@Serializable
enum class ClockTimeFormat(val label: String) {
  FORMAT_24H("24-Hour"),
  FORMAT_12H("12-Hour"),
}

@Serializable
enum class ClockHorizontalAlign(val label: String) {
  START("Left"),
  CENTER("Center"),
  END("Right"),
}

@Serializable
enum class DatePosition(val label: String) {
  ABOVE_CLOCK("Above Clock"),
  BELOW_CLOCK("Below Clock"),
  TOP_STATUS("Top Editorial"),
}

@Serializable
enum class DateFormatPreset(val label: String, val pattern: String) {
  DAY_MONTH_SHORT("Sat, Nov 14", "EEE, MMM d"),
  FULL_DAY_MONTH("Saturday, November 14", "EEEE, MMMM d"),
  COMPACT_NUMERIC("14 . 11 . 2026", "dd . MM . yyyy"),
  MONTH_DAY("November 14", "MMMM d"),
}

@Serializable
enum class DecorativeWidgetStyle(val label: String) {
  NONE("None"),
  MINIMAL_PILL("Glass Pill"),
  HAIRLINE_RULE("Editorial Rule"),
  LOCK_BADGE("Minimal Lock"),
}

@Serializable
data class ClockConfig(
  val timeFormat: ClockTimeFormat = ClockTimeFormat.FORMAT_24H,
  val showAmPm: Boolean = false,
  val showSecondsPreview: Boolean = false,
  val useLiveClock: Boolean = false,
  val customHour: Int = 14,
  val customMinute: Int = 41,
  val customSecond: Int = 0,
  val fontStyle: ClockFontStyle = ClockFontStyle.MODERN_IOS,
  val fontSizeRatio: Float = 0.27f,
  val widthScale: Float = 1.0f,
  val letterSpacingEm: Float = -0.02f,
  val colorArgb: Int = 0xFFEBF3FF.toInt(),
  val opacity: Float = 0.94f,
  val shadowEnabled: Boolean = true,
  val shadowOpacity: Float = 0.25f,
  val posXFraction: Float = 0.50f,
  val posYFraction: Float = 0.27f,
  val horizontalAlign: ClockHorizontalAlign = ClockHorizontalAlign.CENTER,
  val showDate: Boolean = true,
  val dateFormat: DateFormatPreset = DateFormatPreset.DAY_MONTH_SHORT,
  val dateFontStyle: ClockFontStyle = ClockFontStyle.THIN_MINIMALIST,
  val dateColorArgb: Int = 0xFFF2F6FF.toInt(),
  val datePosition: DatePosition = DatePosition.ABOVE_CLOCK,
  val decorativeWidget: DecorativeWidgetStyle = DecorativeWidgetStyle.NONE,
)

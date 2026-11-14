package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MaskToolMode {
  BRUSH,
  ERASER,
}

@Serializable
data class MaskConfig(
  val featherRadiusPx: Float = 4f,
  val edgeRefinement: Float = 0.15f,
  val subjectVisibility: Boolean = true,
  val showMaskOverlay: Boolean = false,
  val activeSubjectIndex: Int = -1, // -1 means all detected foreground subjects combined
)

@Serializable
enum class ExportMode(val title: String, val subtitle: String) {
  DEPTH_CLOCK_WALLPAPER(
    "Depth Clock Wallpaper",
    "Bakes the styled digital clock behind the foreground subject. Static time only.",
  ),
  CLEAN_DEPTH_WALLPAPER(
    "Clean Depth Wallpaper",
    "Exports the framed high-resolution photo without baking in a static clock.",
  ),
}

@Serializable
enum class ExportResolutionPreset(
  val label: String,
  val width: Int,
  val height: Int,
) {
  REDMI_NOTE_11("Redmi Note 11 (1080 × 2400)", 1080, 2400),
  PORTRAIT_FHD_16_9("Classic Portrait (1080 × 1920)", 1080, 1920),
  PORTRAIT_QHD("Tall QHD+ (1440 × 3200)", 1440, 3200),
  ORIGINAL("Original Image Size", 0, 0),
  CUSTOM("Custom Dimensions", 1080, 2400),
}

@Serializable
enum class ExportImageFormat(val label: String, val extension: String, val mimeType: String) {
  JPEG("JPEG", "jpg", "image/jpeg"),
  PNG("PNG (Lossless)", "png", "image/png"),
}

@Serializable
data class ExportConfig(
  val mode: ExportMode = ExportMode.DEPTH_CLOCK_WALLPAPER,
  val preset: ExportResolutionPreset = ExportResolutionPreset.REDMI_NOTE_11,
  val customWidth: Int = 1080,
  val customHeight: Int = 2400,
  val format: ExportImageFormat = ExportImageFormat.JPEG,
  val jpegQuality: Int = 95,
)

@Serializable
enum class AppThemeMode(val label: String) {
  DARK("Midnight Navy"),
  LIGHT("Studio Light"),
  SYSTEM("System"),
}

@Serializable
data class AppSettings(
  val defaultPreset: ExportResolutionPreset = ExportResolutionPreset.REDMI_NOTE_11,
  val defaultFormat: ExportImageFormat = ExportImageFormat.JPEG,
  val defaultJpegQuality: Int = 95,
  val defaultClockStyle: ClockFontStyle = ClockFontStyle.MODERN_IOS,
  val defaultClockFormat: ClockTimeFormat = ClockTimeFormat.FORMAT_24H,
  val themeMode: AppThemeMode = AppThemeMode.DARK,
)

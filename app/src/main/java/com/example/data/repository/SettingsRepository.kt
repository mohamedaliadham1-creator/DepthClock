package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.ExportImageFormat
import com.example.domain.model.ExportResolutionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "depthclock_settings")

class SettingsRepository(private val context: Context) {

  private object Keys {
    val DEFAULT_PRESET = stringPreferencesKey("default_preset")
    val DEFAULT_FORMAT = stringPreferencesKey("default_format")
    val DEFAULT_JPEG_QUALITY = intPreferencesKey("default_jpeg_quality")
    val DEFAULT_CLOCK_STYLE = stringPreferencesKey("default_clock_style")
    val DEFAULT_CLOCK_FORMAT = stringPreferencesKey("default_clock_format")
    val THEME_MODE = stringPreferencesKey("theme_mode")
  }

  val settingsFlow: Flow<AppSettings> =
    context.settingsDataStore.data.map { prefs ->
      AppSettings(
        defaultPreset =
          prefs[Keys.DEFAULT_PRESET]?.let { runCatching { ExportResolutionPreset.valueOf(it) }.getOrNull() }
            ?: ExportResolutionPreset.REDMI_NOTE_11,
        defaultFormat =
          prefs[Keys.DEFAULT_FORMAT]?.let { runCatching { ExportImageFormat.valueOf(it) }.getOrNull() }
            ?: ExportImageFormat.JPEG,
        defaultJpegQuality = (prefs[Keys.DEFAULT_JPEG_QUALITY] ?: 95).coerceIn(60, 100),
        defaultClockStyle =
          prefs[Keys.DEFAULT_CLOCK_STYLE]?.let { runCatching { ClockFontStyle.valueOf(it) }.getOrNull() }
            ?: ClockFontStyle.MODERN_IOS,
        defaultClockFormat =
          prefs[Keys.DEFAULT_CLOCK_FORMAT]?.let { runCatching { ClockTimeFormat.valueOf(it) }.getOrNull() }
            ?: ClockTimeFormat.FORMAT_24H,
        themeMode =
          prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.DARK,
      )
    }

  suspend fun updateDefaultPreset(preset: ExportResolutionPreset) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_PRESET] = preset.name }
  }

  suspend fun updateDefaultFormat(format: ExportImageFormat) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_FORMAT] = format.name }
  }

  suspend fun updateDefaultJpegQuality(quality: Int) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_JPEG_QUALITY] = quality.coerceIn(60, 100) }
  }

  suspend fun updateDefaultClockStyle(style: ClockFontStyle) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_CLOCK_STYLE] = style.name }
  }

  suspend fun updateDefaultClockFormat(format: ClockTimeFormat) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_CLOCK_FORMAT] = format.name }
  }

  suspend fun updateThemeMode(mode: AppThemeMode) {
    context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }
}

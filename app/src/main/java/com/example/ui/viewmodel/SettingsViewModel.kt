package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DepthClockApplication
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.ExportImageFormat
import com.example.domain.model.ExportResolutionPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
  application: Application,
  private val settingsRepository: SettingsRepository,
  private val creationsRepository: CreationsRepository,
) : AndroidViewModel(application) {

  val settings: StateFlow<AppSettings> =
    settingsRepository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppSettings(),
    )

  private val _cacheBytes = MutableStateFlow(0L)
  val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

  private val _message = MutableStateFlow<String?>(null)
  val message: StateFlow<String?> = _message.asStateFlow()

  init {
    refreshCacheSize()
  }

  fun refreshCacheSize() {
    viewModelScope.launch { _cacheBytes.value = creationsRepository.getTemporaryCacheBytes() }
  }

  fun updateDefaultPreset(preset: ExportResolutionPreset) {
    viewModelScope.launch { settingsRepository.updateDefaultPreset(preset) }
  }

  fun updateDefaultFormat(format: ExportImageFormat) {
    viewModelScope.launch { settingsRepository.updateDefaultFormat(format) }
  }

  fun updateDefaultQuality(quality: Int) {
    viewModelScope.launch { settingsRepository.updateDefaultJpegQuality(quality) }
  }

  fun updateDefaultClockStyle(style: ClockFontStyle) {
    viewModelScope.launch { settingsRepository.updateDefaultClockStyle(style) }
  }

  fun updateDefaultClockFormat(format: ClockTimeFormat) {
    viewModelScope.launch { settingsRepository.updateDefaultClockFormat(format) }
  }

  fun updateThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
  }

  fun clearCache() {
    viewModelScope.launch {
      val freed = creationsRepository.clearTemporaryCache()
      _cacheBytes.value = creationsRepository.getTemporaryCacheBytes()
      val mb = String.format(java.util.Locale.US, "%.2f", freed / (1024f * 1024f))
      _message.value = "Cleared $mb MB of temporary cache."
    }
  }

  fun clearMessage() {
    _message.value = null
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepthClockApplication
        SettingsViewModel(
          application = app,
          settingsRepository = app.container.settingsRepository,
          creationsRepository = app.container.creationsRepository,
        )
      }
    }
  }
}

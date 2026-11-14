package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DepthClockApplication
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.CreationProject
import com.example.domain.model.ExportConfig
import com.example.imaging.WallpaperApplyTarget
import com.example.imaging.WallpaperExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreationsViewModel(
  application: Application,
  private val creationsRepository: CreationsRepository,
  private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

  val creations: StateFlow<List<CreationProject>> =
    creationsRepository.creationsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _busyProjectId = MutableStateFlow<String?>(null)
  val busyProjectId: StateFlow<String?> = _busyProjectId.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      creationsRepository.deleteProject(projectId)
      _statusMessage.value = "Creation deleted."
    }
  }

  fun exportCopy(project: CreationProject) {
    if (_busyProjectId.value != null) return
    viewModelScope.launch {
      _busyProjectId.value = project.id
      val appSettings = settingsRepository.settingsFlow.first()
      val config =
        ExportConfig(
          preset = appSettings.defaultPreset,
          format = appSettings.defaultFormat,
          jpegQuality = appSettings.defaultJpegQuality,
        )
      val result = WallpaperExporter.exportToPictures(getApplication(), project, config)
      _busyProjectId.value = null
      result.fold(
        onSuccess = { outcome ->
          creationsRepository.updateExportMetadata(
            project.id,
            outcome.exportedFilePath,
            outcome.mediaStoreUri?.toString(),
          )
          _statusMessage.value = "Exported ${outcome.width}×${outcome.height} to Pictures/DepthClock."
        },
        onFailure = { err ->
          _statusMessage.value = err.localizedMessage ?: "Export failed."
        },
      )
    }
  }

  fun shareProject(project: CreationProject, onLaunchIntent: (Intent) -> Unit) {
    if (_busyProjectId.value != null) return
    viewModelScope.launch {
      _busyProjectId.value = project.id
      val appSettings = settingsRepository.settingsFlow.first()
      val config =
        ExportConfig(
          preset = appSettings.defaultPreset,
          format = appSettings.defaultFormat,
          jpegQuality = appSettings.defaultJpegQuality,
        )
      val result = WallpaperExporter.exportToPictures(getApplication(), project, config)
      _busyProjectId.value = null
      result.fold(
        onSuccess = { outcome ->
          onLaunchIntent(
            WallpaperExporter.buildShareIntent(
              getApplication(),
              outcome.shareUri,
              outcome.format.mimeType,
            ),
          )
        },
        onFailure = { err ->
          _statusMessage.value = err.localizedMessage ?: "Could not prepare share image."
        },
      )
    }
  }

  fun applyWallpaper(project: CreationProject, target: WallpaperApplyTarget) {
    if (_busyProjectId.value != null) return
    viewModelScope.launch {
      _busyProjectId.value = project.id
      val appSettings = settingsRepository.settingsFlow.first()
      val config =
        ExportConfig(
          preset = appSettings.defaultPreset,
          format = appSettings.defaultFormat,
          jpegQuality = appSettings.defaultJpegQuality,
        )
      val bmpRes = WallpaperExporter.renderExportBitmap(getApplication(), project, config)
      bmpRes.fold(
        onSuccess = { bmp ->
          try {
            val applyRes = WallpaperExporter.applyWallpaper(getApplication(), bmp, target)
            _statusMessage.value =
              applyRes.fold(
                onSuccess = { it },
                onFailure = { it.localizedMessage ?: "Wallpaper application restricted." },
              )
          } finally {
            bmp.recycle()
          }
        },
        onFailure = { err ->
          _statusMessage.value = err.localizedMessage ?: "Failed to render wallpaper."
        },
      )
      _busyProjectId.value = null
    }
  }

  fun clearMessage() {
    _statusMessage.value = null
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepthClockApplication
        CreationsViewModel(
          application = app,
          creationsRepository = app.container.creationsRepository,
          settingsRepository = app.container.settingsRepository,
        )
      }
    }
  }
}

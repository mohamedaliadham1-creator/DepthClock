package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DepthClockApplication
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ImportedPhotoPreview
import com.example.imaging.ImageLoader
import com.example.imaging.SampleSceneGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ImportUiState {
  data object Idle : ImportUiState
  data object Inspecting : ImportUiState
  data class PreviewReady(val preview: ImportedPhotoPreview, val projectTitle: String) : ImportUiState
  data object Importing : ImportUiState
  data class Error(val message: String) : ImportUiState
}

class ImportViewModel(
  application: Application,
  private val creationsRepository: CreationsRepository,
  private val settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
  val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

  fun onPhotoSelected(uri: Uri) {
    viewModelScope.launch {
      _uiState.value = ImportUiState.Inspecting
      val result = ImageLoader.inspectUri(getApplication(), uri)
      _uiState.value =
        result.fold(
          onSuccess = { preview ->
            val defaultTitle =
              preview.displayName.substringBeforeLast('.').take(28).ifBlank { "New Depth Wallpaper" }
            ImportUiState.PreviewReady(preview = preview, projectTitle = defaultTitle)
          },
          onFailure = { err ->
            ImportUiState.Error(
              err.localizedMessage
                ?: "Unable to open this photograph. Try selecting a JPEG, PNG, or WebP image.",
            )
          },
        )
    }
  }

  fun updateTitle(title: String) {
    val cur = _uiState.value
    if (cur is ImportUiState.PreviewReady) {
      _uiState.value = cur.copy(projectTitle = title)
    }
  }

  fun importSelectedPhoto(onImported: (String) -> Unit) {
    val cur = _uiState.value as? ImportUiState.PreviewReady ?: return
    viewModelScope.launch {
      _uiState.value = ImportUiState.Importing
      val settings = settingsRepository.settingsFlow.first()
      val result =
        creationsRepository.createProjectFromUri(
          uri = Uri.parse(cur.preview.uriString),
          title = cur.projectTitle,
          defaultClockStyle = settings.defaultClockStyle,
          defaultClockFormat = settings.defaultClockFormat,
        )
      result.fold(
        onSuccess = { project -> onImported(project.id) },
        onFailure = { err ->
          _uiState.value =
            ImportUiState.Error(
              err.localizedMessage ?: "Failed to import photograph. Memory or file format error.",
            )
        },
      )
    }
  }

  fun importSampleScene(
    kind: SampleSceneGenerator.SampleKind,
    onImported: (String) -> Unit,
  ) {
    viewModelScope.launch {
      _uiState.value = ImportUiState.Importing
      val settings = settingsRepository.settingsFlow.first()
      val result =
        creationsRepository.createProjectFromSample(
          kind = kind,
          defaultClockStyle = settings.defaultClockStyle,
          defaultClockFormat = settings.defaultClockFormat,
        )
      result.fold(
        onSuccess = { project -> onImported(project.id) },
        onFailure = { err ->
          _uiState.value = ImportUiState.Error(err.localizedMessage ?: "Failed to create sample scene.")
        },
      )
    }
  }

  fun resetError() {
    _uiState.value = ImportUiState.Idle
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepthClockApplication
        ImportViewModel(
          application = app,
          creationsRepository = app.container.creationsRepository,
          settingsRepository = app.container.settingsRepository,
        )
      }
    }
  }
}

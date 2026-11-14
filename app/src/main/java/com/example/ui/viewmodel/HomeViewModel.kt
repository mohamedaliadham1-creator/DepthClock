package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DepthClockApplication
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.CreationProject
import com.example.imaging.ImageCompositor
import com.example.imaging.SampleSceneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
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

  val settings: StateFlow<AppSettings> =
    settingsRepository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = AppSettings(),
    )

  private val _featuredPreview = MutableStateFlow<Bitmap?>(null)
  val featuredPreview: StateFlow<Bitmap?> = _featuredPreview.asStateFlow()

  private val _featuredBeforePreview = MutableStateFlow<Bitmap?>(null)
  val featuredBeforePreview: StateFlow<Bitmap?> = _featuredBeforePreview.asStateFlow()

  private val _isOpeningSample = MutableStateFlow(false)
  val isOpeningSample: StateFlow<Boolean> = _isOpeningSample.asStateFlow()

  init {
    renderFeaturedHeroPreview()
  }

  private fun renderFeaturedHeroPreview() {
    viewModelScope.launch {
      val (afterBmp, beforeBmp) =
        withContext(Dispatchers.Default) {
          val ctx = getApplication<Application>()
          val src = SampleSceneGenerator.generateSamplePhoto(SampleSceneGenerator.SampleKind.BELL_TOWER, 540, 1200)
          val mask = SampleSceneGenerator.generateReferenceMask(SampleSceneGenerator.SampleKind.BELL_TOWER, 540, 1200)
          val clock =
            ClockConfig(
              fontStyle = ClockFontStyle.MODERN_IOS,
              customHour = 14,
              customMinute = 41,
              fontSizeRatio = 0.28f,
              posYFraction = 0.28f,
              colorArgb = 0xFFE8F2FF.toInt(),
              opacity = 0.94f,
            )
          val composed =
            ImageCompositor.composite(
              context = ctx,
              sourceBitmap = src,
              maskBitmap = mask,
              clockConfig = clock,
              params =
                ImageCompositor.CompositeParams(
                  targetWidth = 540,
                  targetHeight = 1200,
                  includeClock = true,
                  includeForegroundSubject = true,
                ),
            )
          val flat =
            ImageCompositor.composite(
              context = ctx,
              sourceBitmap = src,
              maskBitmap = null,
              clockConfig = clock,
              params =
                ImageCompositor.CompositeParams(
                  targetWidth = 540,
                  targetHeight = 1200,
                  includeClock = true,
                  includeForegroundSubject = false,
                ),
            )
          src.recycle()
          mask.recycle()
          composed to flat
        }
      _featuredPreview.value = afterBmp
      _featuredBeforePreview.value = beforeBmp
    }
  }

  fun launchSampleProject(
    kind: SampleSceneGenerator.SampleKind = SampleSceneGenerator.SampleKind.BELL_TOWER,
    onCreated: (String) -> Unit,
  ) {
    if (_isOpeningSample.value) return
    viewModelScope.launch {
      _isOpeningSample.value = true
      val currentSettings = settingsRepository.settingsFlow.first()
      val result =
        creationsRepository.createProjectFromSample(
          kind = kind,
          defaultClockStyle = currentSettings.defaultClockStyle,
          defaultClockFormat = currentSettings.defaultClockFormat,
        )
      _isOpeningSample.value = false
      result.onSuccess { project -> onCreated(project.id) }
    }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepthClockApplication
        HomeViewModel(
          application = app,
          creationsRepository = app.container.creationsRepository,
          settingsRepository = app.container.settingsRepository,
        )
      }
    }
  }
}

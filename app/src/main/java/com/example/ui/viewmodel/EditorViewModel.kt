package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.DepthClockApplication
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ClockConfig
import com.example.domain.model.CreationProject
import com.example.domain.model.ExportConfig
import com.example.domain.model.MaskConfig
import com.example.domain.model.MaskToolMode
import com.example.imaging.DetectedSubjectMask
import com.example.imaging.ImageCompositor
import com.example.imaging.ImageLoader
import com.example.imaging.MaskHistoryStack
import com.example.imaging.MaskProcessor
import com.example.imaging.SegmentationEngine
import com.example.imaging.SegmentationManager
import com.example.imaging.SegmentationProgress
import com.example.imaging.WallpaperApplyTarget
import com.example.imaging.WallpaperExporter
import com.example.imaging.ExportOutcome
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class EditorTab(val label: String) {
  SUBJECT("Subject"),
  CLOCK("Clock"),
  DATE("Date"),
  EXPORT("Export"),
}

data class EditorUiState(
  val isLoading: Boolean = true,
  val loadError: String? = null,
  val project: CreationProject? = null,
  val activeTab: EditorTab = EditorTab.CLOCK,
  val previewBitmap: Bitmap? = null,
  val previewAspectRatio: Float = 9f / 20f,
  val isBeforeAfterPeek: Boolean = false,
  val toolMode: MaskToolMode = MaskToolMode.BRUSH,
  val brushSizeNorm: Float = 0.055f,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val isSegmenting: Boolean = false,
  val segmentationProgressLabel: String? = null,
  val segmentationDownloadPercent: Int? = null,
  val segmentationEngine: SegmentationEngine? = null,
  val segmentationStatus: String? = null,
  val detectedSubjects: List<DetectedSubjectMask> = emptyList(),
  val snapXActive: Boolean = false,
  val snapYActive: Boolean = false,
  val isDraggingClock: Boolean = false,
  val exportConfig: ExportConfig = ExportConfig(),
  val isExporting: Boolean = false,
  val exportPreviewBitmap: Bitmap? = null,
  val lastExportOutcome: ExportOutcome? = null,
  val snackbarMessage: String? = null,
  val isSaving: Boolean = false,
)

class EditorViewModel(
  application: Application,
  private val projectId: String,
  private val creationsRepository: CreationsRepository,
  private val settingsRepository: SettingsRepository,
  private val segmentationManager: SegmentationManager,
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(EditorUiState())
  val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

  private var workingSourceBitmap: Bitmap? = null
  private var workingAlphaBytes: ByteArray? = null
  private var autoSegmentBackupBytes: ByteArray? = null
  private var maskWidth: Int = 360
  private var maskHeight: Int = 800

  private val history = MaskHistoryStack(maxSteps = 18)
  private val renderMutex = Mutex()
  private var renderJob: Job? = null
  private var saveJob: Job? = null
  private var segmentationJob: Job? = null

  private var lastStrokeNormX: Float = 0f
  private var lastStrokeNormY: Float = 0f
  private var isStrokeActive: Boolean = false

  init {
    loadProject()
    startLiveClockTicker()
  }

  private fun startLiveClockTicker() {
    viewModelScope.launch {
      while (true) {
        delay(1000L)
        val proj = _uiState.value.project
        if (proj != null && (proj.clockConfig.useLiveClock || proj.clockConfig.showSecondsPreview)) {
          scheduleCompositeRender()
        }
      }
    }
  }

  private fun loadProject() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, loadError = null) }
      val project = creationsRepository.getProject(projectId)
      if (project == null) {
        _uiState.update { it.copy(isLoading = false, loadError = "Saved project could not be found.") }
        return@launch
      }
      if (project.isSourceMissing) {
        _uiState.update {
          it.copy(
            isLoading = false,
            project = project,
            loadError = "Original photograph file is missing from device storage.",
          )
        }
        return@launch
      }

      val defaultSettings = settingsRepository.settingsFlow.first()
      val initialExport =
        ExportConfig(
          preset = defaultSettings.defaultPreset,
          format = defaultSettings.defaultFormat,
          jpegQuality = defaultSettings.defaultJpegQuality,
        )

      val srcBmp = ImageLoader.decodeFileBounded(project.sourceImagePath, 1080)
      if (srcBmp == null) {
        _uiState.update {
          it.copy(
            isLoading = false,
            loadError = "Failed to decode project image. The file may be corrupted.",
          )
        }
        return@launch
      }
      workingSourceBitmap = srcBmp

      val maxMaskEdge = 720f
      val maskScale = minOf(1f, maxMaskEdge / max(srcBmp.width, srcBmp.height))
      maskWidth = max(64, (srcBmp.width * maskScale).roundToInt())
      maskHeight = max(64, (srcBmp.height * maskScale).roundToInt())

      val existingMaskFile = File(project.maskFilePath)
      val loadedAlpha: ByteArray? =
        withContext(Dispatchers.IO) {
          if (existingMaskFile.exists()) {
            val decodedMask = BitmapFactory.decodeFile(existingMaskFile.absolutePath)
            if (decodedMask != null) {
              val raw = MaskProcessor.bitmapToAlphaBytes(decodedMask)
              val dw = decodedMask.width
              val dh = decodedMask.height
              decodedMask.recycle()
              MaskProcessor.scaleAlphaBuffer(raw, dw, dh, maskWidth, maskHeight)
            } else {
              null
            }
          } else {
            null
          }
        }

      if (loadedAlpha != null) {
        workingAlphaBytes = loadedAlpha
        autoSegmentBackupBytes = loadedAlpha.copyOf()
        _uiState.update {
          it.copy(
            isLoading = false,
            project = project,
            exportConfig = initialExport,
            segmentationStatus = "Loaded saved foreground mask.",
          )
        }
        scheduleCompositeRender()
      } else {
        workingAlphaBytes = ByteArray(maskWidth * maskHeight)
        _uiState.update {
          it.copy(
            isLoading = false,
            project = project,
            exportConfig = initialExport,
          )
        }
        scheduleCompositeRender()
        runAutoSegmentation()
      }
    }
  }

  fun selectTab(tab: EditorTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun toggleBeforeAfterPeek(active: Boolean) {
    _uiState.update { it.copy(isBeforeAfterPeek = active) }
    scheduleCompositeRender()
  }

  fun runAutoSegmentation() {
    val src = workingSourceBitmap ?: return
    segmentationJob?.cancel()
    segmentationJob =
      viewModelScope.launch {
        _uiState.update {
          it.copy(
            isSegmenting = true,
            segmentationProgressLabel = "Preparing on-device subject detector…",
            segmentationDownloadPercent = null,
          )
        }
        val outcome =
          segmentationManager.segmentImage(
            sourceBitmap = src,
            targetMaskWidth = maskWidth,
            targetMaskHeight = maskHeight,
          ) { progress ->
            val (label, pct) =
              when (progress) {
                is SegmentationProgress.CheckingModel ->
                  "Checking on-device segmentation model…" to null
                is SegmentationProgress.DownloadingModel ->
                  "Downloading on-device ML Kit model via Google Play Services…" to
                    progress.progressPercent
                is SegmentationProgress.RunningSegmentation ->
                  "Detecting foreground subjects…" to null
              }
            _uiState.update {
              it.copy(
                segmentationProgressLabel = label,
                segmentationDownloadPercent = pct,
              )
            }
          }

        workingAlphaBytes?.let { history.pushUndo(it) }
        workingAlphaBytes = outcome.combinedAlphaMask.copyOf()
        autoSegmentBackupBytes = outcome.combinedAlphaMask.copyOf()

        updateMaskConfig { it.copy(activeSubjectIndex = -1, subjectVisibility = true) }
        _uiState.update {
          it.copy(
            isSegmenting = false,
            segmentationProgressLabel = null,
            segmentationDownloadPercent = null,
            segmentationEngine = outcome.engine,
            segmentationStatus = outcome.statusMessage,
            detectedSubjects = outcome.subjects,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
          )
        }
        scheduleCompositeRender()
        scheduleAutoSave()
      }
  }

  fun cancelAutoSegmentation() {
    segmentationJob?.cancel()
    _uiState.update {
      it.copy(
        isSegmenting = false,
        segmentationProgressLabel = null,
        segmentationDownloadPercent = null,
        segmentationStatus = "Automatic segmentation canceled. Use Brush to paint foreground manually.",
      )
    }
  }

  fun selectDetectedSubject(subjectIndex: Int) {
    val subjects = _uiState.value.detectedSubjects
    val currentAlpha = workingAlphaBytes ?: return
    history.pushUndo(currentAlpha)

    if (subjectIndex < 0) {
      workingAlphaBytes = autoSegmentBackupBytes?.copyOf() ?: currentAlpha
    } else {
      val match = subjects.firstOrNull { it.index == subjectIndex }
      if (match != null) {
        workingAlphaBytes = match.alphaMask.copyOf()
      }
    }
    updateMaskConfig { it.copy(activeSubjectIndex = subjectIndex) }
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun setMaskToolMode(mode: MaskToolMode) {
    _uiState.update { it.copy(toolMode = mode) }
  }

  fun setBrushSize(norm: Float) {
    _uiState.update { it.copy(brushSizeNorm = norm.coerceIn(0.015f, 0.18f)) }
  }

  fun beginMaskStroke(canvasNormX: Float, canvasNormY: Float) {
    val alpha = workingAlphaBytes ?: return
    history.pushUndo(alpha)
    val (mx, my) = canvasNormToMaskNorm(canvasNormX, canvasNormY)
    lastStrokeNormX = mx
    lastStrokeNormY = my
    isStrokeActive = true
    applyMaskSegment(mx, my, mx, my)
  }

  fun moveMaskStroke(canvasNormX: Float, canvasNormY: Float) {
    if (!isStrokeActive) return
    val (mx, my) = canvasNormToMaskNorm(canvasNormX, canvasNormY)
    applyMaskSegment(lastStrokeNormX, lastStrokeNormY, mx, my)
    lastStrokeNormX = mx
    lastStrokeNormY = my
  }

  fun endMaskStroke() {
    if (!isStrokeActive) return
    isStrokeActive = false
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleAutoSave()
  }

  private fun applyMaskSegment(fromX: Float, fromY: Float, toX: Float, toY: Float) {
    val alpha = workingAlphaBytes ?: return
    val state = _uiState.value
    MaskProcessor.applyStroke(
      buffer = alpha,
      width = maskWidth,
      height = maskHeight,
      fromNormX = fromX,
      fromNormY = fromY,
      toNormX = toX,
      toNormY = toY,
      radiusNorm = state.brushSizeNorm,
      isEraser = state.toolMode == MaskToolMode.ERASER,
      hardness = 0.72f,
    )
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleCompositeRender()
  }

  private fun canvasNormToMaskNorm(canvasNormX: Float, canvasNormY: Float): Pair<Float, Float> {
    val proj = _uiState.value.project ?: return canvasNormX to canvasNormY
    val previewW = 540f
    val previewH = (540f / _uiState.value.previewAspectRatio).coerceAtLeast(1f)
    val matrix =
      ImageCompositor.computeImageMatrix(
        srcWidth = maskWidth.toFloat(),
        srcHeight = maskHeight.toFloat(),
        dstWidth = previewW,
        dstHeight = previewH,
        cropScale = proj.cropScale,
        cropOffsetX = proj.cropOffsetX,
        cropOffsetY = proj.cropOffsetY,
      )
    val inverse = android.graphics.Matrix()
    if (!matrix.invert(inverse)) return canvasNormX to canvasNormY
    val pts = floatArrayOf(canvasNormX * previewW, canvasNormY * previewH)
    inverse.mapPoints(pts)
    return (pts[0] / maskWidth).coerceIn(0f, 1f) to (pts[1] / maskHeight).coerceIn(0f, 1f)
  }

  fun undoMask() {
    val current = workingAlphaBytes ?: return
    val prev = history.undo(current) ?: return
    workingAlphaBytes = prev
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun redoMask() {
    val current = workingAlphaBytes ?: return
    val next = history.redo(current) ?: return
    workingAlphaBytes = next
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun resetMaskToAuto() {
    val current = workingAlphaBytes ?: return
    val backup = autoSegmentBackupBytes
    if (backup != null) {
      history.pushUndo(current)
      workingAlphaBytes = backup.copyOf()
      _uiState.update {
        it.copy(
          canUndo = history.canUndo,
          canRedo = history.canRedo,
          snackbarMessage = "Mask reset to automatic detection.",
        )
      }
      scheduleCompositeRender()
      scheduleAutoSave()
    } else {
      runAutoSegmentation()
    }
  }

  fun clearMask() {
    val current = workingAlphaBytes ?: return
    history.pushUndo(current)
    workingAlphaBytes = ByteArray(maskWidth * maskHeight)
    _uiState.update {
      it.copy(
        canUndo = history.canUndo,
        canRedo = history.canRedo,
        snackbarMessage = "Mask cleared. Paint foreground with Brush.",
      )
    }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun invertMask() {
    val current = workingAlphaBytes ?: return
    history.pushUndo(current)
    workingAlphaBytes = MaskProcessor.invertMask(current)
    _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun updateMaskConfig(transform: (MaskConfig) -> MaskConfig) {
    val proj = _uiState.value.project ?: return
    val updated = proj.copy(maskConfig = transform(proj.maskConfig))
    _uiState.update { it.copy(project = updated) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun updateClockConfig(transform: (ClockConfig) -> ClockConfig) {
    val proj = _uiState.value.project ?: return
    val updated = proj.copy(clockConfig = transform(proj.clockConfig))
    _uiState.update { it.copy(project = updated) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun dragClockBy(deltaNormX: Float, deltaNormY: Float) {
    val proj = _uiState.value.project ?: return
    val rawX = (proj.clockConfig.posXFraction + deltaNormX).coerceIn(0.14f, 0.86f)
    val rawY = (proj.clockConfig.posYFraction + deltaNormY).coerceIn(0.12f, 0.84f)
    val snapX = abs(rawX - 0.50f) < 0.025f
    val snapY = abs(rawY - 0.50f) < 0.025f
    val finalX = if (snapX) 0.50f else rawX
    val finalY = if (snapY) 0.50f else rawY

    val updated =
      proj.copy(
        clockConfig = proj.clockConfig.copy(posXFraction = finalX, posYFraction = finalY),
      )
    _uiState.update {
      it.copy(
        project = updated,
        isDraggingClock = true,
        snapXActive = snapX,
        snapYActive = snapY,
      )
    }
    scheduleCompositeRender()
  }

  fun endClockDrag() {
    _uiState.update {
      it.copy(isDraggingClock = false, snapXActive = false, snapYActive = false)
    }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun updateCrop(scale: Float? = null, offsetX: Float? = null, offsetY: Float? = null) {
    val proj = _uiState.value.project ?: return
    val updated =
      proj.copy(
        cropScale = (scale ?: proj.cropScale).coerceIn(1.0f, 3.0f),
        cropOffsetX = (offsetX ?: proj.cropOffsetX).coerceIn(-1f, 1f),
        cropOffsetY = (offsetY ?: proj.cropOffsetY).coerceIn(-1f, 1f),
      )
    _uiState.update { it.copy(project = updated) }
    scheduleCompositeRender()
    scheduleAutoSave()
  }

  fun updateExportConfig(transform: (ExportConfig) -> ExportConfig) {
    val next = transform(_uiState.value.exportConfig)
    val aspect =
      when (next.preset) {
        com.example.domain.model.ExportResolutionPreset.REDMI_NOTE_11 -> 1080f / 2400f
        com.example.domain.model.ExportResolutionPreset.PORTRAIT_FHD_16_9 -> 1080f / 1920f
        com.example.domain.model.ExportResolutionPreset.PORTRAIT_QHD -> 1440f / 3200f
        com.example.domain.model.ExportResolutionPreset.ORIGINAL -> {
          val p = _uiState.value.project
          if (p != null && p.imageHeight > 0) (p.imageWidth.toFloat() / p.imageHeight).coerceIn(0.35f, 1.8f)
          else 9f / 20f
        }
        com.example.domain.model.ExportResolutionPreset.CUSTOM -> {
          val w = next.customWidth.coerceAtLeast(256)
          val h = next.customHeight.coerceAtLeast(256)
          (w.toFloat() / h).coerceIn(0.35f, 1.8f)
        }
      }
    _uiState.update { it.copy(exportConfig = next, previewAspectRatio = aspect) }
    scheduleCompositeRender()
  }

  fun saveProjectNow() {
    val proj = _uiState.value.project ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true) }
      persistProjectInternal(proj)
      _uiState.update {
        it.copy(isSaving = false, snackbarMessage = "Project saved to My Creations.")
      }
    }
  }

  fun exportToPictures() {
    val proj = _uiState.value.project ?: return
    if (_uiState.value.isExporting) return
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      persistProjectInternal(proj)
      val result =
        WallpaperExporter.exportToPictures(
          context = getApplication(),
          project = proj,
          exportConfig = _uiState.value.exportConfig,
          overrideMaskAlpha = workingAlphaBytes,
          overrideMaskSize = maskWidth to maskHeight,
        )
      _uiState.update { it.copy(isExporting = false) }
      result.fold(
        onSuccess = { outcome ->
          creationsRepository.updateExportMetadata(
            proj.id,
            outcome.exportedFilePath,
            outcome.mediaStoreUri?.toString(),
          )
          _uiState.update {
            it.copy(
              lastExportOutcome = outcome,
              snackbarMessage =
                "Saved ${outcome.width}×${outcome.height} ${outcome.format.label} to Pictures/DepthClock.",
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(snackbarMessage = err.localizedMessage ?: "Export failed.")
          }
        },
      )
    }
  }

  fun previewExportFullRes() {
    val proj = _uiState.value.project ?: return
    if (_uiState.value.isExporting) return
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      val result =
        WallpaperExporter.renderExportBitmap(
          context = getApplication(),
          project = proj,
          exportConfig = _uiState.value.exportConfig,
          overrideMaskAlpha = workingAlphaBytes,
          overrideMaskSize = maskWidth to maskHeight,
        )
      _uiState.update { it.copy(isExporting = false) }
      result.fold(
        onSuccess = { bmp -> _uiState.update { it.copy(exportPreviewBitmap = bmp) } },
        onFailure = { err ->
          _uiState.update {
            it.copy(snackbarMessage = err.localizedMessage ?: "Could not render export preview.")
          }
        },
      )
    }
  }

  fun dismissExportPreview() {
    _uiState.update { it.copy(exportPreviewBitmap = null) }
  }

  fun applyWallpaper(target: WallpaperApplyTarget) {
    val proj = _uiState.value.project ?: return
    if (_uiState.value.isExporting) return
    viewModelScope.launch {
      _uiState.update { it.copy(isExporting = true) }
      persistProjectInternal(proj)
      val bmpRes =
        WallpaperExporter.renderExportBitmap(
          context = getApplication(),
          project = proj,
          exportConfig = _uiState.value.exportConfig,
          overrideMaskAlpha = workingAlphaBytes,
          overrideMaskSize = maskWidth to maskHeight,
        )
      bmpRes.fold(
        onSuccess = { bmp ->
          try {
            val applyRes = WallpaperExporter.applyWallpaper(getApplication(), bmp, target)
            _uiState.update {
              it.copy(
                isExporting = false,
                snackbarMessage =
                  applyRes.fold(
                    onSuccess = { msg -> msg },
                    onFailure = { err ->
                      err.localizedMessage ?: "Could not set wallpaper directly."
                    },
                  ),
              )
            }
          } finally {
            bmp.recycle()
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isExporting = false,
              snackbarMessage = err.localizedMessage ?: "Failed to render wallpaper.",
            )
          }
        },
      )
    }
  }

  fun buildShareLastExportIntent(): Intent? {
    val outcome = _uiState.value.lastExportOutcome ?: return null
    return WallpaperExporter.buildShareIntent(
      getApplication(),
      outcome.shareUri,
      outcome.format.mimeType,
    )
  }

  fun buildOpenInGalleryIntent(): Intent? {
    val outcome = _uiState.value.lastExportOutcome ?: return null
    val targetUri = outcome.mediaStoreUri ?: outcome.shareUri
    return WallpaperExporter.buildOpenInGalleryIntent(targetUri, outcome.format.mimeType)
  }

  fun clearSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  private fun scheduleCompositeRender() {
    renderJob?.cancel()
    renderJob =
      viewModelScope.launch {
        renderMutex.withLock {
          val state = _uiState.value
          val proj = state.project ?: return@withLock
          val src = workingSourceBitmap ?: return@withLock
          val alpha = workingAlphaBytes ?: return@withLock

          val previewW = 540
          val previewH = (540f / state.previewAspectRatio).roundToInt().coerceIn(280, 1280)

          val rendered =
            withContext(Dispatchers.Default) {
              val maskBmp =
                MaskProcessor.alphaBytesToBitmap(
                  rawAlpha = alpha,
                  width = maskWidth,
                  height = maskHeight,
                  featherRadiusPx = proj.maskConfig.featherRadiusPx,
                  edgeRefinement = proj.maskConfig.edgeRefinement,
                )
              try {
                val includeClock =
                  state.exportConfig.mode == com.example.domain.model.ExportMode.DEPTH_CLOCK_WALLPAPER
                val includeFg =
                  !state.isBeforeAfterPeek &&
                    proj.maskConfig.subjectVisibility &&
                    includeClock
                ImageCompositor.composite(
                  context = getApplication(),
                  sourceBitmap = src,
                  maskBitmap = maskBmp,
                  clockConfig = proj.clockConfig,
                  params =
                    ImageCompositor.CompositeParams(
                      targetWidth = previewW,
                      targetHeight = previewH,
                      cropScale = proj.cropScale,
                      cropOffsetX = proj.cropOffsetX,
                      cropOffsetY = proj.cropOffsetY,
                      includeClock = includeClock,
                      includeForegroundSubject = includeFg,
                      showMaskOverlay = proj.maskConfig.showMaskOverlay,
                      showAlignmentGuides = state.isDraggingClock,
                      snapXActive = state.snapXActive,
                      snapYActive = state.snapYActive,
                    ),
                )
              } finally {
                maskBmp.recycle()
              }
            }

          _uiState.update { it.copy(previewBitmap = rendered) }
        }
      }
  }

  private fun scheduleAutoSave() {
    saveJob?.cancel()
    saveJob =
      viewModelScope.launch {
        delay(650L)
        _uiState.value.project?.let { persistProjectInternal(it) }
      }
  }

  private suspend fun persistProjectInternal(project: CreationProject) {
    val alpha = workingAlphaBytes
    val maskBmp =
      if (alpha != null) {
        withContext(Dispatchers.Default) {
          MaskProcessor.alphaBytesToBitmap(alpha, maskWidth, maskHeight)
        }
      } else {
        null
      }
    try {
      creationsRepository.saveProjectState(
        project = project,
        maskBitmap = maskBmp,
        previewCompositeBitmap = _uiState.value.previewBitmap,
      )
    } finally {
      maskBmp?.recycle()
    }
  }

  override fun onCleared() {
    super.onCleared()
    history.clear()
    workingSourceBitmap?.recycle()
    workingSourceBitmap = null
  }

  companion object {
    fun factory(projectId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DepthClockApplication
        EditorViewModel(
          application = app,
          projectId = projectId,
          creationsRepository = app.container.creationsRepository,
          settingsRepository = app.container.settingsRepository,
          segmentationManager = app.container.segmentationManager,
        )
      }
    }
  }
}

package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ClockStudioPanel
import com.example.ui.components.DateAndFramePanel
import com.example.ui.components.ExportAndApplyPanel
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.RealisticPhoneFrame
import com.example.ui.components.SubjectEditorPanel
import com.example.ui.viewmodel.EditorTab
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
  viewModel: EditorViewModel,
  onBack: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var maskZoom by remember { mutableFloatStateOf(1f) }
  var maskPan by remember { mutableStateOf(Offset.Zero) }

  LaunchedEffect(state.snackbarMessage) {
    val msg = state.snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val launchIntent: (Intent) -> Unit = { intent ->
    runCatching { context.startActivity(intent) }
      .onFailure {
        // Handled cleanly if no viewer app exists
      }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = state.project?.title ?: "Depth Studio",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = {
              viewModel.saveProjectNow()
              onBack()
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          // Hold/Tap Before & After Comparison
          IconButton(
            onClick = { viewModel.toggleBeforeAfterPeek(!state.isBeforeAfterPeek) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("before_after_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Compare,
              contentDescription = "Toggle before and after comparison",
              tint =
                if (state.isBeforeAfterPeek) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
          }

          // Toggle Mask Tint Overlay quickly
          IconButton(
            onClick = {
              val cur = state.project?.maskConfig?.showMaskOverlay ?: false
              viewModel.updateMaskConfig { it.copy(showMaskOverlay = !cur) }
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_mask_overlay_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Layers,
              contentDescription = "Toggle foreground mask overlay",
              tint =
                if (state.project?.maskConfig?.showMaskOverlay == true) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
            )
          }

          IconButton(
            onClick = { viewModel.saveProjectNow() },
            enabled = !state.isSaving,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("save_project_button"),
          ) {
            Icon(Icons.Outlined.Save, contentDescription = "Save project")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
      )
    },
  ) { innerPadding ->
    if (state.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          CircularProgressIndicator()
          Text("Loading studio workspace…", style = MaterialTheme.typography.bodyMedium)
        }
      }
      return@Scaffold
    }

    if (state.loadError != null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
        LiquidGlassPanel {
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Project Unavailable", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
            Text(state.loadError ?: "", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onBack) { Text("Return") }
          }
        }
      }
      return@Scaffold
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      val isWide = maxWidth >= 720.dp

      if (isWide) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            InteractivePreviewCanvas(
              state = state,
              viewModel = viewModel,
              maskZoom = maskZoom,
              maskPan = maskPan,
              onMaskZoomPanChange = { z, p ->
                maskZoom = z
                maskPan = p
              },
            )
          }

          StudioControlsDock(
            state = state,
            viewModel = viewModel,
            maskZoom = maskZoom,
            onResetZoomPan = {
              maskZoom = 1f
              maskPan = Offset.Zero
            },
            onLaunchIntent = launchIntent,
            modifier = Modifier.weight(1.05f).fillMaxHeight().navigationBarsPadding(),
          )
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier.fillMaxWidth().weight(0.50f).padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            InteractivePreviewCanvas(
              state = state,
              viewModel = viewModel,
              maskZoom = maskZoom,
              maskPan = maskPan,
              onMaskZoomPanChange = { z, p ->
                maskZoom = z
                maskPan = p
              },
            )
          }

          StudioControlsDock(
            state = state,
            viewModel = viewModel,
            maskZoom = maskZoom,
            onResetZoomPan = {
              maskZoom = 1f
              maskPan = Offset.Zero
            },
            onLaunchIntent = launchIntent,
            modifier = Modifier.fillMaxWidth().weight(0.50f),
          )
        }
      }
    }
  }

  // Fullscreen Export Preview Modal
  val exportPreview = state.exportPreviewBitmap
  if (exportPreview != null) {
    Dialog(
      onDismissRequest = { viewModel.dismissExportPreview() },
      properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Surface(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)),
        color = Color.Transparent,
      ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
          Image(
            bitmap = exportPreview.asImageBitmap(),
            contentDescription = "Full resolution export preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
          )
          Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
          ) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
              Text(
                text = "${exportPreview.width} × ${exportPreview.height} px",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              )
            }
            Button(
              onClick = { viewModel.dismissExportPreview() },
              modifier = Modifier.height(48.dp),
            ) {
              Text("Close Preview")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InteractivePreviewCanvas(
  state: EditorUiState,
  viewModel: EditorViewModel,
  maskZoom: Float,
  maskPan: Offset,
  onMaskZoomPanChange: (Float, Offset) -> Unit,
) {
  var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

  Box(contentAlignment = Alignment.Center) {
    RealisticPhoneFrame(
      bitmap = state.previewBitmap,
      aspectRatio = state.previewAspectRatio,
      modifier =
        Modifier
          .fillMaxHeight()
          .onSizeChanged { canvasSize = it }
          .graphicsLayer {
            if (state.activeTab == EditorTab.SUBJECT) {
              scaleX = maskZoom
              scaleY = maskZoom
              translationX = maskPan.x
              translationY = maskPan.y
            }
          }
          .pointerInput(state.activeTab, state.previewAspectRatio) {
            if (state.activeTab == EditorTab.CLOCK || state.activeTab == EditorTab.DATE) {
              detectDragGestures(
                onDrag = { change, dragAmount ->
                  change.consume()
                  val w = canvasSize.width.coerceAtLeast(1).toFloat()
                  val h = canvasSize.height.coerceAtLeast(1).toFloat()
                  viewModel.dragClockBy(dragAmount.x / w, dragAmount.y / h)
                },
                onDragEnd = { viewModel.endClockDrag() },
                onDragCancel = { viewModel.endClockDrag() },
              )
            }
          }
          .pointerInput(state.activeTab, maskZoom, maskPan) {
            if (state.activeTab == EditorTab.SUBJECT) {
              awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val w = canvasSize.width.coerceAtLeast(1).toFloat()
                val h = canvasSize.height.coerceAtLeast(1).toFloat()
                var paintingStarted = false
                var currentZoom = maskZoom
                var currentPan = maskPan

                // Start single-finger mask stroke if only 1 pointer is down
                fun toCanvasNorm(pos: Offset): Pair<Float, Float> {
                  return (pos.x / w).coerceIn(0f, 1f) to (pos.y / h).coerceIn(0f, 1f)
                }

                while (true) {
                  val event = awaitPointerEvent()
                  val activePointers = event.changes.filter { it.pressed }
                  if (activePointers.isEmpty()) {
                    if (paintingStarted) viewModel.endMaskStroke()
                    break
                  }

                  if (activePointers.size >= 2) {
                    if (paintingStarted) {
                      viewModel.endMaskStroke()
                      paintingStarted = false
                    }
                    val zoomDelta = event.calculateZoom()
                    val panDelta = event.calculatePan()
                    currentZoom = (currentZoom * zoomDelta).coerceIn(1f, 4f)
                    val maxPanX = (w * (currentZoom - 1f)) * 0.5f
                    val maxPanY = (h * (currentZoom - 1f)) * 0.5f
                    currentPan =
                      if (currentZoom <= 1.02f) {
                        Offset.Zero
                      } else {
                        Offset(
                          x = (currentPan.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                          y = (currentPan.y + panDelta.y).coerceIn(-maxPanY, maxPanY),
                        )
                      }
                    onMaskZoomPanChange(currentZoom, currentPan)
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                  } else {
                    val pointer = activePointers.first()
                    val (nx, ny) = toCanvasNorm(pointer.position)
                    if (!paintingStarted) {
                      paintingStarted = true
                      viewModel.beginMaskStroke(nx, ny)
                    } else if (pointer.positionChanged()) {
                      viewModel.moveMaskStroke(nx, ny)
                    }
                    pointer.consume()
                  }
                }
              }
            }
          },
    ) {
      // Segmentation Progress Overlay (never blocks permanently—includes Cancel)
      if (state.isSegmenting) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color(0xBB070C17)),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text(
              text = state.segmentationProgressLabel ?: "Detecting subject…",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White,
            )
            val pct = state.segmentationDownloadPercent
            if (pct != null) {
              LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth(0.75f),
              )
            }
            OutlinedButton(
              onClick = { viewModel.cancelAutoSegmentation() },
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
            ) {
              Text("Edit Manually", color = Color.White)
            }
          }
        }
      }

      if (state.isBeforeAfterPeek) {
        Surface(
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 30.dp),
          shape = RoundedCornerShape(50),
          color = Color(0xCC0B1324),
        ) {
          Text(
            text = "Before (No Foreground Overlap)",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StudioControlsDock(
  state: EditorUiState,
  viewModel: EditorViewModel,
  maskZoom: Float,
  onResetZoomPan: () -> Unit,
  onLaunchIntent: (Intent) -> Unit,
  modifier: Modifier = Modifier,
) {
  LiquidGlassPanel(
    modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    cornerRadius = 24.dp,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      TabRow(
        selectedTabIndex = EditorTab.entries.indexOf(state.activeTab),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
      ) {
        EditorTab.entries.forEach { tab ->
          Tab(
            selected = state.activeTab == tab,
            onClick = { viewModel.selectTab(tab) },
            text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}"),
          )
        }
      }

      Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        when (state.activeTab) {
          EditorTab.SUBJECT ->
            SubjectEditorPanel(
              state = state,
              viewModel = viewModel,
              maskZoom = maskZoom,
              onResetZoomPan = onResetZoomPan,
            )
          EditorTab.CLOCK -> ClockStudioPanel(state = state, viewModel = viewModel)
          EditorTab.DATE -> DateAndFramePanel(state = state, viewModel = viewModel)
          EditorTab.EXPORT ->
            ExportAndApplyPanel(
              state = state,
              viewModel = viewModel,
              onLaunchIntent = onLaunchIntent,
            )
        }
      }
    }
  }
}

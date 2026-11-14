package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockHorizontalAlign
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.DateFormatPreset
import com.example.domain.model.DatePosition
import com.example.domain.model.DecorativeWidgetStyle
import com.example.domain.model.ExportImageFormat
import com.example.domain.model.ExportMode
import com.example.domain.model.ExportResolutionPreset
import com.example.domain.model.MaskToolMode
import com.example.imaging.SegmentationEngine
import com.example.imaging.WallpaperApplyTarget
import com.example.ui.viewmodel.EditorUiState
import com.example.ui.viewmodel.EditorViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SubjectEditorPanel(
  state: EditorUiState,
  viewModel: EditorViewModel,
  maskZoom: Float,
  onResetZoomPan: () -> Unit,
) {
  val maskConfig = state.project?.maskConfig ?: return

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Auto Segmentation row & honest status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(
        onClick = { viewModel.runAutoSegmentation() },
        enabled = !state.isSegmenting,
        modifier = Modifier.weight(1f).height(48.dp).testTag("auto_segment_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Auto Detect Subject")
      }
      if (maskZoom > 1.05f) {
        OutlinedButton(
          onClick = onResetZoomPan,
          modifier = Modifier.height(48.dp).testTag("reset_mask_zoom_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(String.format(Locale.US, "%.1f× Reset", maskZoom))
        }
      }
    }

    if (state.segmentationStatus != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color =
          if (state.segmentationEngine == SegmentationEngine.LOCAL_SALIENCY_FALLBACK) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Text(
          text = state.segmentationStatus,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    }

    // Multiple detected subject inspection chips
    if (state.detectedSubjects.size > 1) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = maskConfig.activeSubjectIndex == -1,
          onClick = { viewModel.selectDetectedSubject(-1) },
          label = { Text("All Subjects") },
        )
        state.detectedSubjects.forEach { subject ->
          FilterChip(
            selected = maskConfig.activeSubjectIndex == subject.index,
            onClick = { viewModel.selectDetectedSubject(subject.index) },
            label = { Text(subject.label) },
          )
        }
      }
    }

    // Brush / Eraser + Undo / Redo toolbar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = state.toolMode == MaskToolMode.BRUSH,
        onClick = { viewModel.setMaskToolMode(MaskToolMode.BRUSH) },
        label = { Text("Brush") },
        leadingIcon = { Icon(Icons.Outlined.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag("mask_brush_button"),
      )
      FilterChip(
        selected = state.toolMode == MaskToolMode.ERASER,
        onClick = { viewModel.setMaskToolMode(MaskToolMode.ERASER) },
        label = { Text("Eraser") },
        leadingIcon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.testTag("mask_eraser_button"),
      )
      Spacer(Modifier.weight(1f))
      IconButton(
        onClick = { viewModel.undoMask() },
        enabled = state.canUndo,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("mask_undo_button"),
      ) {
        Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo stroke")
      }
      IconButton(
        onClick = { viewModel.redoMask() },
        enabled = state.canRedo,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("mask_redo_button"),
      ) {
        Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo stroke")
      }
    }

    Text(
      text = "Tip: Drag with 1 finger to paint/erase mask; pinch with 2 fingers to zoom & pan.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Sliders: Brush Size, Feather, Edge Refinement
    LabeledSlider(
      label = "Brush Size",
      valueText = "${(state.brushSizeNorm * 1000).roundToInt()}",
      value = state.brushSizeNorm,
      valueRange = 0.015f..0.16f,
      onValueChange = { viewModel.setBrushSize(it) },
    )
    LabeledSlider(
      label = "Edge Feather",
      valueText = String.format(Locale.US, "%.1f px", maskConfig.featherRadiusPx),
      value = maskConfig.featherRadiusPx,
      valueRange = 0f..14f,
      onValueChange = { value -> viewModel.updateMaskConfig { it.copy(featherRadiusPx = value) } },
    )
    LabeledSlider(
      label = "Edge Refinement",
      valueText = "${(maskConfig.edgeRefinement * 100).roundToInt()}%",
      value = maskConfig.edgeRefinement,
      valueRange = 0f..1f,
      onValueChange = { value -> viewModel.updateMaskConfig { it.copy(edgeRefinement = value) } },
    )

    // Visibility & Overlay Toggles
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ToggleTile(
        label = "Mask Overlay",
        checked = maskConfig.showMaskOverlay,
        onCheckedChange = { checked ->
          viewModel.updateMaskConfig { it.copy(showMaskOverlay = checked) }
        },
        modifier = Modifier.weight(1f).testTag("mask_overlay_toggle"),
      )
      ToggleTile(
        label = "Subject Layer",
        checked = maskConfig.subjectVisibility,
        onCheckedChange = { checked ->
          viewModel.updateMaskConfig { it.copy(subjectVisibility = checked) }
        },
        modifier = Modifier.weight(1f).testTag("subject_visibility_toggle"),
      )
    }

    // Mask Utilities: Reset / Invert / Clear
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = { viewModel.resetMaskToAuto() },
        modifier = Modifier.height(48.dp).testTag("reset_mask_button"),
      ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Reset Mask")
      }
      OutlinedButton(
        onClick = { viewModel.invertMask() },
        modifier = Modifier.height(48.dp).testTag("invert_mask_button"),
      ) {
        Icon(Icons.Outlined.InvertColors, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Invert")
      }
      OutlinedButton(
        onClick = { viewModel.clearMask() },
        modifier = Modifier.height(48.dp).testTag("clear_mask_button"),
      ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Clear")
      }
    }
  }
}

@Composable
fun ClockStudioPanel(
  state: EditorUiState,
  viewModel: EditorViewModel,
) {
  val clock = state.project?.clockConfig ?: return
  var showCustomColor by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Typography — Drag clock on preview to reposition",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Font carousel
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ClockFontStyle.entries.forEach { font ->
        val selected = clock.fontStyle == font
        Surface(
          onClick = { viewModel.updateClockConfig { it.copy(fontStyle = font) } },
          shape = RoundedCornerShape(14.dp),
          color =
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
          border =
            BorderStroke(
              1.dp,
              if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
          modifier = Modifier.testTag("font_${font.name.lowercase()}"),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "14:41",
              fontFamily = font.composeFontFamily(),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = font.displayName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Color Swatches + Custom Color button
    val presets =
      listOf(
        0xFFEBF3FF.toInt() to "Ice",
        0xFFFFFFFF.toInt() to "White",
        0xFF101522.toInt() to "Obsidian",
        0xFFC8D2E3.toInt() to "Silver",
        0xFF7EB4FF.toInt() to "Azure",
        0xFFF6D9A6.toInt() to "Champagne",
        0xFFFF9A9E.toInt() to "Coral",
      )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      presets.forEach { (argb, label) ->
        val isSelected = clock.colorArgb == argb
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Color(argb))
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color =
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
              )
              .clickable { viewModel.updateClockConfig { it.copy(colorArgb = argb) } }
              .testTag("clock_color_${label.lowercase()}"),
        )
      }
      FilterChip(
        selected = showCustomColor,
        onClick = { showCustomColor = !showCustomColor },
        label = { Text("Custom") },
        leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
      )
    }

    if (showCustomColor) {
      CustomRgbColorPicker(
        argb = clock.colorArgb,
        onColorChange = { newArgb -> viewModel.updateClockConfig { it.copy(colorArgb = newArgb) } },
      )
    }

    // Size, Width, Letter Spacing, Opacity
    LabeledSlider(
      label = "Clock Size",
      valueText = "${(clock.fontSizeRatio * 100).roundToInt()}%",
      value = clock.fontSizeRatio,
      valueRange = 0.12f..0.44f,
      onValueChange = { v -> viewModel.updateClockConfig { it.copy(fontSizeRatio = v) } },
    )
    LabeledSlider(
      label = "Width Scale",
      valueText = String.format(Locale.US, "%.2f×", clock.widthScale),
      value = clock.widthScale,
      valueRange = 0.75f..1.25f,
      onValueChange = { v -> viewModel.updateClockConfig { it.copy(widthScale = v) } },
    )
    LabeledSlider(
      label = "Digit Spacing",
      valueText = String.format(Locale.US, "%.2f em", clock.letterSpacingEm),
      value = clock.letterSpacingEm,
      valueRange = -0.08f..0.18f,
      onValueChange = { v -> viewModel.updateClockConfig { it.copy(letterSpacingEm = v) } },
    )
    LabeledSlider(
      label = "Opacity",
      valueText = "${(clock.opacity * 100).roundToInt()}%",
      value = clock.opacity,
      valueRange = 0.2f..1.0f,
      onValueChange = { v -> viewModel.updateClockConfig { it.copy(opacity = v) } },
    )

    // Alignment & Snap Center
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = clock.horizontalAlign == ClockHorizontalAlign.START,
        onClick = {
          viewModel.updateClockConfig {
            it.copy(horizontalAlign = ClockHorizontalAlign.START, posXFraction = 0.14f)
          }
        },
        label = { Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, contentDescription = "Align Left") },
      )
      FilterChip(
        selected = clock.horizontalAlign == ClockHorizontalAlign.CENTER,
        onClick = {
          viewModel.updateClockConfig {
            it.copy(horizontalAlign = ClockHorizontalAlign.CENTER, posXFraction = 0.50f)
          }
        },
        label = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = "Align Center") },
      )
      FilterChip(
        selected = clock.horizontalAlign == ClockHorizontalAlign.END,
        onClick = {
          viewModel.updateClockConfig {
            it.copy(horizontalAlign = ClockHorizontalAlign.END, posXFraction = 0.86f)
          }
        },
        label = { Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, contentDescription = "Align Right") },
      )
      Spacer(Modifier.weight(1f))
      OutlinedButton(
        onClick = {
          viewModel.updateClockConfig {
            it.copy(horizontalAlign = ClockHorizontalAlign.CENTER, posXFraction = 0.50f, posYFraction = 0.28f)
          }
        },
        modifier = Modifier.height(44.dp),
      ) {
        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Center")
      }
    }

    // Time Format & Static Time Picker
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ClockTimeFormat.entries.forEach { fmt ->
        FilterChip(
          selected = clock.timeFormat == fmt,
          onClick = { viewModel.updateClockConfig { it.copy(timeFormat = fmt) } },
          label = { Text(fmt.label) },
        )
      }
      if (clock.timeFormat == ClockTimeFormat.FORMAT_12H) {
        FilterChip(
          selected = clock.showAmPm,
          onClick = { viewModel.updateClockConfig { it.copy(showAmPm = !it.showAmPm) } },
          label = { Text("AM/PM") },
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ToggleTile(
        label = "Soft Shadow",
        checked = clock.shadowEnabled,
        onCheckedChange = { c -> viewModel.updateClockConfig { it.copy(shadowEnabled = c) } },
        modifier = Modifier.weight(1f),
      )
      ToggleTile(
        label = "Seconds",
        checked = clock.showSecondsPreview,
        onCheckedChange = { c -> viewModel.updateClockConfig { it.copy(showSecondsPreview = c) } },
        modifier = Modifier.weight(1f),
      )
    }

    ToggleTile(
      label = "Use Live Device Time in Preview",
      checked = clock.useLiveClock,
      onCheckedChange = { c -> viewModel.updateClockConfig { it.copy(useLiveClock = c) } },
    )

    if (!clock.useLiveClock) {
      TimeStepperRow(
        hour = clock.customHour,
        minute = clock.customMinute,
        onChange = { h, m ->
          viewModel.updateClockConfig { it.copy(customHour = h, customMinute = m) }
        },
      )
    }
  }
}

@Composable
fun DateAndFramePanel(
  state: EditorUiState,
  viewModel: EditorViewModel,
) {
  val project = state.project ?: return
  val clock = project.clockConfig

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    ToggleTile(
      label = "Show Date Label",
      checked = clock.showDate,
      onCheckedChange = { c -> viewModel.updateClockConfig { it.copy(showDate = c) } },
      modifier = Modifier.testTag("show_date_toggle"),
    )

    if (clock.showDate) {
      Text("Date Format", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DateFormatPreset.entries.forEach { preset ->
          FilterChip(
            selected = clock.dateFormat == preset,
            onClick = { viewModel.updateClockConfig { it.copy(dateFormat = preset) } },
            label = { Text(preset.label) },
          )
        }
      }

      Text("Date Position", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DatePosition.entries.forEach { pos ->
          FilterChip(
            selected = clock.datePosition == pos,
            onClick = { viewModel.updateClockConfig { it.copy(datePosition = pos) } },
            label = { Text(pos.label) },
          )
        }
      }

      Text("Date Font", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ClockFontStyle.entries.forEach { font ->
          FilterChip(
            selected = clock.dateFontStyle == font,
            onClick = { viewModel.updateClockConfig { it.copy(dateFontStyle = font) } },
            label = { Text(font.displayName, fontFamily = font.composeFontFamily()) },
          )
        }
      }

      Text("Date Color", style = MaterialTheme.typography.labelLarge)
      val dateColors = listOf(0xFFF2F6FF.toInt(), 0xFFFFFFFF.toInt(), 0xFF101522.toInt(), 0xFF9AC4FF.toInt(), 0xFFF6D9A6.toInt())
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        dateColors.forEach { argb ->
          val sel = clock.dateColorArgb == argb
          Box(
            modifier =
              Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(
                  if (sel) 2.5.dp else 1.dp,
                  if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  CircleShape,
                )
                .clickable { viewModel.updateClockConfig { it.copy(dateColorArgb = argb) } },
          )
        }
      }

      Text("Minimal Widget", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DecorativeWidgetStyle.entries.forEach { widget ->
          FilterChip(
            selected = clock.decorativeWidget == widget,
            onClick = { viewModel.updateClockConfig { it.copy(decorativeWidget = widget) } },
            label = { Text(widget.label) },
          )
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Text("Photo Framing", style = MaterialTheme.typography.titleMedium)

    LabeledSlider(
      label = "Photo Zoom",
      valueText = String.format(Locale.US, "%.2f×", project.cropScale),
      value = project.cropScale,
      valueRange = 1.0f..2.5f,
      onValueChange = { z -> viewModel.updateCrop(scale = z) },
    )
    LabeledSlider(
      label = "Horizontal Shift",
      valueText = "${(project.cropOffsetX * 100).roundToInt()}%",
      value = project.cropOffsetX,
      valueRange = -1.0f..1.0f,
      onValueChange = { x -> viewModel.updateCrop(offsetX = x) },
    )
    LabeledSlider(
      label = "Vertical Shift",
      valueText = "${(project.cropOffsetY * 100).roundToInt()}%",
      value = project.cropOffsetY,
      valueRange = -1.0f..1.0f,
      onValueChange = { y -> viewModel.updateCrop(offsetY = y) },
    )
    OutlinedButton(
      onClick = { viewModel.updateCrop(scale = 1.0f, offsetX = 0f, offsetY = 0f) },
      modifier = Modifier.height(46.dp),
    ) {
      Text("Reset Photo Framing")
    }
  }
}

@Composable
fun ExportAndApplyPanel(
  state: EditorUiState,
  viewModel: EditorViewModel,
  onLaunchIntent: (android.content.Intent) -> Unit,
) {
  val exportCfg = state.exportConfig

  Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Static Clock Transparency Notice
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.secondaryContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.static_clock_notice),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }

    // Two explicit export modes
    Text("Export Mode", style = MaterialTheme.typography.labelLarge)
    ExportMode.entries.forEach { mode ->
      val selected = exportCfg.mode == mode
      Surface(
        onClick = { viewModel.updateExportConfig { it.copy(mode = mode) } },
        shape = RoundedCornerShape(14.dp),
        color =
          if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
          else MaterialTheme.colorScheme.surfaceContainerHigh,
        border =
          BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          ),
        modifier = Modifier.fillMaxWidth().testTag("export_mode_${mode.name.lowercase()}"),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(mode.title, style = MaterialTheme.typography.titleMedium)
          Text(
            mode.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Resolution Presets
    Text("Resolution", style = MaterialTheme.typography.labelLarge)
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ExportResolutionPreset.entries.forEach { preset ->
        FilterChip(
          selected = exportCfg.preset == preset,
          onClick = { viewModel.updateExportConfig { it.copy(preset = preset) } },
          label = { Text(preset.label) },
        )
      }
    }

    if (exportCfg.preset == ExportResolutionPreset.CUSTOM) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = exportCfg.customWidth.toString(),
          onValueChange = { txt ->
            val w = txt.filter { it.isDigit() }.toIntOrNull() ?: 1080
            viewModel.updateExportConfig { it.copy(customWidth = w) }
          },
          label = { Text("Width px") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = exportCfg.customHeight.toString(),
          onValueChange = { txt ->
            val h = txt.filter { it.isDigit() }.toIntOrNull() ?: 2400
            viewModel.updateExportConfig { it.copy(customHeight = h) }
          },
          label = { Text("Height px") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Format & Quality
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ExportImageFormat.entries.forEach { fmt ->
        FilterChip(
          selected = exportCfg.format == fmt,
          onClick = { viewModel.updateExportConfig { it.copy(format = fmt) } },
          label = { Text(fmt.label) },
        )
      }
    }

    if (exportCfg.format == ExportImageFormat.JPEG) {
      LabeledSlider(
        label = "JPEG Quality",
        valueText = "${exportCfg.jpegQuality}%",
        value = exportCfg.jpegQuality.toFloat(),
        valueRange = 65f..100f,
        onValueChange = { q -> viewModel.updateExportConfig { it.copy(jpegQuality = q.roundToInt()) } },
      )
    }

    // Primary Export Buttons
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = { viewModel.previewExportFullRes() },
        enabled = !state.isExporting,
        modifier = Modifier.weight(1f).height(50.dp).testTag("preview_export_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Outlined.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Preview")
      }

      Button(
        onClick = { viewModel.exportToPictures() },
        enabled = !state.isExporting,
        modifier = Modifier.weight(1.4f).height(50.dp).testTag("save_image_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (state.isExporting) "Rendering…" else "Save Image")
      }
    }

    // Share & Open in Gallery after export
    if (state.lastExportOutcome != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = { viewModel.buildShareLastExportIntent()?.let(onLaunchIntent) },
          modifier = Modifier.weight(1f).height(48.dp).testTag("share_export_button"),
        ) {
          Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Share")
        }
        OutlinedButton(
          onClick = { viewModel.buildOpenInGalleryIntent()?.let(onLaunchIntent) },
          modifier = Modifier.weight(1f).height(48.dp).testTag("open_gallery_button"),
        ) {
          Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Gallery")
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Text("Apply Directly via Android", style = MaterialTheme.typography.labelLarge)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
      OutlinedButton(
        onClick = { viewModel.applyWallpaper(WallpaperApplyTarget.LOCK_SCREEN) },
        enabled = !state.isExporting,
        modifier = Modifier.weight(1f).height(48.dp).testTag("apply_lock_button"),
      ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Lock", maxLines = 1)
      }
      OutlinedButton(
        onClick = { viewModel.applyWallpaper(WallpaperApplyTarget.HOME_SCREEN) },
        enabled = !state.isExporting,
        modifier = Modifier.weight(1f).height(48.dp).testTag("apply_home_button"),
      ) {
        Icon(Icons.Outlined.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Home", maxLines = 1)
      }
      OutlinedButton(
        onClick = { viewModel.applyWallpaper(WallpaperApplyTarget.BOTH) },
        enabled = !state.isExporting,
        modifier = Modifier.weight(1f).height(48.dp).testTag("apply_both_button"),
      ) {
        Icon(Icons.Outlined.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Both", maxLines = 1)
      }
    }
  }
}

@Composable
private fun LabeledSlider(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
      Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
  }
}

@Composable
private fun ToggleTile(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = { onCheckedChange(!checked) },
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.width(8.dp))
      Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
  }
}

@Composable
private fun TimeStepperRow(
  hour: Int,
  minute: Int,
  onChange: (Int, Int) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Static Time", style = MaterialTheme.typography.bodyMedium)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = { onChange((hour + 23) % 24, minute) },
          modifier = Modifier.height(40.dp),
        ) { Text("-H") }
        Text(
          text = String.format(Locale.US, "%02d:%02d", hour, minute),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedButton(
          onClick = { onChange((hour + 1) % 24, minute) },
          modifier = Modifier.height(40.dp),
        ) { Text("+H") }
        OutlinedButton(
          onClick = { onChange(hour, (minute + 59) % 60) },
          modifier = Modifier.height(40.dp),
        ) { Text("-M") }
        OutlinedButton(
          onClick = { onChange(hour, (minute + 1) % 60) },
          modifier = Modifier.height(40.dp),
        ) { Text("+M") }
      }
    }
  }
}

@Composable
private fun CustomRgbColorPicker(
  argb: Int,
  onColorChange: (Int) -> Unit,
) {
  val r = android.graphics.Color.red(argb).toFloat()
  val g = android.graphics.Color.green(argb).toFloat()
  val b = android.graphics.Color.blue(argb).toFloat()

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    LabeledSlider("Red", "${r.roundToInt()}", r, 0f..255f) { nr ->
      onColorChange(Color(nr / 255f, g / 255f, b / 255f, 1f).toArgb())
    }
    LabeledSlider("Green", "${g.roundToInt()}", g, 0f..255f) { ng ->
      onColorChange(Color(r / 255f, ng / 255f, b / 255f, 1f).toArgb())
    }
    LabeledSlider("Blue", "${b.roundToInt()}", b, 0f..255f) { nb ->
      onColorChange(Color(r / 255f, g / 255f, nb / 255f, 1f).toArgb())
    }
  }
}

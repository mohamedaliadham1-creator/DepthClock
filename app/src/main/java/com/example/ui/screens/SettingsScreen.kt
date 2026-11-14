package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.ExportImageFormat
import com.example.domain.model.ExportResolutionPreset
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.viewmodel.SettingsViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onBack: () -> Unit,
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
  val message by viewModel.message.collectAsStateWithLifecycle()
  val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

  LaunchedEffect(message) {
    val msg = message
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessage()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .align(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Export Defaults
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Export Defaults", style = MaterialTheme.typography.titleMedium)

            Text("Default Resolution", style = MaterialTheme.typography.labelLarge)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ExportResolutionPreset.entries.filter { it != ExportResolutionPreset.CUSTOM }.forEach { preset ->
                FilterChip(
                  selected = settings.defaultPreset == preset,
                  onClick = { viewModel.updateDefaultPreset(preset) },
                  label = { Text(preset.label) },
                )
              }
            }

            Text("Default Format", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ExportImageFormat.entries.forEach { fmt ->
                FilterChip(
                  selected = settings.defaultFormat == fmt,
                  onClick = { viewModel.updateDefaultFormat(fmt) },
                  label = { Text(fmt.label) },
                )
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Default JPEG Quality", style = MaterialTheme.typography.bodyMedium)
              Text(
                "${settings.defaultJpegQuality}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Slider(
              value = settings.defaultJpegQuality.toFloat(),
              onValueChange = { viewModel.updateDefaultQuality(it.roundToInt()) },
              valueRange = 65f..100f,
            )
          }
        }

        // Clock & Appearance Defaults
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Clock & Studio Appearance", style = MaterialTheme.typography.titleMedium)

            Text("Default Clock Style", style = MaterialTheme.typography.labelLarge)
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ClockFontStyle.entries.forEach { style ->
                FilterChip(
                  selected = settings.defaultClockStyle == style,
                  onClick = { viewModel.updateDefaultClockStyle(style) },
                  label = { Text(style.displayName, fontFamily = style.composeFontFamily()) },
                )
              }
            }

            Text("Default Clock Format", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ClockTimeFormat.entries.forEach { fmt ->
                FilterChip(
                  selected = settings.defaultClockFormat == fmt,
                  onClick = { viewModel.updateDefaultClockFormat(fmt) },
                  label = { Text(fmt.label) },
                )
              }
            }

            Text("Appearance", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AppThemeMode.entries.forEach { mode ->
                FilterChip(
                  selected = settings.themeMode == mode,
                  onClick = { viewModel.updateThemeMode(mode) },
                  label = { Text(mode.label) },
                  modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
                )
              }
            }
          }
        }

        // Storage & Cache
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Storage & Cache", style = MaterialTheme.typography.titleMedium)
            val cacheMb = String.format(Locale.US, "%.2f MB", cacheBytes / (1024f * 1024f))
            Text(
              "Temporary preview & export cache: $cacheMb (Saved projects are preserved.)",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = { viewModel.clearCache() },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("clear_cache_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Outlined.CleaningServices, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Clear Temporary Cache")
            }
          }
        }

        // About & Privacy
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
              Text("About DepthClock & Privacy", style = MaterialTheme.typography.titleMedium)
            }
            Text(
              text = stringResource(R.string.privacy_notice),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text =
                "Engine: On-device Google ML Kit Subject Segmentation (optional Google Play Services module) + local edge/saliency fallback, with native Android Canvas DST_IN alpha compositing.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "DepthClock v1.0 • Optimized for 1080×2400 Portrait Displays",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

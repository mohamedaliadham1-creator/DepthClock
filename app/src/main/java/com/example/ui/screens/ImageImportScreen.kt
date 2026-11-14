package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.imaging.SampleSceneGenerator
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.viewmodel.ImportUiState
import com.example.ui.viewmodel.ImportViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageImportScreen(
  viewModel: ImportViewModel,
  onBack: () -> Unit,
  onProjectCreated: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
      if (uri != null) {
        viewModel.onPhotoSelected(uri)
      }
    }

  val documentPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
      if (uri != null) {
        viewModel.onPhotoSelected(uri)
      }
    }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Import Photograph", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("import_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
            )
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
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .align(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Picker Actions Card (Zero-permission Photo Picker & SAF)
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Select Source Photo",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "Supports high-resolution JPEG, PNG, and WebP photos in portrait or landscape. Processed locally with EXIF orientation preserved.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pick_gallery_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
              Spacer(Modifier.width(10.dp))
              Text("Choose from Gallery", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
              onClick = {
                documentPickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp", "image/*"))
              },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_files_button"),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Icon(Icons.Outlined.FolderOpen, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text("Browse Full-Res Files", style = MaterialTheme.typography.labelLarge)
            }
          }
        }

        // State-driven preview or status
        when (val state = uiState) {
          is ImportUiState.Idle -> {
            SampleScenesSection(
              onSelectSample = { kind -> viewModel.importSampleScene(kind, onProjectCreated) },
            )
          }

          is ImportUiState.Inspecting,
          is ImportUiState.Importing -> {
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                Text(
                  text =
                    if (state is ImportUiState.Inspecting) "Inspecting photo dimensions & EXIF…"
                    else "Preparing lossless studio project…",
                  style = MaterialTheme.typography.bodyLarge,
                )
              }
            }
          }

          is ImportUiState.Error -> {
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                  Text(
                    text = "Could Not Open Photograph",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = { viewModel.resetError() },
                  modifier = Modifier.height(48.dp),
                ) {
                  Text("Dismiss")
                }
              }
            }
          }

          is ImportUiState.PreviewReady -> {
            val preview = state.preview
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                val safeAspect =
                  (preview.width.toFloat() / preview.height.coerceAtLeast(1)).coerceIn(0.45f, 1.8f)

                Box(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height(260.dp)
                      .clip(RoundedCornerShape(18.dp))
                      .background(MaterialTheme.colorScheme.background),
                  contentAlignment = Alignment.Center,
                ) {
                  AsyncImage(
                    model = preview.uriString,
                    contentDescription = "Selected photograph preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().aspectRatio(safeAspect, matchHeightConstraintsFirst = true),
                  )
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val sizeMb =
                    if (preview.fileSizeBytes > 0) {
                      String.format(Locale.US, " • %.1f MB", preview.fileSizeBytes / (1024f * 1024f))
                    } else ""
                  Text(
                    text = "${preview.width} × ${preview.height} px$sizeMb",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  if (preview.orientationCorrected) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                      )
                      Text(
                        text = "EXIF Auto-Aligned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                      )
                    }
                  }
                }

                OutlinedTextField(
                  value = state.projectTitle,
                  onValueChange = { viewModel.updateTitle(it) },
                  label = { Text("Project Title") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("project_title_input"),
                  shape = RoundedCornerShape(14.dp),
                )

                Button(
                  onClick = { viewModel.importSelectedPhoto(onProjectCreated) },
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height(54.dp)
                      .testTag("open_in_studio_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Text("Open in Depth Studio", style = MaterialTheme.typography.titleMedium)
                }
              }
            }
          }
        }

        // Local privacy guarantee
        Text(
          text = stringResource(R.string.privacy_notice),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun SampleScenesSection(
  onSelectSample: (SampleSceneGenerator.SampleKind) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "Built-In Sample Scenes",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    SampleSceneGenerator.SampleKind.entries.forEach { kind ->
      Surface(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectSample(kind) }
            .testTag("sample_scene_${kind.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Icon(
              imageVector =
                if (kind == SampleSceneGenerator.SampleKind.BELL_TOWER) {
                  Icons.Outlined.Architecture
                } else {
                  Icons.Outlined.Landscape
                },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(10.dp).size(22.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = kind.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = kind.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = "Open",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

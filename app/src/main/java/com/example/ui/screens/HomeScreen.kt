package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.CreationProject
import com.example.imaging.SampleSceneGenerator
import com.example.ui.components.DepthClockLogoMark
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.components.RealisticPhoneFrame
import com.example.ui.viewmodel.HomeViewModel
import java.io.File

@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onCreateWallpaper: () -> Unit,
  onOpenProject: (String) -> Unit,
  onOpenCreations: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val creations by viewModel.creations.collectAsStateWithLifecycle()
  val featuredAfter by viewModel.featuredPreview.collectAsStateWithLifecycle()
  val featuredBefore by viewModel.featuredBeforePreview.collectAsStateWithLifecycle()
  val isOpeningSample by viewModel.isOpeningSample.collectAsStateWithLifecycle()
  var showBefore by remember { mutableStateOf(false) }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.background,
              ),
            ),
          )
          .padding(innerPadding),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp)
            .align(Alignment.TopCenter)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // Minimal editorial header: Logo + Name + Settings
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            DepthClockLogoMark(size = 38.dp)
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            IconButton(
              onClick = onOpenSettings,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Open settings",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        // Hero Featured Wallpaper Preview
        LiquidGlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .widthIn(max = 215.dp)
                  .fillMaxWidth(0.58f)
                  .pointerInput(Unit) {
                    detectTapGestures(
                      onPress = {
                        showBefore = true
                        tryAwaitRelease()
                        showBefore = false
                      },
                    )
                  },
            ) {
              RealisticPhoneFrame(
                bitmap = if (showBefore) featuredBefore else featuredAfter,
                aspectRatio = 9f / 20f,
              ) {
                Surface(
                  modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                  shape = RoundedCornerShape(50),
                  color = Color(0xAA0A1222),
                  border = BorderStroke(0.75.dp, Color(0x44FFFFFF)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Compare,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = if (showBefore) "Flat Clock" else "Hold to Compare",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White,
                    )
                  }
                }
              }
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Foreground Depth, Layered Naturally",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Subjects emerge in front of your lock-screen clock with on-device masking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            // Primary CTAs
            Button(
              onClick = onCreateWallpaper,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("create_wallpaper_button"),
              shape = RoundedCornerShape(18.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.create_wallpaper),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            OutlinedButton(
              onClick = {
                viewModel.launchSampleProject(SampleSceneGenerator.SampleKind.BELL_TOWER) { id ->
                  onOpenProject(id)
                }
              },
              enabled = !isOpeningSample,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("try_sample_button"),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              if (isOpeningSample) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              } else {
                Icon(
                  imageVector = Icons.Outlined.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.try_sample_scene),
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }

        // My Creations / Recently Edited Section
        EditorialSectionHeader(
          title = stringResource(R.string.my_creations),
          actionText = if (creations.isNotEmpty()) "View All (${creations.size})" else null,
          onAction = onOpenCreations,
        )

        AnimatedVisibility(
          visible = creations.isEmpty(),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          LiquidGlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Layers,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(12.dp).size(24.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "No saved creations yet",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Import any portrait or architectural photo, or try the sample scene above to begin.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        if (creations.isNotEmpty()) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
          ) {
            items(creations.take(8), key = { it.id }) { project ->
              RecentCreationCard(
                project = project,
                onClick = { onOpenProject(project.id) },
              )
            }
          }
        }

        Spacer(Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun RecentCreationCard(
  project: CreationProject,
  onClick: () -> Unit,
) {
  val thumbBitmap =
    remember(project.thumbnailPath, project.updatedAt) {
      val path = project.thumbnailPath
      if (path != null && File(path).exists()) {
        BitmapFactory.decodeFile(path)
      } else {
        null
      }
    }

  Surface(
    modifier =
      Modifier
        .width(126.dp)
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("recent_creation_${project.id}"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 18f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
      ) {
        if (thumbBitmap != null) {
          Image(
            bitmap = thumbBitmap.asImageBitmap(),
            contentDescription = "${project.title} wallpaper thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.Layers,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Text(
        text = project.title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

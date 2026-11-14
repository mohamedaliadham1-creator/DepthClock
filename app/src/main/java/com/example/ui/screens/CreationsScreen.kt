package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CreationProject
import com.example.imaging.WallpaperApplyTarget
import com.example.ui.components.LiquidGlassPanel
import com.example.ui.viewmodel.CreationsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationsScreen(
  viewModel: CreationsViewModel,
  onBack: () -> Unit,
  onOpenProject: (String) -> Unit,
  onCreateNew: () -> Unit,
) {
  val creations by viewModel.creations.collectAsStateWithLifecycle()
  val busyId by viewModel.busyProjectId.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingDelete by remember { mutableStateOf<CreationProject?>(null) }

  LaunchedEffect(statusMessage) {
    val msg = statusMessage
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
        title = { Text("My Creations", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("creations_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = onCreateNew,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("creations_add_button"),
          ) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Create new wallpaper")
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
    if (creations.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        LiquidGlassPanel {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text("No Saved Creations", style = MaterialTheme.typography.titleLarge)
            Text(
              "Your saved depth projects and wallpaper edits live privately on this device.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = onCreateNew,
              modifier = Modifier.height(48.dp).testTag("empty_create_button"),
            ) {
              Text("Create Wallpaper")
            }
          }
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize().padding(innerPadding).navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(creations, key = { it.id }) { project ->
          CreationGalleryCard(
            project = project,
            isBusy = busyId == project.id,
            onOpen = { onOpenProject(project.id) },
            onExportCopy = { viewModel.exportCopy(project) },
            onShare = {
              viewModel.shareProject(project) { intent ->
                runCatching { context.startActivity(intent) }
              }
            },
            onApplyLock = { viewModel.applyWallpaper(project, WallpaperApplyTarget.LOCK_SCREEN) },
            onDelete = { pendingDelete = project },
          )
        }
      }
    }
  }

  val targetDelete = pendingDelete
  if (targetDelete != null) {
    AlertDialog(
      onDismissRequest = { pendingDelete = null },
      title = { Text("Delete Creation?") },
      text = { Text("Remove \"${targetDelete.title}\" and its saved mask from DepthClock?") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteProject(targetDelete.id)
            pendingDelete = null
          },
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun CreationGalleryCard(
  project: CreationProject,
  isBusy: Boolean,
  onOpen: () -> Unit,
  onExportCopy: () -> Unit,
  onShare: () -> Unit,
  onApplyLock: () -> Unit,
  onDelete: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val thumbBitmap =
    remember(project.thumbnailPath, project.updatedAt) {
      val p = project.thumbnailPath
      if (p != null && File(p).exists()) BitmapFactory.decodeFile(p) else null
    }
  val dateLabel =
    remember(project.updatedAt) {
      SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
    }

  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(enabled = !project.isSourceMissing, onClick = onOpen)
        .testTag("creation_card_${project.id}"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
      ) {
        if (thumbBitmap != null) {
          Image(
            bitmap = thumbBitmap.asImageBitmap(),
            contentDescription = "${project.title} preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = "Missing preview",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (project.isSourceMissing) {
          Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = "Source Missing",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }

        if (isBusy) {
          Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = project.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "$dateLabel • ${project.imageWidth}×${project.imageHeight}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("creation_menu_${project.id}"),
          ) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Creation options")
          }
          DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            if (!project.isSourceMissing) {
              DropdownMenuItem(
                text = { Text("Export Copy") },
                leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onExportCopy()
                },
              )
              DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onShare()
                },
              )
              DropdownMenuItem(
                text = { Text("Apply to Lock Screen") },
                leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onApplyLock()
                },
              )
            }
            DropdownMenuItem(
              text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
              leadingIcon = {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              },
              onClick = {
                menuExpanded = false
                onDelete()
              },
            )
          }
        }
      }
    }
  }
}

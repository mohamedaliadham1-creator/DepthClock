package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.CreationDao
import com.example.data.local.CreationEntity
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.CreationProject
import com.example.domain.model.MaskConfig
import com.example.imaging.ImageCompositor
import com.example.imaging.ImageLoader
import com.example.imaging.SampleSceneGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CreationsRepository(
  private val context: Context,
  private val dao: CreationDao,
) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val projectsRoot: File
    get() = File(context.filesDir, "projects").apply { mkdirs() }

  val creationsFlow: Flow<List<CreationProject>> =
    dao.observeAllCreations().map { entities -> entities.map { it.toDomain() } }

  suspend fun getProject(id: String): CreationProject? =
    withContext(Dispatchers.IO) { dao.getCreationById(id)?.toDomain() }

  suspend fun createProjectFromUri(
    uri: Uri,
    title: String,
    defaultClockStyle: ClockFontStyle,
    defaultClockFormat: ClockTimeFormat,
  ): Result<CreationProject> =
    withContext(Dispatchers.IO) {
      runCatching {
        val projectId = UUID.randomUUID().toString()
        val projectDir = File(projectsRoot, projectId).apply { mkdirs() }
        val sourceFile = File(projectDir, "source_master.jpg")
        val maskFile = File(projectDir, "mask.png")
        val thumbFile = File(projectDir, "thumb.jpg")

        val (w, h) = ImageLoader.importToMasterFile(context, uri, sourceFile).getOrThrow()

        val now = System.currentTimeMillis()
        val clockConfig =
          ClockConfig(
            fontStyle = defaultClockStyle,
            timeFormat = defaultClockFormat,
          )
        val project =
          CreationProject(
            id = projectId,
            title = title.ifBlank { "Untitled Depth" },
            sourceImagePath = sourceFile.absolutePath,
            originalUriString = uri.toString(),
            maskFilePath = maskFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            lastExportedPath = null,
            lastExportedUri = null,
            imageWidth = w,
            imageHeight = h,
            clockConfig = clockConfig,
            maskConfig = MaskConfig(),
            createdAt = now,
            updatedAt = now,
          )

        generateAndSaveThumbnail(project)
        dao.upsertCreation(project.toEntity())
        project
      }
    }

  suspend fun createProjectFromSample(
    kind: SampleSceneGenerator.SampleKind,
    defaultClockStyle: ClockFontStyle,
    defaultClockFormat: ClockTimeFormat,
  ): Result<CreationProject> =
    withContext(Dispatchers.IO) {
      runCatching {
        val projectId = UUID.randomUUID().toString()
        val projectDir = File(projectsRoot, projectId).apply { mkdirs() }
        val sourceFile = File(projectDir, "source_master.png")
        val maskFile = File(projectDir, "mask.png")
        val thumbFile = File(projectDir, "thumb.jpg")

        val sampleW = 1080
        val sampleH = 2400
        val sampleBmp = SampleSceneGenerator.generateSamplePhoto(kind, sampleW, sampleH)
        FileOutputStream(sourceFile).use { sampleBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sampleBmp.recycle()

        val refMask = SampleSceneGenerator.generateReferenceMask(kind, 540, 1200)
        FileOutputStream(maskFile).use { refMask.compress(Bitmap.CompressFormat.PNG, 100, it) }
        refMask.recycle()

        val now = System.currentTimeMillis()
        val clockConfig =
          ClockConfig(
            fontStyle = defaultClockStyle,
            timeFormat = defaultClockFormat,
            posYFraction = if (kind == SampleSceneGenerator.SampleKind.BELL_TOWER) 0.28f else 0.25f,
          )
        val project =
          CreationProject(
            id = projectId,
            title = kind.title,
            sourceImagePath = sourceFile.absolutePath,
            originalUriString = null,
            maskFilePath = maskFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            lastExportedPath = null,
            lastExportedUri = null,
            imageWidth = sampleW,
            imageHeight = sampleH,
            clockConfig = clockConfig,
            maskConfig = MaskConfig(),
            createdAt = now,
            updatedAt = now,
          )
        generateAndSaveThumbnail(project)
        dao.upsertCreation(project.toEntity())
        project
      }
    }

  suspend fun saveProjectState(
    project: CreationProject,
    maskBitmap: Bitmap? = null,
    previewCompositeBitmap: Bitmap? = null,
  ) =
    withContext(Dispatchers.IO) {
      val updated = project.copy(updatedAt = System.currentTimeMillis())
      if (maskBitmap != null) {
        val maskFile = File(updated.maskFilePath)
        maskFile.parentFile?.mkdirs()
        FileOutputStream(maskFile).use { out ->
          maskBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
      }
      if (previewCompositeBitmap != null && updated.thumbnailPath != null) {
        val thumbFile = File(updated.thumbnailPath)
        thumbFile.parentFile?.mkdirs()
        val thumbScale = 420f / maxOf(previewCompositeBitmap.width, previewCompositeBitmap.height)
        val tw = (previewCompositeBitmap.width * thumbScale).toInt().coerceAtLeast(1)
        val th = (previewCompositeBitmap.height * thumbScale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(previewCompositeBitmap, tw, th, true)
        FileOutputStream(thumbFile).use { out ->
          scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (scaled !== previewCompositeBitmap) scaled.recycle()
      } else {
        generateAndSaveThumbnail(updated)
      }
      dao.upsertCreation(updated.toEntity())
    }

  suspend fun updateExportMetadata(
    projectId: String,
    exportedPath: String,
    exportedUri: String?,
  ) =
    withContext(Dispatchers.IO) {
      val existing = dao.getCreationById(projectId) ?: return@withContext
      dao.upsertCreation(
        existing.copy(
          lastExportedPath = exportedPath,
          lastExportedUri = exportedUri,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }

  suspend fun deleteProject(projectId: String) =
    withContext(Dispatchers.IO) {
      dao.deleteCreationById(projectId)
      val projectDir = File(projectsRoot, projectId)
      if (projectDir.exists()) {
        projectDir.deleteRecursively()
      }
    }

  suspend fun getTemporaryCacheBytes(): Long =
    withContext(Dispatchers.IO) {
      folderSize(context.cacheDir) + folderSize(File(context.filesDir, "exports"))
    }

  suspend fun clearTemporaryCache(): Long =
    withContext(Dispatchers.IO) {
      val before = getTemporaryCacheBytes()
      context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
      File(context.filesDir, "exports").listFiles()?.forEach { it.deleteRecursively() }
      before
    }

  private suspend fun generateAndSaveThumbnail(project: CreationProject) {
    val thumbPath = project.thumbnailPath ?: return
    val src = ImageLoader.decodeFileBounded(project.sourceImagePath, 420) ?: return
    val maskFile = File(project.maskFilePath)
    val mask = if (maskFile.exists()) BitmapFactory.decodeFile(maskFile.absolutePath) else null
    try {
      val composed =
        ImageCompositor.composite(
          context = context,
          sourceBitmap = src,
          maskBitmap = mask,
          clockConfig = project.clockConfig,
          params =
            ImageCompositor.CompositeParams(
              targetWidth = 270,
              targetHeight = 600,
              cropScale = project.cropScale,
              cropOffsetX = project.cropOffsetX,
              cropOffsetY = project.cropOffsetY,
              includeClock = true,
              includeForegroundSubject = project.maskConfig.subjectVisibility && mask != null,
            ),
        )
      val thumbFile = File(thumbPath)
      thumbFile.parentFile?.mkdirs()
      FileOutputStream(thumbFile).use { out ->
        composed.compress(Bitmap.CompressFormat.JPEG, 88, out)
      }
      composed.recycle()
    } finally {
      src.recycle()
      mask?.recycle()
    }
  }

  private fun folderSize(dir: File): Long {
    if (!dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    var sum = 0L
    dir.listFiles()?.forEach { sum += folderSize(it) }
    return sum
  }

  private fun CreationEntity.toDomain(): CreationProject {
    val parsedClock =
      runCatching { json.decodeFromString<ClockConfig>(clockConfigJson) }.getOrDefault(ClockConfig())
    val parsedMask =
      runCatching { json.decodeFromString<MaskConfig>(maskConfigJson) }.getOrDefault(MaskConfig())
    val sourceExists = File(sourceImagePath).exists()
    return CreationProject(
      id = id,
      title = title,
      sourceImagePath = sourceImagePath,
      originalUriString = originalUriString,
      maskFilePath = maskFilePath,
      thumbnailPath = thumbnailPath,
      lastExportedPath = lastExportedPath,
      lastExportedUri = lastExportedUri,
      imageWidth = imageWidth,
      imageHeight = imageHeight,
      cropScale = cropScale,
      cropOffsetX = cropOffsetX,
      cropOffsetY = cropOffsetY,
      clockConfig = parsedClock,
      maskConfig = parsedMask,
      createdAt = createdAt,
      updatedAt = updatedAt,
      isSourceMissing = !sourceExists,
    )
  }

  private fun CreationProject.toEntity(): CreationEntity =
    CreationEntity(
      id = id,
      title = title,
      sourceImagePath = sourceImagePath,
      originalUriString = originalUriString,
      maskFilePath = maskFilePath,
      thumbnailPath = thumbnailPath,
      lastExportedPath = lastExportedPath,
      lastExportedUri = lastExportedUri,
      imageWidth = imageWidth,
      imageHeight = imageHeight,
      cropScale = cropScale,
      cropOffsetX = cropOffsetX,
      cropOffsetY = cropOffsetY,
      clockConfigJson = json.encodeToString(clockConfig),
      maskConfigJson = json.encodeToString(maskConfig),
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
}

package com.example.imaging

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.domain.model.CreationProject
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportImageFormat
import com.example.domain.model.ExportMode
import com.example.domain.model.ExportResolutionPreset
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class WallpaperApplyTarget(val label: String) {
  LOCK_SCREEN("Lock Screen"),
  HOME_SCREEN("Home Screen"),
  BOTH("Lock & Home"),
}

data class ExportOutcome(
  val exportedFilePath: String,
  val mediaStoreUri: Uri?,
  val shareUri: Uri,
  val width: Int,
  val height: Int,
  val format: ExportImageFormat,
)

object WallpaperExporter {

  fun resolveTargetDimensions(project: CreationProject, config: ExportConfig): Result<Pair<Int, Int>> {
    return runCatching {
      when (config.preset) {
        ExportResolutionPreset.REDMI_NOTE_11 -> 1080 to 2400
        ExportResolutionPreset.PORTRAIT_FHD_16_9 -> 1080 to 1920
        ExportResolutionPreset.PORTRAIT_QHD -> 1440 to 3200
        ExportResolutionPreset.ORIGINAL -> {
          val w = project.imageWidth.coerceAtLeast(1)
          val h = project.imageHeight.coerceAtLeast(1)
          w to h
        }
        ExportResolutionPreset.CUSTOM -> {
          val w = config.customWidth
          val h = config.customHeight
          require(w in 256..4320 && h in 256..4320) {
            "Custom dimensions must be between 256 and 4320 pixels."
          }
          w to h
        }
      }
    }
  }

  suspend fun renderExportBitmap(
    context: Context,
    project: CreationProject,
    exportConfig: ExportConfig,
    overrideMaskAlpha: ByteArray? = null,
    overrideMaskSize: Pair<Int, Int>? = null,
  ): Result<Bitmap> =
    withContext(Dispatchers.Default) {
      runCatching {
        val (targetW, targetH) = resolveTargetDimensions(project, exportConfig).getOrThrow()
        val sourceFile = File(project.sourceImagePath)
        require(sourceFile.exists()) { "Original project photo is missing from local storage." }

        val maxNeededEdge = maxOf(targetW, targetH, 2400)
        val sourceBitmap =
          ImageLoader.decodeFileBounded(project.sourceImagePath, maxNeededEdge)
            ?: error("Could not decode project photo for export.")

        val maskBitmap: Bitmap? =
          try {
            if (overrideMaskAlpha != null && overrideMaskSize != null) {
              val (mw, mh) = overrideMaskSize
              val scaleFactor = maxOf(1f, minOf(targetW, targetH).toFloat() / maxOf(1, minOf(mw, mh)))
              MaskProcessor.alphaBytesToBitmap(
                rawAlpha = overrideMaskAlpha,
                width = mw,
                height = mh,
                featherRadiusPx = project.maskConfig.featherRadiusPx,
                edgeRefinement = project.maskConfig.edgeRefinement,
              ).also {
                // Keep feather scale coherent across preview and export
                val _unused = scaleFactor
              }
            } else {
              val maskFile = File(project.maskFilePath)
              if (maskFile.exists()) {
                val rawMaskBmp = BitmapFactory.decodeFile(maskFile.absolutePath)
                if (rawMaskBmp != null) {
                  val bytes = MaskProcessor.bitmapToAlphaBytes(rawMaskBmp)
                  val mw = rawMaskBmp.width
                  val mh = rawMaskBmp.height
                  rawMaskBmp.recycle()
                  MaskProcessor.alphaBytesToBitmap(
                    rawAlpha = bytes,
                    width = mw,
                    height = mh,
                    featherRadiusPx = project.maskConfig.featherRadiusPx,
                    edgeRefinement = project.maskConfig.edgeRefinement,
                  )
                } else {
                  null
                }
              } else {
                null
              }
            }
          } catch (_: Throwable) {
            null
          }

        try {
          val includeClock = exportConfig.mode == ExportMode.DEPTH_CLOCK_WALLPAPER
          val includeFg =
            includeClock && project.maskConfig.subjectVisibility && maskBitmap != null

          ImageCompositor.composite(
            context = context,
            sourceBitmap = sourceBitmap,
            maskBitmap = maskBitmap,
            clockConfig = project.clockConfig,
            params =
              ImageCompositor.CompositeParams(
                targetWidth = targetW,
                targetHeight = targetH,
                cropScale = project.cropScale,
                cropOffsetX = project.cropOffsetX,
                cropOffsetY = project.cropOffsetY,
                includeClock = includeClock,
                includeForegroundSubject = includeFg,
                showMaskOverlay = false,
                showAlignmentGuides = false,
              ),
          )
        } finally {
          sourceBitmap.recycle()
          maskBitmap?.recycle()
        }
      }
    }

  suspend fun exportToPictures(
    context: Context,
    project: CreationProject,
    exportConfig: ExportConfig,
    overrideMaskAlpha: ByteArray? = null,
    overrideMaskSize: Pair<Int, Int>? = null,
  ): Result<ExportOutcome> =
    withContext(Dispatchers.IO) {
      runCatching {
        val rendered =
          renderExportBitmap(context, project, exportConfig, overrideMaskAlpha, overrideMaskSize)
            .getOrThrow()

        try {
          val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
          val fileName = "DepthClock_${timestamp}.${exportConfig.format.extension}"
          val compressFormat =
            when (exportConfig.format) {
              ExportImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
              ExportImageFormat.PNG -> Bitmap.CompressFormat.PNG
            }
          val quality =
            if (exportConfig.format == ExportImageFormat.PNG) 100
            else exportConfig.jpegQuality.coerceIn(60, 100)

          // Save app-local export copy for reliable FileProvider Share & Quick Preview
          val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
          val internalFile = File(exportsDir, fileName)
          FileOutputStream(internalFile).use { out ->
            if (!rendered.compress(compressFormat, quality, out)) {
              error("Failed to encode image.")
            }
          }

          val shareUri =
            FileProvider.getUriForFile(
              context,
              "${context.packageName}.fileprovider",
              internalFile,
            )

          // Save to Pictures/DepthClock via MediaStore on Android 10+ (zero permissions required)
          var galleryUri: Uri? = null
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values =
              ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, exportConfig.format.mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DepthClock")
                put(MediaStore.Images.Media.IS_PENDING, 1)
              }
            val resolver = context.contentResolver
            val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (inserted != null) {
              try {
                resolver.openOutputStream(inserted)?.use { os ->
                  if (!rendered.compress(compressFormat, quality, os)) {
                    error("Failed writing image to MediaStore.")
                  }
                }
                val doneValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(inserted, doneValues, null, null)
                galleryUri = inserted
              } catch (e: Throwable) {
                resolver.delete(inserted, null, null)
                throw e
              }
            }
          }

          ExportOutcome(
            exportedFilePath = internalFile.absolutePath,
            mediaStoreUri = galleryUri,
            shareUri = shareUri,
            width = rendered.width,
            height = rendered.height,
            format = exportConfig.format,
          )
        } finally {
          rendered.recycle()
        }
      }
    }

  suspend fun applyWallpaper(
    context: Context,
    bitmap: Bitmap,
    target: WallpaperApplyTarget,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported) {
          error("This device does not support changing wallpapers programmatically.")
        }
        if (!manager.isSetWallpaperAllowed) {
          error("Setting wallpaper is currently restricted by system policy. Save the image and apply it from your Gallery.")
        }

        val flags =
          when (target) {
            WallpaperApplyTarget.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
            WallpaperApplyTarget.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
            WallpaperApplyTarget.BOTH -> WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM
          }

        val resultId = manager.setBitmap(bitmap, null, true, flags)
        if (resultId == 0) {
          error(
            "Android did not confirm applying the ${target.label.lowercase()} wallpaper. On HyperOS/MIUI, some themes restrict lock-screen changes—use 'Open in Gallery' to apply manually.",
          )
        }
        "Applied to ${target.label}."
      }
    }

  fun buildShareIntent(context: Context, shareUri: Uri, mimeType: String): Intent {
    val send =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return Intent.createChooser(send, "Share DepthClock Wallpaper")
  }

  fun buildOpenInGalleryIntent(uri: Uri, mimeType: String): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }
}

package com.example.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.domain.model.ImportedPhotoPreview
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageLoader {

  private const val MAX_MASTER_EDGE = 4096

  suspend fun inspectUri(context: Context, uri: Uri): Result<ImportedPhotoPreview> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        var displayName = "Selected Photo"
        var fileSize = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (cursor.moveToFirst()) {
            if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
            if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
          }
        }

        val bounds =
          BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { opts ->
            resolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream, null, opts)
            } ?: error("Cannot open selected image stream.")
          }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
          error("Unsupported or corrupted image format.")
        }

        val orientation =
          resolver.openInputStream(uri)?.use { stream ->
            runCatching {
              ExifInterface(stream)
                .getAttributeInt(
                  ExifInterface.TAG_ORIENTATION,
                  ExifInterface.ORIENTATION_NORMAL,
                )
            }
              .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
          } ?: ExifInterface.ORIENTATION_NORMAL

        val rotated90or270 =
          orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

        val finalW = if (rotated90or270) bounds.outHeight else bounds.outWidth
        val finalH = if (rotated90or270) bounds.outWidth else bounds.outHeight

        ImportedPhotoPreview(
          uriString = uri.toString(),
          displayName = displayName,
          width = finalW,
          height = finalH,
          mimeType = mimeType ?: bounds.outMimeType,
          orientationCorrected = orientation != ExifInterface.ORIENTATION_NORMAL,
          fileSizeBytes = fileSize,
        )
      }
    }

  suspend fun importToMasterFile(
    context: Context,
    uri: Uri,
    destFile: File,
  ): Result<Pair<Int, Int>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val bounds =
          BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { opts ->
            resolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream, null, opts)
            } ?: error("Unable to read selected image.")
          }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
          error("Selected file is not a valid image.")
        }

        val orientation =
          resolver.openInputStream(uri)?.use { stream ->
            runCatching {
              ExifInterface(stream)
                .getAttributeInt(
                  ExifInterface.TAG_ORIENTATION,
                  ExifInterface.ORIENTATION_NORMAL,
                )
            }
              .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
          } ?: ExifInterface.ORIENTATION_NORMAL

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_MASTER_EDGE)
        val decodeOpts =
          BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }

        val decoded =
          resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: error("Failed to decode image into memory.")

        val oriented = applyExifOrientation(decoded, orientation)
        if (oriented !== decoded) decoded.recycle()

        destFile.parentFile?.mkdirs()
        FileOutputStream(destFile).use { out ->
          // Preserve lossless master quality in internal project storage
          val format =
            if (bounds.outMimeType?.contains("png", ignoreCase = true) == true) {
              Bitmap.CompressFormat.PNG
            } else {
              Bitmap.CompressFormat.JPEG
            }
          oriented.compress(format, 98, out)
        }
        val dims = oriented.width to oriented.height
        oriented.recycle()
        dims
      }
    }

  suspend fun decodeFileBounded(path: String, maxEdge: Int): Bitmap? =
    withContext(Dispatchers.IO) {
      val file = File(path)
      if (!file.exists()) return@withContext null
      val bounds =
        BitmapFactory.Options().apply { inJustDecodeBounds = true }.also {
          BitmapFactory.decodeFile(path, it)
        }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
      val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
      val opts =
        BitmapFactory.Options().apply {
          inSampleSize = sample
          inPreferredConfig = Bitmap.Config.ARGB_8888
        }
      BitmapFactory.decodeFile(path, opts)
    }

  fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    val longest = maxOf(width, height)
    while (longest / (sample * 2) >= maxEdge) {
      sample *= 2
    }
    return sample
  }

  private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
        matrix.setRotate(180f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.setRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.setRotate(270f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
      else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}

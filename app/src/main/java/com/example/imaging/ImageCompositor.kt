package com.example.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.example.domain.model.ClockConfig
import kotlin.math.max

/**
 * Genuine 3-layer depth compositing engine:
 *   Layer 1: Original photograph (fitted + framed)
 *   Layer 2: Digital clock & date
 *   Layer 3: Isolated foreground subject rendered above the clock via feathered alpha mask
 *
 * The exact same pipeline powers both the interactive Live Preview and final high-res Export.
 */
object ImageCompositor {

  data class CompositeParams(
    val targetWidth: Int,
    val targetHeight: Int,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val includeClock: Boolean = true,
    val includeForegroundSubject: Boolean = true,
    val showMaskOverlay: Boolean = false,
    val showAlignmentGuides: Boolean = false,
    val snapXActive: Boolean = false,
    val snapYActive: Boolean = false,
    val clockTimeMillis: Long = System.currentTimeMillis(),
  )

  fun composite(
    context: Context,
    sourceBitmap: Bitmap,
    maskBitmap: Bitmap?,
    clockConfig: ClockConfig,
    params: CompositeParams,
  ): Bitmap {
    val w = params.targetWidth.coerceAtLeast(1)
    val h = params.targetHeight.coerceAtLeast(1)
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val imageMatrix =
      computeImageMatrix(
        srcWidth = sourceBitmap.width.toFloat(),
        srcHeight = sourceBitmap.height.toFloat(),
        dstWidth = w.toFloat(),
        dstHeight = h.toFloat(),
        cropScale = params.cropScale,
        cropOffsetX = params.cropOffsetX,
        cropOffsetY = params.cropOffsetY,
      )

    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    // LAYER 1: Original photograph background
    canvas.drawBitmap(sourceBitmap, imageMatrix, imagePaint)

    // LAYER 2: Digital clock & date
    if (params.includeClock) {
      ClockRenderer.drawClock(
        context = context,
        canvas = canvas,
        bounds = RectF(0f, 0f, w.toFloat(), h.toFloat()),
        config = clockConfig,
        nowMillis = params.clockTimeMillis,
        showAlignmentGuides = params.showAlignmentGuides,
        snapXActive = params.snapXActive,
        snapYActive = params.snapYActive,
      )
    }

    // LAYER 3: Isolated foreground subject rendered ABOVE the clock using feathered alpha mask
    if (params.includeForegroundSubject && maskBitmap != null) {
      val maskMatrix =
        Matrix().apply {
          setScale(
            sourceBitmap.width.toFloat() / maskBitmap.width.toFloat(),
            sourceBitmap.height.toFloat() / maskBitmap.height.toFloat(),
          )
          postConcat(imageMatrix)
        }

      val saveCount = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), null)
      canvas.drawBitmap(sourceBitmap, imageMatrix, imagePaint)
      val dstInPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
          xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
      canvas.drawBitmap(maskBitmap, maskMatrix, dstInPaint)
      canvas.restoreToCount(saveCount)
    }

    // Optional translucent foreground selection tint for inspection / manual editing
    if (params.showMaskOverlay && maskBitmap != null) {
      val overlay = MaskProcessor.createOverlayTintBitmap(maskBitmap)
      val maskMatrix =
        Matrix().apply {
          setScale(
            sourceBitmap.width.toFloat() / maskBitmap.width.toFloat(),
            sourceBitmap.height.toFloat() / maskBitmap.height.toFloat(),
          )
          postConcat(imageMatrix)
        }
      canvas.drawBitmap(overlay, maskMatrix, imagePaint)
      overlay.recycle()
    }

    return output
  }

  fun computeImageMatrix(
    srcWidth: Float,
    srcHeight: Float,
    dstWidth: Float,
    dstHeight: Float,
    cropScale: Float,
    cropOffsetX: Float,
    cropOffsetY: Float,
  ): Matrix {
    val baseScale = max(dstWidth / srcWidth, dstHeight / srcHeight)
    val totalScale = baseScale * cropScale.coerceIn(1.0f, 3.0f)

    val scaledW = srcWidth * totalScale
    val scaledH = srcHeight * totalScale

    val maxShiftX = max(0f, (scaledW - dstWidth) * 0.5f)
    val maxShiftY = max(0f, (scaledH - dstHeight) * 0.5f)

    val dx = (dstWidth - scaledW) * 0.5f + cropOffsetX.coerceIn(-1f, 1f) * maxShiftX
    val dy = (dstHeight - scaledH) * 0.5f + cropOffsetY.coerceIn(-1f, 1f) * maxShiftY

    return Matrix().apply {
      setScale(totalScale, totalScale)
      postTranslate(dx, dy)
    }
  }
}

package com.example.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * Generates clean, clock-free high-resolution architectural and landscape photography scenes on-device
 * so users can immediately test the full segmentation, mask editing, and 3-layer compositing pipeline
 * before importing their own gallery photos.
 */
object SampleSceneGenerator {

  enum class SampleKind(val title: String, val subtitle: String) {
    BELL_TOWER("Mediterranean Bell Tower", "Warm sandstone cupola against crisp azure sky"),
    ALPINE_PEAK("Midnight Ridge", "Snow-lit alpine summit against twilight indigo sky"),
  }

  fun generateSamplePhoto(
    kind: SampleKind = SampleKind.BELL_TOWER,
    width: Int = 1080,
    height: Int = 2400,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    when (kind) {
      SampleKind.BELL_TOWER -> drawBellTowerScene(canvas, width.toFloat(), height.toFloat())
      SampleKind.ALPINE_PEAK -> drawAlpinePeakScene(canvas, width.toFloat(), height.toFloat())
    }
    return bmp
  }

  /**
   * Generates a crisp reference alpha mask matching the sample scene so the Home screen featured
   * preview and instant demo never rely on pre-baked clocks.
   */
  fun generateReferenceMask(
    kind: SampleKind = SampleKind.BELL_TOWER,
    width: Int,
    height: Int,
  ): Bitmap {
    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(mask)
    canvas.drawColor(Color.TRANSPARENT)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    val w = width.toFloat()
    val h = height.toFloat()

    if (kind == SampleKind.BELL_TOWER) {
      val cx = w * 0.62f
      // Spire cross
      canvas.drawRect(cx - w * 0.006f, h * 0.165f, cx + w * 0.006f, h * 0.23f, paint)
      canvas.drawRect(cx - w * 0.025f, h * 0.18f, cx + w * 0.025f, h * 0.19f, paint)
      // Lantern cupola
      canvas.drawArc(
        RectF(cx - w * 0.048f, h * 0.222f, cx + w * 0.048f, h * 0.27f),
        180f,
        180f,
        true,
        paint,
      )
      canvas.drawRect(cx - w * 0.044f, h * 0.246f, cx + w * 0.044f, h * 0.30f, paint)
      // Balcony ring
      canvas.drawRoundRect(
        RectF(cx - w * 0.068f, h * 0.295f, cx + w * 0.068f, h * 0.318f),
        8f,
        8f,
        paint,
      )
      // Main slate dome
      canvas.drawArc(
        RectF(cx - w * 0.095f, h * 0.312f, cx + w * 0.095f, h * 0.435f),
        180f,
        180f,
        true,
        paint,
      )
      // Cornice
      canvas.drawRoundRect(
        RectF(cx - w * 0.118f, h * 0.370f, cx + w * 0.118f, h * 0.415f),
        10f,
        10f,
        paint,
      )
      // Drum & lower tower
      canvas.drawRect(cx - w * 0.105f, h * 0.410f, cx + w * 0.105f, h * 0.54f, paint)
      canvas.drawRoundRect(
        RectF(cx - w * 0.125f, h * 0.535f, cx + w * 0.125f, h * 0.558f),
        6f,
        6f,
        paint,
      )
      canvas.drawRect(cx - w * 0.115f, h * 0.555f, cx + w * 0.115f, h * 1.0f, paint)
    } else {
      val ridge =
        Path().apply {
          moveTo(0f, h)
          lineTo(0f, h * 0.62f)
          lineTo(w * 0.22f, h * 0.48f)
          lineTo(w * 0.38f, h * 0.53f)
          lineTo(w * 0.56f, h * 0.24f)
          lineTo(w * 0.76f, h * 0.47f)
          lineTo(w * 0.90f, h * 0.41f)
          lineTo(w, h * 0.55f)
          lineTo(w, h)
          close()
        }
      canvas.drawPath(ridge, paint)
    }
    return mask
  }

  private fun drawBellTowerScene(canvas: Canvas, w: Float, h: Float) {
    // Rich cerulean-azure sky gradient
    val skyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
              Color.rgb(34, 146, 245),
              Color.rgb(55, 162, 248),
              Color.rgb(92, 185, 250),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, w, h, skyPaint)

    val cx = w * 0.62f

    val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(214, 152, 72) }
    val warmWall =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx - w * 0.12f,
            0f,
            cx + w * 0.12f,
            0f,
            intArrayOf(Color.rgb(232, 178, 102), Color.rgb(199, 138, 66), Color.rgb(152, 98, 42)),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    val creamTrim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 232, 206) }
    val darkSlate =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx - w * 0.095f,
            0f,
            cx + w * 0.095f,
            0f,
            intArrayOf(Color.rgb(58, 66, 82), Color.rgb(34, 39, 50), Color.rgb(20, 24, 32)),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    val deepShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 28, 34) }
    val bronze = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(38, 42, 48) }

    // Cross spire
    canvas.drawRect(cx - w * 0.0035f, h * 0.165f, cx + w * 0.0035f, h * 0.215f, bronze)
    canvas.drawRect(cx - w * 0.022f, h * 0.181f, cx + w * 0.022f, h * 0.187f, bronze)
    canvas.drawCircle(cx, h * 0.218f, w * 0.011f, gold)

    // Lantern cupola
    canvas.drawArc(
      RectF(cx - w * 0.044f, h * 0.222f, cx + w * 0.044f, h * 0.268f),
      180f,
      180f,
      true,
      darkSlate,
    )
    canvas.drawRect(cx - w * 0.042f, h * 0.245f, cx + w * 0.042f, h * 0.296f, warmWall)
    // Lantern arches
    canvas.drawRoundRect(
      RectF(cx - w * 0.026f, h * 0.256f, cx - w * 0.008f, h * 0.290f),
      12f,
      12f,
      deepShadow,
    )
    canvas.drawRoundRect(
      RectF(cx + w * 0.008f, h * 0.256f, cx + w * 0.026f, h * 0.290f),
      12f,
      12f,
      deepShadow,
    )

    // Balcony balustrade
    canvas.drawRoundRect(
      RectF(cx - w * 0.068f, h * 0.295f, cx + w * 0.068f, h * 0.316f),
      6f,
      6f,
      creamTrim,
    )

    // Main Dome
    canvas.drawArc(
      RectF(cx - w * 0.095f, h * 0.312f, cx + w * 0.095f, h * 0.435f),
      180f,
      180f,
      true,
      darkSlate,
    )
    // Decorative dome ribs
    val ribPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(65, 220, 232, 248)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
      }
    for (i in -3..3) {
      val offset = i * w * 0.022f
      canvas.drawArc(
        RectF(cx - kotlin.math.abs(offset) - 8f, h * 0.314f, cx + kotlin.math.abs(offset) + 8f, h * 0.432f),
        180f,
        180f,
        false,
        ribPaint,
      )
    }

    // Cornice & relief band
    canvas.drawRoundRect(
      RectF(cx - w * 0.116f, h * 0.372f, cx + w * 0.116f, h * 0.415f),
      8f,
      8f,
      warmWall,
    )
    canvas.drawRect(cx - w * 0.118f, h * 0.370f, cx + w * 0.118f, h * 0.378f, creamTrim)
    for (i in -2..2) {
      canvas.drawCircle(cx + i * w * 0.045f, h * 0.396f, w * 0.011f, creamTrim)
    }

    // Main upper drum
    canvas.drawRect(cx - w * 0.105f, h * 0.414f, cx + w * 0.105f, h * 0.54f, warmWall)
    // Oculi windows
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 218, 190) }
    canvas.drawOval(RectF(cx - w * 0.070f, h * 0.438f, cx - w * 0.028f, h * 0.494f), rimPaint)
    canvas.drawOval(RectF(cx - w * 0.064f, h * 0.443f, cx - w * 0.034f, h * 0.489f), deepShadow)
    canvas.drawOval(RectF(cx + w * 0.028f, h * 0.438f, cx + w * 0.070f, h * 0.494f), rimPaint)
    canvas.drawOval(RectF(cx + w * 0.034f, h * 0.443f, cx + w * 0.064f, h * 0.489f), deepShadow)

    // Lower cornice and tower shaft
    canvas.drawRoundRect(
      RectF(cx - w * 0.124f, h * 0.535f, cx + w * 0.124f, h * 0.556f),
      5f,
      5f,
      creamTrim,
    )
    canvas.drawRect(cx - w * 0.114f, h * 0.556f, cx + w * 0.114f, h, warmWall)

    // Tall lower arched window
    canvas.drawRoundRect(
      RectF(cx - w * 0.032f, h * 0.59f, cx + w * 0.032f, h * 0.74f),
      40f,
      40f,
      creamTrim,
    )
    canvas.drawRoundRect(
      RectF(cx - w * 0.022f, h * 0.60f, cx + w * 0.022f, h * 0.73f),
      32f,
      32f,
      deepShadow,
    )
  }

  private fun drawAlpinePeakScene(canvas: Canvas, w: Float, h: Float) {
    val skyPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
              Color.rgb(10, 22, 52),
              Color.rgb(28, 62, 120),
              Color.rgb(88, 134, 198),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, w, h, skyPaint)

    val mountainLit =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            h * 0.24f,
            w,
            h,
            intArrayOf(Color.rgb(225, 236, 250), Color.rgb(68, 88, 120), Color.rgb(18, 25, 42)),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    val mountainShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 30, 50) }

    val ridge =
      Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.62f)
        lineTo(w * 0.22f, h * 0.48f)
        lineTo(w * 0.38f, h * 0.53f)
        lineTo(w * 0.56f, h * 0.24f)
        lineTo(w * 0.76f, h * 0.47f)
        lineTo(w * 0.90f, h * 0.41f)
        lineTo(w, h * 0.55f)
        lineTo(w, h)
        close()
      }
    canvas.drawPath(ridge, mountainLit)

    val shadowFacet =
      Path().apply {
        moveTo(w * 0.56f, h * 0.24f)
        lineTo(w * 0.76f, h * 0.47f)
        lineTo(w * 0.90f, h * 0.41f)
        lineTo(w, h * 0.55f)
        lineTo(w, h)
        lineTo(w * 0.53f, h)
        close()
      }
    canvas.drawPath(shadowFacet, mountainShadow)
  }
}

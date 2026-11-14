package com.example.imaging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockHorizontalAlign
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.DatePosition
import com.example.domain.model.DecorativeWidgetStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ClockRenderer {

  private val typefaceCache = mutableMapOf<Int, Typeface>()

  fun resolveTypeface(context: Context, style: ClockFontStyle): Typeface {
    return typefaceCache[style.fontRes]
      ?: runCatching { ResourcesCompat.getFont(context, style.fontRes) }
        .getOrNull()
        ?.also { typefaceCache[style.fontRes] = it }
      ?: Typeface.create(Typeface.SANS_SERIF, if (style.defaultWeightBold) Typeface.BOLD else Typeface.NORMAL)
  }

  fun formatTimeString(config: ClockConfig, nowMillis: Long = System.currentTimeMillis()): Pair<String, String?> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour24 = if (config.useLiveClock) cal.get(Calendar.HOUR_OF_DAY) else config.customHour.coerceIn(0, 23)
    val minute = if (config.useLiveClock) cal.get(Calendar.MINUTE) else config.customMinute.coerceIn(0, 59)
    val second = if (config.useLiveClock) cal.get(Calendar.SECOND) else config.customSecond.coerceIn(0, 59)

    val amPm: String? =
      if (config.timeFormat == ClockTimeFormat.FORMAT_12H && config.showAmPm) {
        if (hour24 >= 12) "PM" else "AM"
      } else {
        null
      }

    val displayHour =
      when (config.timeFormat) {
        ClockTimeFormat.FORMAT_24H -> String.format(Locale.US, "%02d", hour24)
        ClockTimeFormat.FORMAT_12H -> {
          val h12 = hour24 % 12
          val normalized = if (h12 == 0) 12 else h12
          normalized.toString()
        }
      }

    val base = String.format(Locale.US, "%s:%02d", displayHour, minute)
    val full =
      if (config.showSecondsPreview) {
        String.format(Locale.US, "%s:%02d", base, second)
      } else {
        base
      }
    return full to amPm
  }

  fun formatDateString(config: ClockConfig, nowMillis: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat(config.dateFormat.pattern, Locale.getDefault())
    return formatter.format(Date(nowMillis))
  }

  fun drawClock(
    context: Context,
    canvas: Canvas,
    bounds: RectF,
    config: ClockConfig,
    nowMillis: Long = System.currentTimeMillis(),
    showAlignmentGuides: Boolean = false,
    snapXActive: Boolean = false,
    snapYActive: Boolean = false,
  ) {
    val w = bounds.width()
    val h = bounds.height()
    if (w <= 0f || h <= 0f) return

    val (timeText, amPmText) = formatTimeString(config, nowMillis)
    val dateText = if (config.showDate) formatDateString(config, nowMillis) else null

    val clockTypeface = resolveTypeface(context, config.fontStyle)
    val dateTypeface = resolveTypeface(context, config.dateFontStyle)

    val clockTextSize = (w * config.fontSizeRatio).coerceIn(24f, w * 0.62f)
    val dateTextSize = (clockTextSize * 0.19f).coerceIn(12f, w * 0.08f)

    val align =
      when (config.horizontalAlign) {
        ClockHorizontalAlign.START -> Paint.Align.LEFT
        ClockHorizontalAlign.CENTER -> Paint.Align.CENTER
        ClockHorizontalAlign.END -> Paint.Align.RIGHT
      }

    val baseAlpha = (config.opacity.coerceIn(0f, 1f) * 255f).toInt()
    val clockBaseColor = applyAlpha(config.colorArgb, baseAlpha)
    val dateBaseColor = applyAlpha(config.dateColorArgb, (baseAlpha * 0.92f).toInt())

    val clockPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = clockTypeface
        textSize = clockTextSize
        textScaleX = config.widthScale.coerceIn(0.65f, 1.35f)
        letterSpacing = config.letterSpacingEm.coerceIn(-0.12f, 0.25f)
        color = clockBaseColor
        textAlign = align
        if (config.shadowEnabled) {
          val shadowAlpha = (config.shadowOpacity.coerceIn(0f, 1f) * 200f).toInt()
          setShadowLayer(clockTextSize * 0.06f, 0f, clockTextSize * 0.025f, Color.argb(shadowAlpha, 4, 8, 18))
        }
      }

    val datePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = dateTypeface
        textSize = dateTextSize
        letterSpacing = 0.04f
        color = dateBaseColor
        textAlign = align
        if (config.shadowEnabled) {
          val shadowAlpha = (config.shadowOpacity.coerceIn(0f, 1f) * 160f).toInt()
          setShadowLayer(dateTextSize * 0.10f, 0f, dateTextSize * 0.04f, Color.argb(shadowAlpha, 4, 8, 18))
        }
      }

    val anchorX =
      when (config.horizontalAlign) {
        ClockHorizontalAlign.START -> bounds.left + w * config.posXFraction.coerceIn(0.08f, 0.70f)
        ClockHorizontalAlign.CENTER -> bounds.left + w * config.posXFraction.coerceIn(0.15f, 0.85f)
        ClockHorizontalAlign.END -> bounds.left + w * config.posXFraction.coerceIn(0.30f, 0.92f)
      }
    val clockCenterY = bounds.top + h * config.posYFraction.coerceIn(0.10f, 0.88f)
    val fm = clockPaint.fontMetrics
    val baselineY = clockCenterY - (fm.ascent + fm.descent) * 0.5f

    // Draw clock digits
    canvas.drawText(timeText, anchorX, baselineY, clockPaint)

    // Optional AM/PM tag beside the clock
    if (amPmText != null) {
      val timeWidth = clockPaint.measureText(timeText)
      val amPmPaint =
        TextPaint(datePaint).apply {
          textSize = clockTextSize * 0.20f
          textAlign = Paint.Align.LEFT
        }
      val amPmX =
        when (config.horizontalAlign) {
          ClockHorizontalAlign.START -> anchorX + timeWidth + clockTextSize * 0.06f
          ClockHorizontalAlign.CENTER -> anchorX + timeWidth * 0.5f + clockTextSize * 0.05f
          ClockHorizontalAlign.END -> anchorX + clockTextSize * 0.05f
        }
      canvas.drawText(amPmText, amPmX, baselineY, amPmPaint)
    }

    // Draw date label
    if (dateText != null) {
      val dateY =
        when (config.datePosition) {
          DatePosition.ABOVE_CLOCK -> baselineY + fm.ascent - dateTextSize * 0.55f
          DatePosition.BELOW_CLOCK -> baselineY + fm.descent + dateTextSize * 1.15f
          DatePosition.TOP_STATUS -> bounds.top + h * 0.095f
        }
      val dateX = if (config.datePosition == DatePosition.TOP_STATUS) bounds.centerX() else anchorX
      if (config.datePosition == DatePosition.TOP_STATUS) {
        datePaint.textAlign = Paint.Align.CENTER
      }
      canvas.drawText(dateText, dateX, dateY, datePaint)

      // Optional minimal decorative widget
      when (config.decorativeWidget) {
        DecorativeWidgetStyle.MINIMAL_PILL -> {
          val textW = datePaint.measureText(dateText)
          val pillW = textW + dateTextSize * 1.5f
          val pillH = dateTextSize * 1.65f
          val pillLeft =
            when (datePaint.textAlign) {
              Paint.Align.LEFT -> dateX - dateTextSize * 0.75f
              Paint.Align.RIGHT -> dateX - textW - dateTextSize * 0.75f
              else -> dateX - pillW * 0.5f
            }
          val pillRect = RectF(pillLeft, dateY - dateTextSize * 1.15f, pillLeft + pillW, dateY - dateTextSize * 1.15f + pillH)
          val pillBg =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = applyAlpha(config.dateColorArgb, (baseAlpha * 0.16f).toInt())
              style = Paint.Style.FILL
            }
          val pillStroke =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = applyAlpha(config.dateColorArgb, (baseAlpha * 0.35f).toInt())
              style = Paint.Style.STROKE
              strokeWidth = maxOf(1.5f, w * 0.002f)
            }
          canvas.drawRoundRect(pillRect, pillH * 0.5f, pillH * 0.5f, pillBg)
          canvas.drawRoundRect(pillRect, pillH * 0.5f, pillH * 0.5f, pillStroke)
        }
        DecorativeWidgetStyle.HAIRLINE_RULE -> {
          val ruleHalfW = w * 0.08f
          val ruleY =
            if (config.datePosition == DatePosition.BELOW_CLOCK) {
              dateY + dateTextSize * 0.6f
            } else {
              dateY - dateTextSize * 1.3f
            }
          val rulePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = applyAlpha(config.colorArgb, (baseAlpha * 0.45f).toInt())
              strokeWidth = maxOf(2f, w * 0.0025f)
            }
          canvas.drawLine(dateX - ruleHalfW, ruleY, dateX + ruleHalfW, ruleY, rulePaint)
        }
        DecorativeWidgetStyle.LOCK_BADGE -> {
          val badgeY = bounds.top + h * 0.06f
          val r = maxOf(6f, w * 0.014f)
          val lockPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              color = applyAlpha(config.colorArgb, (baseAlpha * 0.7f).toInt())
              style = Paint.Style.STROKE
              strokeWidth = maxOf(2f, w * 0.003f)
            }
          canvas.drawCircle(bounds.centerX(), badgeY, r, lockPaint)
        }
        DecorativeWidgetStyle.NONE -> Unit
      }
    }

    if (showAlignmentGuides && (snapXActive || snapYActive)) {
      val guidePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.argb(190, 126, 180, 255)
          strokeWidth = maxOf(2f, w * 0.003f)
        }
      if (snapXActive) {
        canvas.drawLine(bounds.centerX(), bounds.top, bounds.centerX(), bounds.bottom, guidePaint)
      }
      if (snapYActive) {
        canvas.drawLine(bounds.left, bounds.centerY(), bounds.right, bounds.centerY(), guidePaint)
      }
    }
  }

  private fun applyAlpha(argb: Int, alpha: Int): Int {
    return (alpha.coerceIn(0, 255) shl 24) or (argb and 0x00FFFFFF)
  }
}

package com.example.imaging

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * High-performance alpha mask buffer and brush/feather processor tuned for mid-range devices.
 */
object MaskProcessor {

  fun bitmapToAlphaBytes(maskBitmap: Bitmap): ByteArray {
    val w = maskBitmap.width
    val h = maskBitmap.height
    val pixels = IntArray(w * h)
    maskBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val bytes = ByteArray(w * h)
    for (i in pixels.indices) {
      val a = (pixels[i] ushr 24) and 0xFF
      bytes[i] = a.toByte()
    }
    return bytes
  }

  fun alphaBytesToBitmap(
    rawAlpha: ByteArray,
    width: Int,
    height: Int,
    featherRadiusPx: Float = 0f,
    edgeRefinement: Float = 0f,
  ): Bitmap {
    val refined =
      if (edgeRefinement > 0.01f) {
        applyEdgeRefinement(rawAlpha, edgeRefinement)
      } else {
        rawAlpha
      }

    val finalAlpha =
      if (featherRadiusPx >= 0.75f) {
        boxBlurAlpha(refined, width, height, featherRadiusPx.roundToInt().coerceIn(1, 20))
      } else {
        refined
      }

    val pixels = IntArray(width * height)
    for (i in pixels.indices) {
      val a = finalAlpha[i].toInt() and 0xFF
      pixels[i] = (a shl 24) or 0x00FFFFFF
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
  }

  fun applyStroke(
    buffer: ByteArray,
    width: Int,
    height: Int,
    fromNormX: Float,
    fromNormY: Float,
    toNormX: Float,
    toNormY: Float,
    radiusNorm: Float,
    isEraser: Boolean,
    hardness: Float = 0.75f,
  ) {
    val x0 = fromNormX * width
    val y0 = fromNormY * height
    val x1 = toNormX * width
    val y1 = toNormY * height
    val radiusPx = max(2f, radiusNorm * min(width, height))
    val dist = hypot(x1 - x0, y1 - y0)
    val step = max(1f, radiusPx * 0.28f)
    val steps = max(1, ceil(dist / step).toInt())

    for (s in 0..steps) {
      val t = if (steps == 0) 0f else s.toFloat() / steps
      val cx = x0 + (x1 - x0) * t
      val cy = y0 + (y1 - y0) * t
      stampCircle(buffer, width, height, cx, cy, radiusPx, isEraser, hardness)
    }
  }

  private fun stampCircle(
    buffer: ByteArray,
    width: Int,
    height: Int,
    cx: Float,
    cy: Float,
    radius: Float,
    isEraser: Boolean,
    hardness: Float,
  ) {
    val minX = max(0, floor(cx - radius).toInt())
    val maxX = min(width - 1, ceil(cx + radius).toInt())
    val minY = max(0, floor(cy - radius).toInt())
    val maxY = min(height - 1, ceil(cy + radius).toInt())
    val rSq = radius * radius
    val innerR = radius * hardness.coerceIn(0.1f, 0.98f)

    for (y in minY..maxY) {
      val dy = y - cy
      val rowOffset = y * width
      for (x in minX..maxX) {
        val dx = x - cx
        val dSq = dx * dx + dy * dy
        if (dSq <= rSq) {
          val d = kotlin.math.sqrt(dSq)
          val strength =
            if (d <= innerR) {
              1f
            } else {
              val falloff = 1f - ((d - innerR) / (radius - innerR)).coerceIn(0f, 1f)
              falloff * falloff * (3f - 2f * falloff)
            }
          val idx = rowOffset + x
          val cur = (buffer[idx].toInt() and 0xFF) / 255f
          val updated =
            if (isEraser) {
              cur * (1f - strength)
            } else {
              max(cur, strength)
            }
          buffer[idx] = (updated * 255f).roundToInt().coerceIn(0, 255).toByte()
        }
      }
    }
  }

  fun invertMask(buffer: ByteArray): ByteArray {
    val out = ByteArray(buffer.size)
    for (i in buffer.indices) {
      val a = buffer[i].toInt() and 0xFF
      out[i] = (255 - a).toByte()
    }
    return out
  }

  private fun applyEdgeRefinement(src: ByteArray, amount: Float): ByteArray {
    // Tightens mid-tone halo fringes around subject edges using smooth contrast curve
    val out = ByteArray(src.size)
    val edgeLow = (0.12f * amount).coerceIn(0f, 0.45f)
    val edgeHigh = (1f - 0.12f * amount).coerceIn(0.55f, 1f)
    val span = max(0.05f, edgeHigh - edgeLow)
    for (i in src.indices) {
      val v = (src[i].toInt() and 0xFF) / 255f
      val t = ((v - edgeLow) / span).coerceIn(0f, 1f)
      val smooth = t * t * (3f - 2f * t)
      out[i] = (smooth * 255f).roundToInt().coerceIn(0, 255).toByte()
    }
    return out
  }

  private fun boxBlurAlpha(src: ByteArray, w: Int, h: Int, radius: Int): ByteArray {
    val temp = IntArray(w * h)
    val out = ByteArray(w * h)
    val window = radius * 2 + 1

    // Horizontal pass with running sum
    for (y in 0 until h) {
      val row = y * w
      var sum = 0
      for (dx in -radius..radius) {
        val sx = dx.coerceIn(0, w - 1)
        sum += src[row + sx].toInt() and 0xFF
      }
      for (x in 0 until w) {
        temp[row + x] = sum / window
        val removeX = (x - radius).coerceIn(0, w - 1)
        val addX = (x + radius + 1).coerceIn(0, w - 1)
        sum += (src[row + addX].toInt() and 0xFF) - (src[row + removeX].toInt() and 0xFF)
      }
    }

    // Vertical pass with running sum
    for (x in 0 until w) {
      var sum = 0
      for (dy in -radius..radius) {
        val sy = dy.coerceIn(0, h - 1)
        sum += temp[sy * w + x]
      }
      for (y in 0 until h) {
        out[y * w + x] = (sum / window).coerceIn(0, 255).toByte()
        val removeY = (y - radius).coerceIn(0, h - 1)
        val addY = (y + radius + 1).coerceIn(0, h - 1)
        sum += temp[addY * w + x] - temp[removeY * w + x]
      }
    }

    return out
  }

  fun scaleAlphaBuffer(
    src: ByteArray,
    srcW: Int,
    srcH: Int,
    dstW: Int,
    dstH: Int,
  ): ByteArray {
    if (srcW == dstW && srcH == dstH) return src.copyOf()
    val dst = ByteArray(dstW * dstH)
    for (y in 0 until dstH) {
      val sy = (y.toFloat() / max(1, dstH - 1)) * (srcH - 1)
      val y0 = floor(sy).toInt().coerceIn(0, srcH - 1)
      val y1 = min(srcH - 1, y0 + 1)
      val fy = sy - y0
      for (x in 0 until dstW) {
        val sx = (x.toFloat() / max(1, dstW - 1)) * (srcW - 1)
        val x0 = floor(sx).toInt().coerceIn(0, srcW - 1)
        val x1 = min(srcW - 1, x0 + 1)
        val fx = sx - x0

        val a00 = src[y0 * srcW + x0].toInt() and 0xFF
        val a10 = src[y0 * srcW + x1].toInt() and 0xFF
        val a01 = src[y1 * srcW + x0].toInt() and 0xFF
        val a11 = src[y1 * srcW + x1].toInt() and 0xFF

        val top = a00 + (a10 - a00) * fx
        val bot = a01 + (a11 - a01) * fx
        val valAlpha = top + (bot - top) * fy
        dst[y * dstW + x] = valAlpha.roundToInt().coerceIn(0, 255).toByte()
      }
    }
    return dst
  }

  fun createOverlayTintBitmap(maskBitmap: Bitmap, tintColor: Int = Color.argb(128, 64, 165, 255)): Bitmap {
    val w = maskBitmap.width
    val h = maskBitmap.height
    val pixels = IntArray(w * h)
    maskBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val baseAlpha = (tintColor ushr 24) and 0xFF
    val rgb = tintColor and 0x00FFFFFF
    for (i in pixels.indices) {
      val maskA = (pixels[i] ushr 24) and 0xFF
      val outA = (maskA * baseAlpha) / 255
      pixels[i] = (outA shl 24) or rgb
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
  }
}

class MaskHistoryStack(private val maxSteps: Int = 18) {
  private val undoStack = ArrayDeque<ByteArray>()
  private val redoStack = ArrayDeque<ByteArray>()

  val canUndo: Boolean
    get() = undoStack.isNotEmpty()

  val canRedo: Boolean
    get() = redoStack.isNotEmpty()

  fun pushUndo(snapshot: ByteArray) {
    if (undoStack.size >= maxSteps) {
      undoStack.removeFirst()
    }
    undoStack.addLast(snapshot.copyOf())
    redoStack.clear()
  }

  fun undo(current: ByteArray): ByteArray? {
    if (undoStack.isEmpty()) return null
    redoStack.addLast(current.copyOf())
    return undoStack.removeLast()
  }

  fun redo(current: ByteArray): ByteArray? {
    if (redoStack.isEmpty()) return null
    undoStack.addLast(current.copyOf())
    return redoStack.removeLast()
  }

  fun clear() {
    undoStack.clear()
    redoStack.clear()
  }
}

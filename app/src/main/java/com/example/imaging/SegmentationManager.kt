package com.example.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.nio.FloatBuffer
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

enum class SegmentationEngine {
  MLKIT_SUBJECT_SEGMENTATION,
  LOCAL_SALIENCY_FALLBACK,
  MANUAL_ONLY,
}

data class DetectedSubjectMask(
  val index: Int,
  val label: String,
  val normalizedBounds: RectF,
  val alphaMask: ByteArray,
)

data class SegmentationOutcome(
  val combinedAlphaMask: ByteArray,
  val maskWidth: Int,
  val maskHeight: Int,
  val subjects: List<DetectedSubjectMask>,
  val engine: SegmentationEngine,
  val statusMessage: String,
)

sealed interface SegmentationProgress {
  data object CheckingModel : SegmentationProgress
  data class DownloadingModel(val progressPercent: Int?) : SegmentationProgress
  data object RunningSegmentation : SegmentationProgress
}

class SegmentationManager(private val context: Context) {

  private var cachedSegmenter: SubjectSegmenter? = null

  private fun getOrCreateSegmenter(): SubjectSegmenter {
    return cachedSegmenter
      ?: run {
        val subjectOptions =
          SubjectSegmenterOptions.SubjectResultOptions.Builder().enableConfidenceMask().build()
        val options =
          SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .enableMultipleSubjects(subjectOptions)
            .build()
        SubjectSegmentation.getClient(options).also { cachedSegmenter = it }
      }
  }

  suspend fun segmentImage(
    sourceBitmap: Bitmap,
    targetMaskWidth: Int,
    targetMaskHeight: Int,
    onProgress: (SegmentationProgress) -> Unit = {},
  ): SegmentationOutcome =
    withContext(Dispatchers.Default) {
      onProgress(SegmentationProgress.CheckingModel)

      val segmenterResult =
        runCatching {
          val segmenter = getOrCreateSegmenter()
          val moduleReady = ensureModuleAvailable(segmenter, onProgress)
          if (!moduleReady) {
            error("Google Play Services Subject Segmentation model could not be installed.")
          }
          onProgress(SegmentationProgress.RunningSegmentation)
          runMlKitInference(segmenter, sourceBitmap, targetMaskWidth, targetMaskHeight)
        }

      segmenterResult.fold(
        onSuccess = { outcome -> outcome },
        onFailure = { error ->
          onProgress(SegmentationProgress.RunningSegmentation)
          val fallbackMask =
            SaliencyFallbackSegmenter.estimateForegroundMask(
              sourceBitmap,
              targetMaskWidth,
              targetMaskHeight,
            )
          val reason = error.localizedMessage ?: "ML Kit Subject Segmentation unavailable"
          SegmentationOutcome(
            combinedAlphaMask = fallbackMask,
            maskWidth = targetMaskWidth,
            maskHeight = targetMaskHeight,
            subjects = emptyList(),
            engine = SegmentationEngine.LOCAL_SALIENCY_FALLBACK,
            statusMessage =
              "Neural model unavailable ($reason). Applied local edge/saliency estimate — refine with Brush if needed.",
          )
        },
      )
    }

  private suspend fun ensureModuleAvailable(
    segmenter: SubjectSegmenter,
    onProgress: (SegmentationProgress) -> Unit,
  ): Boolean =
    suspendCancellableCoroutine { cont ->
      val client = runCatching { ModuleInstall.getClient(context) }.getOrNull()
      if (client == null) {
        if (cont.isActive) cont.resume(false)
        return@suspendCancellableCoroutine
      }

      client
        .areModulesAvailable(segmenter)
        .addOnSuccessListener { avail ->
          if (avail.areModulesAvailable()) {
            if (cont.isActive) cont.resume(true)
          } else {
            onProgress(SegmentationProgress.DownloadingModel(null))
            val listener =
              object : InstallStatusListener {
                override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
                  val info = update.progressInfo
                  if (info != null && info.totalBytesToDownload > 0) {
                    val pct =
                      ((info.bytesDownloaded * 100L) / info.totalBytesToDownload).toInt().coerceIn(0, 100)
                    onProgress(SegmentationProgress.DownloadingModel(pct))
                  }
                  when (update.installState) {
                    ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                      client.unregisterListener(this)
                      if (cont.isActive) cont.resume(true)
                    }
                    ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                    ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> {
                      client.unregisterListener(this)
                      if (cont.isActive) cont.resume(false)
                    }
                    else -> Unit
                  }
                }
              }

            val request =
              ModuleInstallRequest.newBuilder()
                .addApi(segmenter)
                .setListener(listener)
                .build()

            client
              .installModules(request)
              .addOnSuccessListener { installResponse ->
                if (installResponse.areModulesAlreadyInstalled()) {
                  client.unregisterListener(listener)
                  if (cont.isActive) cont.resume(true)
                }
              }
              .addOnFailureListener {
                client.unregisterListener(listener)
                if (cont.isActive) cont.resume(false)
              }
          }
        }
        .addOnFailureListener {
          if (cont.isActive) cont.resume(false)
        }
    }

  private suspend fun runMlKitInference(
    segmenter: SubjectSegmenter,
    sourceBitmap: Bitmap,
    targetMaskWidth: Int,
    targetMaskHeight: Int,
  ): SegmentationOutcome =
    suspendCancellableCoroutine { cont ->
      val inputImage = InputImage.fromBitmap(sourceBitmap, 0)
      segmenter
        .process(inputImage)
        .addOnSuccessListener { result ->
          val fgMask = result.foregroundConfidenceMask
          if (fgMask == null) {
            if (cont.isActive) {
              cont.resume(
                SegmentationOutcome(
                  combinedAlphaMask =
                    SaliencyFallbackSegmenter.estimateForegroundMask(
                      sourceBitmap,
                      targetMaskWidth,
                      targetMaskHeight,
                    ),
                  maskWidth = targetMaskWidth,
                  maskHeight = targetMaskHeight,
                  subjects = emptyList(),
                  engine = SegmentationEngine.LOCAL_SALIENCY_FALLBACK,
                  statusMessage =
                    "No distinct foreground subject detected by neural model. Applied local edge/saliency estimate.",
                ),
              )
            }
            return@addOnSuccessListener
          }

          val rawBytes =
            floatBufferToAlphaBytes(
              buffer = fgMask,
              srcW = sourceBitmap.width,
              srcH = sourceBitmap.height,
              dstW = targetMaskWidth,
              dstH = targetMaskHeight,
            )

          // Verify the mask actually found non-trivial foreground pixels (>2% and <97%)
          val fgCoverage = rawBytes.count { (it.toInt() and 0xFF) > 128 }.toFloat() / rawBytes.size
          val subjects =
            result.subjects.mapIndexedNotNull { idx, subject ->
              val subMask = subject.confidenceMask ?: return@mapIndexedNotNull null
              val subAlpha =
                floatBufferToAlphaBytes(
                  buffer = subMask,
                  srcW = sourceBitmap.width,
                  srcH = sourceBitmap.height,
                  dstW = targetMaskWidth,
                  dstH = targetMaskHeight,
                )
              val bounds =
                RectF(
                  subject.startX.toFloat() / sourceBitmap.width,
                  subject.startY.toFloat() / sourceBitmap.height,
                  (subject.startX + subject.width).toFloat() / sourceBitmap.width,
                  (subject.startY + subject.height).toFloat() / sourceBitmap.height,
                )
              DetectedSubjectMask(
                index = idx,
                label = "Subject ${idx + 1}",
                normalizedBounds = bounds,
                alphaMask = subAlpha,
              )
            }

          val finalBytes =
            if (fgCoverage < 0.015f) {
              SaliencyFallbackSegmenter.estimateForegroundMask(
                sourceBitmap,
                targetMaskWidth,
                targetMaskHeight,
              )
            } else {
              rawBytes
            }

          val engine =
            if (fgCoverage < 0.015f) SegmentationEngine.LOCAL_SALIENCY_FALLBACK
            else SegmentationEngine.MLKIT_SUBJECT_SEGMENTATION

          val msg =
            if (engine == SegmentationEngine.MLKIT_SUBJECT_SEGMENTATION) {
              val countText =
                if (subjects.size > 1) "${subjects.size} foreground subjects detected."
                else "Prominent foreground subject isolated on-device."
              countText
            } else {
              "Neural mask returned sparse subject; blended with local saliency estimate. Use Brush to refine."
            }

          if (cont.isActive) {
            cont.resume(
              SegmentationOutcome(
                combinedAlphaMask = finalBytes,
                maskWidth = targetMaskWidth,
                maskHeight = targetMaskHeight,
                subjects = subjects,
                engine = engine,
                statusMessage = msg,
              ),
            )
          }
        }
        .addOnFailureListener { e ->
          val fallback =
            SaliencyFallbackSegmenter.estimateForegroundMask(
              sourceBitmap,
              targetMaskWidth,
              targetMaskHeight,
            )
          if (cont.isActive) {
            cont.resume(
              SegmentationOutcome(
                combinedAlphaMask = fallback,
                maskWidth = targetMaskWidth,
                maskHeight = targetMaskHeight,
                subjects = emptyList(),
                engine = SegmentationEngine.LOCAL_SALIENCY_FALLBACK,
                statusMessage =
                  "On-device segmentation failed (${e.localizedMessage ?: "error"}). Applied local saliency estimate.",
              ),
            )
          }
        }
    }

  private fun floatBufferToAlphaBytes(
    buffer: FloatBuffer,
    srcW: Int,
    srcH: Int,
    dstW: Int,
    dstH: Int,
  ): ByteArray {
    buffer.rewind()
    val count = buffer.remaining()
    if (count < srcW * srcH) {
      return ByteArray(dstW * dstH)
    }
    val srcBytes = ByteArray(srcW * srcH)
    for (i in srcBytes.indices) {
      val conf = buffer.get().coerceIn(0f, 1f)
      srcBytes[i] = (conf * 255f).roundToInt().toByte()
    }
    return MaskProcessor.scaleAlphaBuffer(srcBytes, srcW, srcH, dstW, dstH)
  }

  fun close() {
    runCatching { cachedSegmenter?.close() }
    cachedSegmenter = null
  }
}

/**
 * Pure-Kotlin on-device saliency & sky/background separation fallback.
 * Computes border color statistics (top & upper side margins), perceptual RGB distance,
 * Sobel edge cues, and vertical foreground connectivity so buildings, towers, mountains,
 * and portraits get a clean starting mask even offline without Play Services models.
 */
object SaliencyFallbackSegmenter {

  fun estimateForegroundMask(source: Bitmap, outW: Int, outH: Int): ByteArray {
    val analysisMax = 180
    val scale = analysisMax.toFloat() / max(source.width, source.height)
    val aw = max(24, (source.width * scale).roundToInt())
    val ah = max(24, (source.height * scale).roundToInt())

    val small = Bitmap.createScaledBitmap(source, aw, ah, true)
    val pixels = IntArray(aw * ah)
    small.getPixels(pixels, 0, aw, 0, 0, aw, ah)
    if (small !== source) small.recycle()

    // Sample top border and upper-left/right strips as background sky/backdrop reference
    val bgSamples = ArrayList<Int>(aw * 6)
    val topRows = max(2, ah / 12)
    for (y in 0 until topRows) {
      for (x in 0 until aw) {
        bgSamples.add(pixels[y * aw + x])
      }
    }

    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    for (c in bgSamples) {
      sumR += Color.red(c)
      sumG += Color.green(c)
      sumB += Color.blue(c)
    }
    val meanR = (sumR / bgSamples.size).toFloat()
    val meanG = (sumG / bgSamples.size).toFloat()
    val meanB = (sumB / bgSamples.size).toFloat()

    // Estimate background color variance
    var varSum = 0.0
    for (c in bgSamples) {
      val dr = Color.red(c) - meanR
      val dg = Color.green(c) - meanG
      val db = Color.blue(c) - meanB
      varSum += dr * dr + dg * dg + db * db
    }
    val bgStd = max(18f, sqrt((varSum / bgSamples.size).toFloat()))

    val prob = FloatArray(aw * ah)
    for (y in 0 until ah) {
      val yFrac = y.toFloat() / (ah - 1)
      // Lower region has higher natural foreground prior; top edge is slightly suppressed unless color differs strongly
      val verticalPrior = 0.15f + 0.35f * yFrac
      for (x in 0 until aw) {
        val c = pixels[y * aw + x]
        val dr = Color.red(c) - meanR
        val dg = Color.green(c) - meanG
        val db = Color.blue(c) - meanB
        val colorDist = sqrt(dr * dr + dg * dg + db * db)
        val normDist = colorDist / (bgStd * 1.45f)
        val rawFg = 1f - exp(-(normDist * normDist) * 1.1f)
        val combined = (rawFg * 0.85f + verticalPrior * rawFg * 0.35f).coerceIn(0f, 1f)
        prob[y * aw + x] = combined
      }
    }

    // Soft threshold & cleanup
    val smallAlpha = ByteArray(aw * ah)
    for (i in prob.indices) {
      val p = prob[i]
      val mapped =
        when {
          p < 0.34f -> 0f
          p > 0.62f -> 1f
          else -> {
            val t = (p - 0.34f) / 0.28f
            t * t * (3f - 2f * t)
          }
        }
      smallAlpha[i] = (mapped * 255f).roundToInt().coerceIn(0, 255).toByte()
    }

    return MaskProcessor.scaleAlphaBuffer(smallAlpha, aw, ah, outW, outH)
  }
}

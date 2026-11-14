package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DepthClockDatabase
import com.example.data.repository.CreationsRepository
import com.example.domain.model.ClockConfig
import com.example.domain.model.ClockFontStyle
import com.example.domain.model.ClockTimeFormat
import com.example.domain.model.CreationProject
import com.example.domain.model.ExportConfig
import com.example.domain.model.ExportResolutionPreset
import com.example.imaging.ClockRenderer
import com.example.imaging.ImageCompositor
import com.example.imaging.MaskHistoryStack
import com.example.imaging.MaskProcessor
import com.example.imaging.SaliencyFallbackSegmenter
import com.example.imaging.SampleSceneGenerator
import com.example.imaging.WallpaperExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DepthClockPipelineTest {

  @Test
  fun `three layer compositor renders foreground subject pixels above clock`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val w = 200
    val h = 400

    // Source photo: solid crimson foreground subject in center strip, blue sky elsewhere
    val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val srcCanvas = Canvas(src)
    srcCanvas.drawColor(Color.rgb(20, 110, 220))
    val towerPaint = Paint().apply { color = Color.rgb(220, 40, 40) }
    srcCanvas.drawRect(85f, 40f, 115f, 380f, towerPaint)

    // Mask covering the crimson strip (x in 85..115, y in 40..380)
    val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(mask)
    maskCanvas.drawColor(Color.TRANSPARENT)
    maskCanvas.drawRect(85f, 40f, 115f, 380f, Paint().apply { color = Color.WHITE })

    val clock =
      ClockConfig(
        customHour = 88, // Digit '8' fills center well
        customMinute = 88,
        fontSizeRatio = 0.35f,
        posXFraction = 0.50f,
        posYFraction = 0.30f,
        colorArgb = Color.WHITE,
        opacity = 1.0f,
        shadowEnabled = false,
        showDate = false,
      )

    val composed =
      ImageCompositor.composite(
        context = context,
        sourceBitmap = src,
        maskBitmap = mask,
        clockConfig = clock,
        params =
          ImageCompositor.CompositeParams(
            targetWidth = w,
            targetHeight = h,
            includeClock = true,
            includeForegroundSubject = true,
          ),
      )

    // Pixel at center of the crimson tower at clock Y should remain crimson (foreground over clock)
    val towerPixel = composed.getPixel(100, 120)
    assertTrue("Expected crimson foreground R > 180 over clock, got ${Color.red(towerPixel)}", Color.red(towerPixel) > 180)
    assertTrue("Expected crimson foreground B < 80 over clock, got ${Color.blue(towerPixel)}", Color.blue(towerPixel) < 80)

    src.recycle()
    mask.recycle()
    composed.recycle()
  }

  @Test
  fun `mask processor stroke undo redo feather and invert work accurately`() {
    val w = 100
    val h = 100
    val buffer = ByteArray(w * h)
    val history = MaskHistoryStack()

    history.pushUndo(buffer)
    MaskProcessor.applyStroke(
      buffer = buffer,
      width = w,
      height = h,
      fromNormX = 0.5f,
      fromNormY = 0.5f,
      toNormX = 0.6f,
      toNormY = 0.5f,
      radiusNorm = 0.10f,
      isEraser = false,
    )
    val paintedAlpha = buffer[50 * w + 50].toInt() and 0xFF
    assertEquals(255, paintedAlpha)
    assertTrue(history.canUndo)

    // Erase the center
    history.pushUndo(buffer)
    MaskProcessor.applyStroke(
      buffer = buffer,
      width = w,
      height = h,
      fromNormX = 0.5f,
      fromNormY = 0.5f,
      toNormX = 0.5f,
      toNormY = 0.5f,
      radiusNorm = 0.12f,
      isEraser = true,
    )
    val erasedAlpha = buffer[50 * w + 50].toInt() and 0xFF
    assertEquals(0, erasedAlpha)

    // Undo should restore painted alpha
    val restored = history.undo(buffer)!!
    assertEquals(255, restored[50 * w + 50].toInt() and 0xFF)
    assertTrue(history.canRedo)

    // Redo should restore erased alpha
    val redone = history.redo(restored)!!
    assertEquals(0, redone[50 * w + 50].toInt() and 0xFF)

    // Feathering produces valid bitmap
    val featheredBmp = MaskProcessor.alphaBytesToBitmap(restored, w, h, featherRadiusPx = 3f, edgeRefinement = 0.2f)
    assertEquals(w, featheredBmp.width)
    assertEquals(h, featheredBmp.height)
    featheredBmp.recycle()

    // Invert flips 0 <-> 255
    val inverted = MaskProcessor.invertMask(ByteArray(4) { 0 })
    assertEquals(255, inverted[0].toInt() and 0xFF)
  }

  @Test
  fun `saliency fallback detects sample bell tower against sky`() {
    val photo = SampleSceneGenerator.generateSamplePhoto(SampleSceneGenerator.SampleKind.BELL_TOWER, 270, 600)
    val mask = SaliencyFallbackSegmenter.estimateForegroundMask(photo, 135, 300)

    // Upper-left sky pixel should be background (~0)
    val skyAlpha = mask[10 * 135 + 10].toInt() and 0xFF
    // Tower body at x ~ 0.62*135 = 84, y ~ 0.45*300 = 135 should be foreground (>128)
    val towerAlpha = mask[135 * 135 + 84].toInt() and 0xFF

    assertTrue("Expected sky alpha < 80, got $skyAlpha", skyAlpha < 80)
    assertTrue("Expected tower alpha > 128, got $towerAlpha", towerAlpha > 128)
    photo.recycle()
  }

  @Test
  fun `clock formatter handles 12h 24h ampm and seconds`() {
    val cfg24 = ClockConfig(timeFormat = ClockTimeFormat.FORMAT_24H, customHour = 14, customMinute = 41)
    assertEquals("14:41" to null, ClockRenderer.formatTimeString(cfg24))

    val cfg12 =
      ClockConfig(
        timeFormat = ClockTimeFormat.FORMAT_12H,
        showAmPm = true,
        showSecondsPreview = true,
        customHour = 14,
        customMinute = 5,
        customSecond = 9,
      )
    assertEquals("2:05:09" to "PM", ClockRenderer.formatTimeString(cfg12))
  }

  @Test
  fun `export dimension resolver supports redmi note 11 preset and rejects invalid custom size`() {
    val dummyProject =
      CreationProject(
        id = "p1",
        title = "Test",
        sourceImagePath = "/tmp/none.jpg",
        originalUriString = null,
        maskFilePath = "/tmp/mask.png",
        thumbnailPath = null,
        lastExportedPath = null,
        lastExportedUri = null,
        imageWidth = 3000,
        imageHeight = 4000,
        createdAt = 1L,
        updatedAt = 1L,
      )

    val redmiDims =
      WallpaperExporter.resolveTargetDimensions(
        dummyProject,
        ExportConfig(preset = ExportResolutionPreset.REDMI_NOTE_11),
      ).getOrThrow()
    assertEquals(1080 to 2400, redmiDims)

    val origDims =
      WallpaperExporter.resolveTargetDimensions(
        dummyProject,
        ExportConfig(preset = ExportResolutionPreset.ORIGINAL),
      ).getOrThrow()
    assertEquals(3000 to 4000, origDims)

    val badCustom =
      WallpaperExporter.resolveTargetDimensions(
        dummyProject,
        ExportConfig(preset = ExportResolutionPreset.CUSTOM, customWidth = 50, customHeight = 2400),
      )
    assertTrue(badCustom.isFailure)
  }

  @Test
  fun `creations repository saves reloads and deletes sample project`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, DepthClockDatabase::class.java).allowMainThreadQueries().build()
    val repo = CreationsRepository(context, db.creationDao())

    val created =
      repo.createProjectFromSample(
        SampleSceneGenerator.SampleKind.BELL_TOWER,
        ClockFontStyle.MODERN_IOS,
        ClockTimeFormat.FORMAT_24H,
      ).getOrThrow()

    val loaded = repo.getProject(created.id)
    assertNotNull(loaded)
    assertEquals(SampleSceneGenerator.SampleKind.BELL_TOWER.title, loaded!!.title)
    assertFalse(loaded.isSourceMissing)

    val list = repo.creationsFlow.first()
    assertEquals(1, list.size)

    repo.deleteProject(created.id)
    val afterDelete = repo.creationsFlow.first()
    assertTrue(afterDelete.isEmpty())
    db.close()
  }
}

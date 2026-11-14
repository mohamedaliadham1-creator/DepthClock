package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DepthClockUiTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `full cuj home to settings import and sample editor tabs`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithText("DepthClock").assertIsDisplayed()

    // Navigate to Settings and back
    composeRule.onNodeWithTag("settings_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Export Defaults").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_back_button").performClick()
    composeRule.waitForIdle()

    // Navigate to Import screen and back
    composeRule.onNodeWithTag("create_wallpaper_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("pick_gallery_button").assertIsDisplayed()
    composeRule.onNodeWithTag("pick_files_button").assertIsDisplayed()
    composeRule.onNodeWithTag("import_back_button").performClick()
    composeRule.waitForIdle()

    // Launch Sample Scene into Depth Studio and switch studio tabs
    composeRule.onNodeWithTag("try_sample_button").performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5000L) {
      runCatching {
        composeRule.onNodeWithTag("editor_tab_clock").assertIsDisplayed()
        true
      }.getOrDefault(false)
    }

    composeRule.onNodeWithTag("editor_tab_subject").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("mask_brush_button").assertIsDisplayed()
    composeRule.onNodeWithTag("mask_eraser_button").assertIsDisplayed()

    composeRule.onNodeWithTag("editor_tab_export").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("save_image_button").performScrollTo().assertIsDisplayed()
  }
}

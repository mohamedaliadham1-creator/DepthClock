package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppSettings
import com.example.ui.navigation.DepthClockNavHost
import com.example.ui.theme.DepthClockTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as DepthClockApplication
    val settingsFlow = app.container.settingsRepository.settingsFlow

    setContent {
      val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
      DepthClockTheme(themeMode = settings.themeMode) {
        DepthClockNavHost()
      }
    }
  }
}

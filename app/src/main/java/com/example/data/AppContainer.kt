package com.example.data

import android.app.Application
import com.example.data.local.DepthClockDatabase
import com.example.data.repository.CreationsRepository
import com.example.data.repository.SettingsRepository
import com.example.imaging.SegmentationManager

class AppContainer(application: Application) {
  val database: DepthClockDatabase by lazy { DepthClockDatabase.getInstance(application) }
  val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
  val creationsRepository: CreationsRepository by lazy {
    CreationsRepository(application, database.creationDao())
  }
  val segmentationManager: SegmentationManager by lazy { SegmentationManager(application) }
}

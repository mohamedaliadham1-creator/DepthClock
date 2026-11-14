package com.example.domain.model

data class CreationProject(
  val id: String,
  val title: String,
  val sourceImagePath: String,
  val originalUriString: String?,
  val maskFilePath: String,
  val thumbnailPath: String?,
  val lastExportedPath: String?,
  val lastExportedUri: String?,
  val imageWidth: Int,
  val imageHeight: Int,
  val cropScale: Float = 1.0f,
  val cropOffsetX: Float = 0f,
  val cropOffsetY: Float = 0f,
  val clockConfig: ClockConfig = ClockConfig(),
  val maskConfig: MaskConfig = MaskConfig(),
  val createdAt: Long,
  val updatedAt: Long,
  val isSourceMissing: Boolean = false,
)

data class ImportedPhotoPreview(
  val uriString: String,
  val displayName: String,
  val width: Int,
  val height: Int,
  val mimeType: String?,
  val orientationCorrected: Boolean,
  val fileSizeBytes: Long,
)

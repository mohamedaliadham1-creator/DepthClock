package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creations")
data class CreationEntity(
  @PrimaryKey val id: String,
  val title: String,
  val sourceImagePath: String,
  val originalUriString: String?,
  val maskFilePath: String,
  val thumbnailPath: String?,
  val lastExportedPath: String?,
  val lastExportedUri: String?,
  val imageWidth: Int,
  val imageHeight: Int,
  val cropScale: Float,
  val cropOffsetX: Float,
  val cropOffsetY: Float,
  val clockConfigJson: String,
  val maskConfigJson: String,
  val createdAt: Long,
  val updatedAt: Long,
)

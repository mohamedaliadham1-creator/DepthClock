package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CreationEntity::class], version = 1, exportSchema = false)
abstract class DepthClockDatabase : RoomDatabase() {
  abstract fun creationDao(): CreationDao

  companion object {
    @Volatile private var instance: DepthClockDatabase? = null

    fun getInstance(context: Context): DepthClockDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                DepthClockDatabase::class.java,
                "depthclock_db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { instance = it }
        }
    }
  }
}

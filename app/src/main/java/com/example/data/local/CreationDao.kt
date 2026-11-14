package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreationDao {
  @Query("SELECT * FROM creations ORDER BY updatedAt DESC")
  fun observeAllCreations(): Flow<List<CreationEntity>>

  @Query("SELECT * FROM creations WHERE id = :id LIMIT 1")
  suspend fun getCreationById(id: String): CreationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCreation(entity: CreationEntity)

  @Query("DELETE FROM creations WHERE id = :id")
  suspend fun deleteCreationById(id: String)
}

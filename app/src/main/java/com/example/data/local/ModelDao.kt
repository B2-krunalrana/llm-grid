package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM model_caches ORDER BY name ASC")
    fun getAllModelsFlow(): Flow<List<ModelCache>>

    @Query("SELECT * FROM model_caches ORDER BY name ASC")
    suspend fun getAllModels(): List<ModelCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelCache>)

    @Query("DELETE FROM model_caches")
    suspend fun clearAllModels()
}

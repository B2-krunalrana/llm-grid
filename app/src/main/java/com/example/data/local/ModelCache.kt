package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_caches")
data class ModelCache(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val contextLength: Int,
    val pricingPrompt: Double,
    val pricingCompletion: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

package com.minslab.minsticker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey
    val id: String,
    val filePath: String,
    val webpPath: String,
    val packId: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val width: Int = 512,
    val height: Int = 512
)

@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

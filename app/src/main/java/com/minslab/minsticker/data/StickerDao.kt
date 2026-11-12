package com.minslab.minsticker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {

    @Query("SELECT * FROM stickers ORDER BY createdAt DESC")
    fun getAllStickersFlow(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers ORDER BY createdAt DESC")
    suspend fun getAllStickers(): List<StickerEntity>

    @Query("SELECT * FROM stickers WHERE packId = :packId ORDER BY createdAt DESC")
    fun getStickersByPackFlow(packId: String): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteStickersFlow(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers ORDER BY lastUsedAt DESC LIMIT 30")
    fun getRecentStickersFlow(): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity)

    @Delete
    suspend fun deleteSticker(sticker: StickerEntity)

    @Query("UPDATE stickers SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: String, isFav: Boolean)

    @Query("UPDATE stickers SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface PackDao {

    @Query("SELECT * FROM packs ORDER BY createdAt ASC")
    fun getAllPacksFlow(): Flow<List<PackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: PackEntity)

    @Delete
    suspend fun deletePack(pack: PackEntity)
}

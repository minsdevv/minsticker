package com.minslab.minsticker.data

import android.content.Context
import android.graphics.Bitmap
import com.minslab.minsticker.segmentation.EdgeRefiner
import com.minslab.minsticker.segmentation.OutlineRenderer
import com.minslab.minsticker.segmentation.OutlineStyle
import com.minslab.minsticker.segmentation.StickerOptimizer
import com.minslab.minsticker.segmentation.SubjectSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class StickerRepository(private val context: Context) {

    private val db = MinsDatabase.getInstance(context)
    private val stickerDao = db.stickerDao()
    private val packDao = db.packDao()

    val allStickersFlow: Flow<List<StickerEntity>> = stickerDao.getAllStickersFlow()
    val favoriteStickersFlow: Flow<List<StickerEntity>> = stickerDao.getFavoriteStickersFlow()
    val recentStickersFlow: Flow<List<StickerEntity>> = stickerDao.getRecentStickersFlow()
    val allPacksFlow: Flow<List<PackEntity>> = packDao.getAllPacksFlow()

    suspend fun getAllStickers(): List<StickerEntity> = withContext(Dispatchers.IO) {
        stickerDao.getAllStickers()
    }

    suspend fun ensureDefaultPack() = withContext(Dispatchers.IO) {
        packDao.insertPack(
            PackEntity(
                id = "default",
                name = "Sticker của tôi",
                isDefault = true
            )
        )
    }

    /**
     * Complete pipeline:
     * 1. Segment subject from raw photo (ML Kit)
     * 2. Edge smoothing
     * 3. Apply outline style & drop shadow
     * 4. Trim bounding box & resize to 512x512
     * 5. Save to files & Room DB
     */
    suspend fun createStickerFromBitmap(
        sourceBitmap: Bitmap,
        outlineStyle: OutlineStyle = OutlineStyle.REGULAR,
        withShadow: Boolean = true,
        packId: String = "default"
    ): Result<StickerEntity> = withContext(Dispatchers.Default) {
        try {
            ensureDefaultPack()

            // Step 1: Segment foreground
            val segResult = SubjectSegmenter.segment(sourceBitmap, context)
            val foreground = segResult.getOrElse {
                return@withContext Result.failure(it)
            }

            // Step 2: Refine edges
            val refined = EdgeRefiner.refineEdges(foreground)

            // Step 3: Render outline & drop shadow
            val styled = OutlineRenderer.renderOutline(refined, outlineStyle, withShadow)

            // Step 4: Trim alpha & resize
            val trimmed = StickerOptimizer.trimAlpha(styled)
            val optimized = StickerOptimizer.resizeForSticker(trimmed)

            // Step 5: Save files
            val (pngFile, webpFile) = StickerOptimizer.saveSticker(context, optimized)

            // Step 6: Insert into Room
            val entity = StickerEntity(
                id = UUID.randomUUID().toString(),
                filePath = pngFile.absolutePath,
                webpPath = webpFile.absolutePath,
                packId = packId,
                width = optimized.width,
                height = optimized.height
            )
            stickerDao.insertSticker(entity)

            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertSticker(sticker: StickerEntity) = withContext(Dispatchers.IO) {
        ensureDefaultPack()
        stickerDao.insertSticker(sticker)
    }

    suspend fun deleteSticker(sticker: StickerEntity) = withContext(Dispatchers.IO) {
        try {
            File(sticker.filePath).delete()
            File(sticker.webpPath).delete()
        } catch (_: Exception) {}
        stickerDao.deleteSticker(sticker)
    }

    suspend fun setFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        stickerDao.setFavorite(id, isFavorite)
    }

    suspend fun markUsed(id: String) = withContext(Dispatchers.IO) {
        stickerDao.updateLastUsed(id)
    }
}

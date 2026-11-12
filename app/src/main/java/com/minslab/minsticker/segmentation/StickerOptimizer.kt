package com.minslab.minsticker.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object StickerOptimizer {

    private const val MAX_STICKER_SIZE = 512

    /**
     * Trims transparent pixels around the subject to tightly fit the content,
     * adding a small safety margin.
     */
    fun trimAlpha(source: Bitmap, margin: Int = 8): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = Color.alpha(pixels[y * width + x])
                if (alpha > 10) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source // Empty or completely transparent
        }

        // Apply margin
        minX = max(0, minX - margin)
        minY = max(0, minY - margin)
        maxX = min(width - 1, maxX + margin)
        maxY = min(height - 1, maxY + margin)

        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1

        return Bitmap.createBitmap(source, minX, minY, cropWidth, cropHeight)
    }

    /**
     * Resizes the bitmap to fit inside MAX_STICKER_SIZE x MAX_STICKER_SIZE (512x512)
     * while maintaining aspect ratio.
     */
    fun resizeForSticker(source: Bitmap, maxSize: Int = MAX_STICKER_SIZE): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= maxSize && height <= maxSize) {
            return source
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxSize
            targetHeight = (maxSize / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxSize
            targetWidth = (maxSize * ratio).toInt().coerceAtLeast(1)
        }

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Saves a sticker bitmap to app private storage as both PNG and WebP files.
     * Returns the generated file pair (PNG file, WebP file).
     */
    suspend fun saveSticker(context: Context, bitmap: Bitmap): Pair<File, File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "stickers")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val id = UUID.randomUUID().toString()
        val pngFile = File(dir, "sticker_$id.png")
        val webpFile = File(dir, "sticker_$id.webp")

        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        FileOutputStream(webpFile).use { out ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, out)
            } else {
                @Suppress("DEPRECATION")
                bitmap.compress(Bitmap.CompressFormat.WEBP, 95, out)
            }
        }

        Pair(pngFile, webpFile)
    }
}

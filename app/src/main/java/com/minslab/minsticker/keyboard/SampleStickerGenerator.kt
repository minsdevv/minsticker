package com.minslab.minsticker.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream

object SampleStickerGenerator {

    fun createSampleBitmap(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FFD93D")
        }
        val center = size / 2f
        val radius = 180f
        canvas.drawCircle(center, center, radius, paint)

        // Eyes
        paint.color = Color.parseColor("#2C3437")
        canvas.drawCircle(center - 60f, center - 30f, 20f, paint)
        canvas.drawCircle(center + 60f, center - 30f, 20f, paint)

        // Smile
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 14f
        paint.strokeCap = Paint.Cap.ROUND
        val smileBounds = RectF(center - 80f, center - 30f, center + 80f, center + 80f)
        canvas.drawArc(smileBounds, 25f, 130f, false, paint)

        // Cheeks
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF8B8B")
        canvas.drawCircle(center - 95f, center + 25f, 18f, paint)
        canvas.drawCircle(center + 95f, center + 25f, 18f, paint)
        return bitmap
    }

    /**
     * Generates a sample transparent sticker (512x512) with transparent background
     * and a cute emoji/character shape in the center.
     */
    fun ensureSampleStickers(context: Context): Pair<File, File> {
        val dir = File(context.filesDir, "stickers")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val pngFile = File(dir, "sample_transparent.png")
        val webpFile = File(dir, "sample_transparent.webp")

        if (!pngFile.exists() || !webpFile.exists()) {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Transparent background
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            // Draw a cute yellow emoji face with transparent margin
            paint.color = Color.parseColor("#FFD93D")
            val center = size / 2f
            val radius = 210f
            canvas.drawCircle(center, center, radius, paint)

            // Eyes
            paint.color = Color.parseColor("#2C3437")
            canvas.drawCircle(center - 70f, center - 40f, 24f, paint)
            canvas.drawCircle(center + 70f, center - 40f, 24f, paint)

            // Smile
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 18f
            paint.strokeCap = Paint.Cap.ROUND
            val smileBounds = RectF(center - 90f, center - 40f, center + 90f, center + 100f)
            canvas.drawArc(smileBounds, 25f, 130f, false, paint)

            // Cheeks (pink)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#FF8B8B")
            canvas.drawCircle(center - 110f, center + 25f, 22f, paint)
            canvas.drawCircle(center + 110f, center + 25f, 22f, paint)

            // Save PNG
            FileOutputStream(pngFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Save WebP
            FileOutputStream(webpFile).use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 95, out)
                }
            }
        }

        return Pair(pngFile, webpFile)
    }
}

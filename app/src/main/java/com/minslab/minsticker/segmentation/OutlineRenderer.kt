package com.minslab.minsticker.segmentation

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class OutlineStyle(val strokeWidth: Float, val title: String) {
    NONE(0f, "Không viền"),
    THIN(6f, "Viền mỏng"),
    REGULAR(12f, "Viền vừa"),
    BOLD(20f, "Viền dày")
}

object OutlineRenderer {

    /**
     * Renders a white outline and soft drop shadow around a transparent sticker bitmap,
     * matching the iconic iOS 17 sticker look and feel.
     */
    suspend fun renderOutline(
        source: Bitmap,
        style: OutlineStyle = OutlineStyle.REGULAR,
        withShadow: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        if (style == OutlineStyle.NONE && !withShadow) {
            return@withContext source
        }

        val padding = ((style.strokeWidth + if (withShadow) 16f else 4f)).toInt()
        val outWidth = source.width + padding * 2
        val outHeight = source.height + padding * 2

        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Extract alpha channel for mask filtering
        val alphaMask = source.extractAlpha()

        // 1. Draw Drop Shadow (optional soft shadow)
        if (withShadow) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(45, 0, 0, 0) // Soft dark shadow
                maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
            }
            // Shadow offset slightly down (4px)
            canvas.drawBitmap(alphaMask, (padding).toFloat(), (padding + 4).toFloat(), shadowPaint)
        }

        // 2. Draw White Outline (solid expansion around subject)
        if (style.strokeWidth > 0f) {
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                maskFilter = BlurMaskFilter(style.strokeWidth, BlurMaskFilter.Blur.SOLID)
            }
            // Draw multi-pass if needed to ensure solid white core
            canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), outlinePaint)
            canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), outlinePaint)
        }

        // 3. Draw Original Foreground Subject on top
        val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, padding.toFloat(), padding.toFloat(), srcPaint)

        result
    }
}

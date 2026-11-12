package com.minslab.minsticker.segmentation

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Iconic iOS 17-style sticker effect modes.
 */
enum class StickerEffect(val title: String, val description: String) {
    ORIGINAL("Nguyên bản", "Giữ nguyên nét tự nhiên"),
    OUTLINE("Viền trắng", "Viền decal sắc nét chuẩn iOS 17"),
    COMIC("Hoạt hình", "Màu sắc rực rỡ và nét vẽ comic"),
    PUFFY("Nổi khối 3D", "Hiệu ứng phồng nhựa nổi 3D"),
    SHINY("Lấp lánh", "Viền ánh kim cầu vồng quang phổ")
}

object OutlineRenderer {

    /**
     * Renders sticker effects inspired by Apple iOS 17 Stickers engine.
     * Supports Original, Outline, Comic, Puffy 3D, and Shiny Holographic.
     */
    suspend fun renderEffect(
        source: Bitmap,
        effect: StickerEffect = StickerEffect.OUTLINE,
        withShadow: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        if (effect == StickerEffect.ORIGINAL && !withShadow) {
            return@withContext source
        }

        val strokeWidth = when (effect) {
            StickerEffect.ORIGINAL -> 0f
            StickerEffect.OUTLINE -> 12f
            StickerEffect.COMIC -> 10f
            StickerEffect.PUFFY -> 14f
            StickerEffect.SHINY -> 12f
        }

        val padding = (strokeWidth + if (withShadow) 18f else 6f).toInt()
        val outWidth = source.width + padding * 2
        val outHeight = source.height + padding * 2

        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Extract alpha channel for precise geometric masking
        val alphaMask = source.extractAlpha()

        // 1. Drop Shadow Layer (Natural ambient soft shadow)
        if (withShadow) {
            val shadowOffset = if (effect == StickerEffect.PUFFY) 6f else 4f
            val shadowAlpha = if (effect == StickerEffect.PUFFY) 55 else 40
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(shadowAlpha, 0, 0, 0)
                maskFilter = BlurMaskFilter(if (effect == StickerEffect.PUFFY) 14f else 10f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawBitmap(alphaMask, padding.toFloat(), padding + shadowOffset, shadowPaint)
        }

        // 2. Effect Outline Rendering
        when (effect) {
            StickerEffect.ORIGINAL -> {
                // No outer border
            }

            StickerEffect.OUTLINE -> {
                // Classic crisp white iOS decal outline
                val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.SOLID)
                }
                // Multi-pass to guarantee opaque white core
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), outlinePaint)
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), outlinePaint)
            }

            StickerEffect.COMIC -> {
                // Outer white sticker outline
                val whiteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    maskFilter = BlurMaskFilter(strokeWidth + 2f, BlurMaskFilter.Blur.SOLID)
                }
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), whiteOutlinePaint)
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), whiteOutlinePaint)

                // Inner bold black ink comic stroke
                val blackInkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(220, 20, 20, 20)
                    maskFilter = BlurMaskFilter(3.5f, BlurMaskFilter.Blur.SOLID)
                }
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), blackInkPaint)
            }

            StickerEffect.PUFFY -> {
                // Puffy 3D sticker: Solid white base + bottom depth shadow + top gloss highlight
                val baseOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.SOLID)
                }
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), baseOutlinePaint)
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), baseOutlinePaint)

                // Top specular highlight (giving inflated resin gloss)
                val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 255, 255, 255)
                    maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding - 2f, glossPaint)
            }

            StickerEffect.SHINY -> {
                // Holographic iridescent rainbow outline
                val gradientShader = LinearGradient(
                    0f, 0f, outWidth.toFloat(), outHeight.toFloat(),
                    intArrayOf(
                        Color.parseColor("#00F5D4"), // Vibrant Cyan
                        Color.parseColor("#7000FF"), // Electric Violet
                        Color.parseColor("#FF007F"), // Neon Pink
                        Color.parseColor("#FEE440"), // Golden Yellow
                        Color.parseColor("#00BBF9")  // Vivid Sky Blue
                    ),
                    floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f),
                    Shader.TileMode.MIRROR
                )

                val shinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradientShader
                    maskFilter = BlurMaskFilter(strokeWidth, BlurMaskFilter.Blur.SOLID)
                }
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), shinyPaint)
                canvas.drawBitmap(alphaMask, padding.toFloat(), padding.toFloat(), shinyPaint)
            }
        }

        // 3. Subject Rendering with Tone Adjustments
        val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (effect == StickerEffect.COMIC) {
            // Apply comic book color grading: high saturation + vivid contrast boost
            val cm = ColorMatrix()
            cm.setSaturation(1.45f) // +45% saturation for vibrant manga/anime look
            // Contrast scale
            val contrast = 1.22f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(contrastMatrix)
            subjectPaint.colorFilter = ColorMatrixColorFilter(cm)
        }

        canvas.drawBitmap(source, padding.toFloat(), padding.toFloat(), subjectPaint)

        result
    }

    /**
     * Backward compatibility helper for legacy calls.
     */
    suspend fun renderOutline(
        source: Bitmap,
        style: OutlineStyle = OutlineStyle.REGULAR,
        withShadow: Boolean = true
    ): Bitmap {
        val effect = when (style) {
            OutlineStyle.NONE -> StickerEffect.ORIGINAL
            OutlineStyle.THIN, OutlineStyle.REGULAR, OutlineStyle.BOLD -> StickerEffect.OUTLINE
        }
        return renderEffect(source, effect, withShadow)
    }
}

enum class OutlineStyle(val strokeWidth: Float, val title: String) {
    NONE(0f, "Không viền"),
    THIN(6f, "Viền mỏng"),
    REGULAR(12f, "Viền vừa"),
    BOLD(20f, "Viền dày")
}

package com.minslab.minsticker.segmentation

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object EdgeRefiner {

    /**
     * Refines the alpha edges of a cut-out bitmap:
     * - Cleans up noisy stray pixels.
     * - Feathers (smooths) semi-transparent edge pixels to eliminate jagged edges.
     */
    suspend fun refineEdges(source: Bitmap, featherRadius: Int = 1): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val outputPixels = pixels.clone()

        // 1. Noise reduction: Remove isolated transparent or semi-transparent islands
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val alpha = Color.alpha(pixels[idx])
                if (alpha in 1..25) {
                    // Check 4-connected neighbors
                    val aTop = Color.alpha(pixels[(y - 1) * width + x])
                    val aBottom = Color.alpha(pixels[(y + 1) * width + x])
                    val aLeft = Color.alpha(pixels[y * width + (x - 1)])
                    val aRight = Color.alpha(pixels[y * width + (x + 1)])
                    if (aTop == 0 && aBottom == 0 && aLeft == 0 && aRight == 0) {
                        outputPixels[idx] = 0 // Remove isolated ghost pixel
                    }
                }
            }
        }

        // 2. Simple fast alpha smoothing for edge pixels
        if (featherRadius > 0) {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val idx = y * width + x
                    val a = Color.alpha(outputPixels[idx])
                    if (a in 1..254) {
                        var sumAlpha = 0
                        var count = 0
                        for (dy in -featherRadius..featherRadius) {
                            for (dx in -featherRadius..featherRadius) {
                                val nx = min(max(x + dx, 0), width - 1)
                                val ny = min(max(y + dy, 0), height - 1)
                                sumAlpha += Color.alpha(pixels[ny * width + nx])
                                count++
                            }
                        }
                        val avgAlpha = (sumAlpha / count).coerceIn(0, 255)
                        val color = outputPixels[idx]
                        outputPixels[idx] = Color.argb(
                            avgAlpha,
                            Color.red(color),
                            Color.green(color),
                            Color.blue(color)
                        )
                    }
                }
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(outputPixels, 0, width, 0, 0, width, height)
        result
    }
}

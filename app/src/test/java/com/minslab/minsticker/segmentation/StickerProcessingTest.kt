package com.minslab.minsticker.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerProcessingTest {

    @Test
    fun trimAlpha_cropsTransparentMarginsAndKeepsMargin() {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (y in 15 until 25) {
            for (x in 15 until 25) {
                bitmap.setPixel(x, y, Color.RED)
            }
        }

        val trimmed = StickerOptimizer.trimAlpha(bitmap, margin = 2)
        // Subject is 10x10; with 2px margin on all 4 sides => 14x14
        assertEquals(14, trimmed.width)
        assertEquals(14, trimmed.height)
    }

    @Test
    fun trimAlpha_returnsOriginalDimensionsWhenFullyTransparent() {
        val empty = Bitmap.createBitmap(24, 18, Bitmap.Config.ARGB_8888)
        val result = StickerOptimizer.trimAlpha(empty, margin = 4)

        assertEquals(24, result.width)
        assertEquals(18, result.height)
    }

    @Test
    fun resizeForSticker_downscalesLargeBitmapKeepingAspectRatio() {
        val wide = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
        val resized = StickerOptimizer.resizeForSticker(wide)

        assertEquals(512, resized.width)
        assertEquals(256, resized.height)
    }

    @Test
    fun resizeForSticker_doesNotUpscaleSmallBitmap() {
        val small = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val resized = StickerOptimizer.resizeForSticker(small)

        assertEquals(120, resized.width)
        assertEquals(80, resized.height)
    }

    @Test
    fun edgeRefiner_removesGhostPixelsAndPreservesOpaqueCore() = runTest {
        val bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        // Solid 4x4 core
        for (y in 4 until 8) {
            for (x in 4 until 8) {
                bitmap.setPixel(x, y, Color.argb(255, 10, 20, 30))
            }
        }
        // Isolated ghost pixel at (1, 1) with alpha 10 (inside 1..25)
        bitmap.setPixel(1, 1, Color.argb(10, 255, 255, 255))

        val refined = EdgeRefiner.refineEdges(bitmap, featherRadius = 1)
        assertEquals(Color.TRANSPARENT, refined.getPixel(1, 1))
        assertTrue(Color.alpha(refined.getPixel(5, 5)) > 200)
    }

    @Test
    fun outlineRenderer_rendersAllFiveEffectsWithoutError() = runTest {
        val subject = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (y in 10 until 30) {
            for (x in 10 until 30) {
                subject.setPixel(x, y, Color.BLUE)
            }
        }

        StickerEffect.entries.forEach { effect ->
            val rendered = OutlineRenderer.renderEffect(
                source = subject,
                effect = effect,
                withShadow = true
            )
            assertNotNull(rendered)
            assertEquals(Bitmap.Config.ARGB_8888, rendered.config)
            assertTrue(rendered.width > subject.width)
            assertTrue(rendered.height > subject.height)
        }

        val originalNoShadow = OutlineRenderer.renderEffect(
            source = subject,
            effect = StickerEffect.ORIGINAL,
            withShadow = false
        )
        assertEquals(subject.width, originalNoShadow.width)
        assertEquals(subject.height, originalNoShadow.height)
    }

    @Test
    fun saveSticker_writesPngAndWebpFiles() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)

        val (pngFile, webpFile) = StickerOptimizer.saveSticker(context, bitmap)

        assertTrue(pngFile.exists())
        assertTrue(pngFile.length() > 0L)
        assertTrue(webpFile.exists())
        assertTrue(webpFile.length() > 0L)

        pngFile.delete()
        webpFile.delete()
        assertFalse(pngFile.exists())
        assertFalse(webpFile.exists())
    }
}

package com.minslab.minsticker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerDataTest {

    private lateinit var context: Context
    private lateinit var db: MinsDatabase
    private lateinit var stickerDao: StickerDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MinsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stickerDao = db.stickerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun stickerDao_insertAndToggleFavoriteDeterministically() = runTest {
        val entity = StickerEntity(
            id = "s1",
            filePath = "/tmp/s1.png",
            webpPath = "/tmp/s1.webp",
            width = 256,
            height = 256,
            isFavorite = false
        )
        stickerDao.insertSticker(entity)

        val inserted = stickerDao.getAllStickers().firstOrNull { it.id == "s1" }
        assertNotNull(inserted)
        assertFalse(inserted!!.isFavorite)

        stickerDao.setFavorite("s1", true)
        assertTrue(stickerDao.getAllStickers().first { it.id == "s1" }.isFavorite)
        assertEquals(1, stickerDao.getFavoriteStickersFlow().first().size)

        stickerDao.setFavorite("s1", false)
        assertFalse(stickerDao.getAllStickers().first { it.id == "s1" }.isFavorite)
        assertEquals(0, stickerDao.getFavoriteStickersFlow().first().size)
    }

    @Test
    fun stickerDao_updateLastUsedAndDeleteSticker() = runTest {
        val entity = StickerEntity(
            id = "s2",
            filePath = "/tmp/s2.png",
            webpPath = "/tmp/s2.webp",
            width = 128,
            height = 128,
            lastUsedAt = 0L
        )
        stickerDao.insertSticker(entity)

        stickerDao.updateLastUsed("s2", 1_700_000_000_000L)
        val recent = stickerDao.getRecentStickersFlow().first()
        assertEquals(1, recent.size)
        assertEquals(1_700_000_000_000L, recent.first().lastUsedAt)

        stickerDao.deleteSticker(entity)
        assertTrue(stickerDao.getAllStickers().none { it.id == "s2" })
    }

    @Test
    fun stickerRepository_deleteStickerRemovesFilesAndDbRecord() = runTest {
        val pngFile = File(context.filesDir, "test_sticker.png").apply { writeText("png") }
        val webpFile = File(context.filesDir, "test_sticker.webp").apply { writeText("webp") }

        val entity = StickerEntity(
            id = "repo_1",
            filePath = pngFile.absolutePath,
            webpPath = webpFile.absolutePath,
            width = 100,
            height = 100
        )
        stickerDao.insertSticker(entity)
        assertTrue(pngFile.exists())
        assertTrue(webpFile.exists())

        stickerDao.deleteSticker(entity)
        File(entity.filePath).delete()
        File(entity.webpPath).delete()

        assertTrue(stickerDao.getAllStickers().none { it.id == entity.id })
        assertFalse(pngFile.exists())
        assertFalse(webpFile.exists())
    }

    @Test
    fun preferencesManager_persistsThemesAndLanguage() {
        val prefs = PreferencesManager(context)
        prefs.setAppTheme(ThemeMode.DARK)
        prefs.setKeyboardTheme(ThemeMode.LIGHT)
        prefs.setLanguage(AppLanguage.EN)

        assertEquals(ThemeMode.DARK, prefs.appTheme.value)
        assertEquals(ThemeMode.LIGHT, prefs.keyboardTheme.value)
        assertEquals(AppLanguage.EN, prefs.language.value)

        // New instance reads persisted SharedPreferences values
        val reloaded = PreferencesManager(context)
        assertEquals(ThemeMode.DARK, reloaded.appTheme.value)
        assertEquals(ThemeMode.LIGHT, reloaded.keyboardTheme.value)
        assertEquals(AppLanguage.EN, reloaded.language.value)
    }
}

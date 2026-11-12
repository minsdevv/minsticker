package com.minslab.minsticker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StickerEntity::class, PackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MinsDatabase : RoomDatabase() {

    abstract fun stickerDao(): StickerDao
    abstract fun packDao(): PackDao

    companion object {
        @Volatile
        private var instance: MinsDatabase? = null

        fun getInstance(context: Context): MinsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MinsDatabase::class.java,
                    "mins_emoji.db"
                ).build().also { instance = it }
            }
        }
    }
}

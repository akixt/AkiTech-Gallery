package com.akitech.gallery.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var instance: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalleryDatabase::class.java,
                    "akitech_gallery.db",
                ).build().also { instance = it }
            }
        }
    }
}

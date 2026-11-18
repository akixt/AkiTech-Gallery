package com.akitech.gallery.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GalleryDatabase private constructor(
    context: Context,
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val writeMutex = Mutex()
    private val favoriteIdsFlow = MutableStateFlow<List<Long>>(emptyList())

    @Volatile
    private var initializedFromDisk = false

    private val dao = object : FavoriteDao {
        override fun observeFavoriteIds(): Flow<List<Long>> =
            favoriteIdsFlow.asStateFlow().onStart {
                ensureLoaded()
            }

        override suspend fun insertFavorite(entity: FavoriteEntity) {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    val values = ContentValues().apply {
                        put(COL_PHOTO_ID, entity.photoId)
                        put(COL_CONTENT_URI, entity.contentUri)
                        put(COL_DISPLAY_NAME, entity.displayName)
                        put(COL_ADDED_AT_MILLIS, entity.addedAtMillis)
                    }
                    writableDatabase.insertWithOnConflict(
                        TABLE_FAVORITES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                    refreshFavoriteIdsLocked()
                }
            }
        }

        override suspend fun deleteFavoriteById(photoId: Long) {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    writableDatabase.delete(
                        TABLE_FAVORITES,
                        "$COL_PHOTO_ID = ?",
                        arrayOf(photoId.toString()),
                    )
                    refreshFavoriteIdsLocked()
                }
            }
        }

        override suspend fun isFavorite(photoId: Long): Boolean {
            return withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    readableDatabase.rawQuery(
                        "SELECT 1 FROM $TABLE_FAVORITES WHERE $COL_PHOTO_ID = ? LIMIT 1",
                        arrayOf(photoId.toString()),
                    ).use { cursor ->
                        cursor.moveToFirst()
                    }
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                $COL_PHOTO_ID INTEGER PRIMARY KEY NOT NULL,
                $COL_CONTENT_URI TEXT NOT NULL,
                $COL_DISPLAY_NAME TEXT NOT NULL,
                $COL_ADDED_AT_MILLIS INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun favoriteDao(): FavoriteDao = dao

    private suspend fun ensureLoaded() {
        if (initializedFromDisk) return
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                if (!initializedFromDisk) {
                    refreshFavoriteIdsLocked()
                }
            }
        }
    }

    private fun refreshFavoriteIdsLocked() {
        val ids = ArrayList<Long>()
        readableDatabase.query(
            TABLE_FAVORITES,
            arrayOf(COL_PHOTO_ID),
            null,
            null,
            null,
            null,
            "$COL_ADDED_AT_MILLIS DESC",
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(COL_PHOTO_ID)
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(idIdx))
            }
        }
        favoriteIdsFlow.value = ids
        initializedFromDisk = true
    }

    companion object {
        private const val DB_NAME = "akitech_gallery.db"
        private const val DB_VERSION = 1
        private const val TABLE_FAVORITES = "favorites"
        private const val COL_PHOTO_ID = "photoId"
        private const val COL_CONTENT_URI = "contentUri"
        private const val COL_DISPLAY_NAME = "displayName"
        private const val COL_ADDED_AT_MILLIS = "addedAtMillis"

        @Volatile
        private var instance: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return instance ?: synchronized(this) {
                instance ?: GalleryDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}

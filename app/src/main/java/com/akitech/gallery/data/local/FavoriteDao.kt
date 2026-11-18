package com.akitech.gallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT photoId FROM favorites ORDER BY addedAtMillis DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE photoId = :photoId")
    suspend fun deleteFavoriteById(photoId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE photoId = :photoId)")
    suspend fun isFavorite(photoId: Long): Boolean
}

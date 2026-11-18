package com.akitech.gallery.data.local

import kotlinx.coroutines.flow.Flow

interface FavoriteDao {
    fun observeFavoriteIds(): Flow<List<Long>>

    suspend fun insertFavorite(entity: FavoriteEntity)

    suspend fun deleteFavoriteById(photoId: Long)

    suspend fun isFavorite(photoId: Long): Boolean
}

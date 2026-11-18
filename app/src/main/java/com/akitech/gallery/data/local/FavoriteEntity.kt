package com.akitech.gallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val photoId: Long,
    val contentUri: String,
    val displayName: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
)

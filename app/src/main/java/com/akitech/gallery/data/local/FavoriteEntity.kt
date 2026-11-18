package com.akitech.gallery.data.local

data class FavoriteEntity(
    val photoId: Long,
    val contentUri: String,
    val displayName: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
)

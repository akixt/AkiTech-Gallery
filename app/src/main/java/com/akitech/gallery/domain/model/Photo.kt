package com.akitech.gallery.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

enum class MediaType {
    IMAGE,
    VIDEO,
}

@Immutable
data class Photo(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketId: Long,
    val bucketDisplayName: String,
    val relativePath: String?,
    val mediaType: MediaType = MediaType.IMAGE,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
) {
    val effectiveDateSeconds: Long
        get() = if (dateAddedSeconds > 0L) dateAddedSeconds else dateModifiedSeconds

    val isVideo: Boolean
        get() = mediaType == MediaType.VIDEO

    val isImage: Boolean
        get() = mediaType == MediaType.IMAGE
}

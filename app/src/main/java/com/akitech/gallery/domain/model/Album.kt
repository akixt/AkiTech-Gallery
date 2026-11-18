package com.akitech.gallery.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val bucketId: Long,
    val name: String,
    val coverUri: Uri,
    val photoCount: Int,
    val latestDateSeconds: Long,
)

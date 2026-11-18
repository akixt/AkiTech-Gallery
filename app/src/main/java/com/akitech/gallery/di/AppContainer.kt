package com.akitech.gallery.di

import android.content.Context
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.data.local.GalleryDatabase
import com.akitech.gallery.data.mediastore.MediaStoreDataSource
import com.akitech.gallery.data.repository.GalleryRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: GalleryDatabase by lazy {
        GalleryDatabase.getInstance(appContext)
    }
    private val mediaStoreDataSource: MediaStoreDataSource by lazy {
        MediaStoreDataSource(appContext)
    }

    val galleryRepository: GalleryRepository by lazy {
        GalleryRepository(
            context = appContext,
            mediaStoreDataSource = mediaStoreDataSource,
            favoriteDao = database.favoriteDao(),
        )
    }

    val aiMediaRepository: AiMediaRepository by lazy {
        AiMediaRepository(appContext)
    }
}

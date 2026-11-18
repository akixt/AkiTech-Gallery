package com.akitech.gallery.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import com.akitech.gallery.data.local.FavoriteDao
import com.akitech.gallery.data.local.FavoriteEntity
import com.akitech.gallery.data.mediastore.DeleteOutcome
import com.akitech.gallery.data.mediastore.MediaStoreDataSource
import com.akitech.gallery.domain.model.Album
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.GridDensity
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val favoriteDao: FavoriteDao,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appSettings = MutableStateFlow(readSavedSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val refreshNonce = MutableStateFlow(0)

    fun triggerRefresh() {
        refreshNonce.value += 1
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_appSettings.value).let {
            it.copy(gridColumns = it.gridColumns.coerceIn(2, 5))
        }
        _appSettings.value = updated
        prefs.edit()
            .putString(KEY_THEME_MODE, updated.themeMode.name)
            .putString(KEY_GRID_DENSITY, updated.gridDensity.name)
            .putString(KEY_GRID_VIEW_MODE, updated.gridViewMode.name)
            .putInt(KEY_GRID_COLUMNS, updated.gridColumns)
            .putString(KEY_SORT_ORDER, updated.defaultSortOrder.name)
            .putBoolean(KEY_AUTO_PLAY_VIDEOS, updated.autoPlayVideos)
            .putBoolean(KEY_LOOP_VIDEOS, updated.loopVideos)
            .putBoolean(KEY_SHOW_BADGES, updated.showMediaBadges)
            .apply()
    }

    private fun readSavedSettings(): AppSettings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        }.getOrDefault(ThemeMode.DARK)
        val density = runCatching {
            GridDensity.valueOf(prefs.getString(KEY_GRID_DENSITY, GridDensity.STANDARD.name) ?: GridDensity.STANDARD.name)
        }.getOrDefault(GridDensity.STANDARD)
        val viewMode = runCatching {
            GridViewMode.valueOf(prefs.getString(KEY_GRID_VIEW_MODE, GridViewMode.COMPACT.name) ?: GridViewMode.COMPACT.name)
        }.getOrDefault(GridViewMode.COMPACT)
        val columns = prefs.getInt(KEY_GRID_COLUMNS, 3).coerceIn(2, 5)
        val sort = runCatching {
            SortOrder.valueOf(prefs.getString(KEY_SORT_ORDER, SortOrder.NEWEST.name) ?: SortOrder.NEWEST.name)
        }.getOrDefault(SortOrder.NEWEST)

        return AppSettings(
            themeMode = theme,
            gridDensity = density,
            gridViewMode = viewMode,
            gridColumns = columns,
            defaultSortOrder = sort,
            autoPlayVideos = prefs.getBoolean(KEY_AUTO_PLAY_VIDEOS, true),
            loopVideos = prefs.getBoolean(KEY_LOOP_VIDEOS, true),
            showMediaBadges = prefs.getBoolean(KEY_SHOW_BADGES, true),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePhotos(): Flow<List<Photo>> {
        val deviceMediaFlow = refreshNonce.flatMapLatest {
            mediaStoreDataSource.observeDeviceMedia()
        }
        return combine(
            deviceMediaFlow,
            favoriteDao.observeFavoriteIds(),
        ) { items, favoriteIds ->
            val favSet = favoriteIds.toHashSet()
            items.map { item ->
                if (item.id in favSet) item.copy(isFavorite = true) else item
            }
        }
    }

    fun observeAlbums(photosFlow: Flow<List<Photo>> = observePhotos()): Flow<List<Album>> {
        return photosFlow.map { photos ->
            photos
                .groupBy { it.bucketId }
                .values
                .map { bucketPhotos ->
                    val newest = bucketPhotos.first()
                    Album(
                        bucketId = newest.bucketId,
                        name = newest.bucketDisplayName,
                        coverUri = newest.contentUri,
                        photoCount = bucketPhotos.size,
                        latestDateSeconds = newest.effectiveDateSeconds,
                    )
                }
                .sortedByDescending { it.latestDateSeconds }
        }
    }

    suspend fun saveEditedImage(bitmap: Bitmap, fileName: String): Uri? {
        val uri = mediaStoreDataSource.saveEditedImage(bitmap, fileName)
        if (uri != null) triggerRefresh()
        return uri
    }

    suspend fun saveEditedVideo(sourceFile: File, fileName: String): Uri? {
        val uri = mediaStoreDataSource.saveEditedVideo(sourceFile, fileName)
        if (uri != null) triggerRefresh()
        return uri
    }

    suspend fun saveGeneratedAudio(sourceFile: File, fileName: String, mimeType: String): Uri? {
        return mediaStoreDataSource.saveGeneratedAudio(sourceFile, fileName, mimeType)
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val imageCache = context.cacheDir.resolve("image_cache")
        if (!imageCache.exists()) return@withContext 0L
        imageCache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        context.cacheDir.resolve("image_cache").deleteRecursively()
    }

    suspend fun toggleFavorite(photo: Photo) {
        if (favoriteDao.isFavorite(photo.id)) {
            favoriteDao.deleteFavoriteById(photo.id)
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    photoId = photo.id,
                    contentUri = photo.contentUri.toString(),
                    displayName = photo.displayName,
                ),
            )
        }
    }

    suspend fun setFavoriteForMany(photos: List<Photo>, favorite: Boolean) {
        for (photo in photos) {
            if (favorite) {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        photoId = photo.id,
                        contentUri = photo.contentUri.toString(),
                        displayName = photo.displayName,
                    ),
                )
            } else {
                favoriteDao.deleteFavoriteById(photo.id)
            }
        }
    }

    suspend fun removeFavorite(photoId: Long) {
        favoriteDao.deleteFavoriteById(photoId)
    }

    suspend fun removeFavorites(photoIds: List<Long>) {
        for (id in photoIds) {
            favoriteDao.deleteFavoriteById(id)
        }
    }

    suspend fun requestDelete(uri: Uri): DeleteOutcome {
        return mediaStoreDataSource.requestDelete(uri)
    }

    suspend fun requestDeleteMultiple(uris: List<Uri>): DeleteOutcome {
        return mediaStoreDataSource.requestDeleteMultiple(uris)
    }

    suspend fun performLegacyDelete(uri: Uri): DeleteOutcome {
        return mediaStoreDataSource.performLegacyDelete(uri)
    }

    suspend fun performLegacyDeleteMultiple(uris: List<Uri>): DeleteOutcome {
        return mediaStoreDataSource.performLegacyDeleteMultiple(uris)
    }

    private companion object {
        const val PREFS_NAME = "akitech_gallery_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_GRID_DENSITY = "grid_density"
        const val KEY_GRID_VIEW_MODE = "grid_view_mode"
        const val KEY_GRID_COLUMNS = "grid_columns"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_AUTO_PLAY_VIDEOS = "auto_play_videos"
        const val KEY_LOOP_VIDEOS = "loop_videos"
        const val KEY_SHOW_BADGES = "show_media_badges"
    }
}

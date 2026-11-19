package com.akitech.gallery.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.akitech.gallery.AkiTechGalleryApp
import com.akitech.gallery.R
import com.akitech.gallery.data.mediastore.DeleteOutcome
import com.akitech.gallery.data.repository.GalleryRepository
import com.akitech.gallery.domain.model.Album
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.GridDensity
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.MediaFilter
import com.akitech.gallery.domain.model.MediaType
import com.akitech.gallery.domain.model.PermissionStatus
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.domain.model.ViewerSource
import com.akitech.gallery.ui.util.PermissionHelper
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DeleteEvent {
    data class LaunchSystemConfirm(val intentSender: IntentSender, val photos: List<Photo>) : DeleteEvent
    data class ShowLegacyConfirm(val photos: List<Photo>) : DeleteEvent
    data class ShowMessage(val message: String) : DeleteEvent
}

class GalleryViewModel(
    private val app: Application,
    private val repository: GalleryRepository,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val _permissionStatus = MutableStateFlow(PermissionHelper.checkPermissionStatus(app))
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    val favoritesOnlyFilter: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_FAVORITES_FILTER, false)
    val isSearchActive: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_SEARCH_ACTIVE, false)

    private val mediaFilterName: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MEDIA_FILTER, MediaFilter.ALL.name)
    val mediaFilter: StateFlow<MediaFilter> = mediaFilterName.map { name ->
        runCatching { MediaFilter.valueOf(name) }.getOrDefault(MediaFilter.ALL)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MediaFilter.ALL)

    val appSettings: StateFlow<AppSettings> = repository.appSettings
    val themeMode: StateFlow<ThemeMode> = appSettings.map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appSettings.value.themeMode)
    val sortOrder: StateFlow<SortOrder> = appSettings.map { it.defaultSortOrder }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appSettings.value.defaultSortOrder)
    val gridDensity: StateFlow<GridDensity> = appSettings.map { it.gridDensity }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appSettings.value.gridDensity)
    val gridViewMode: StateFlow<GridViewMode> = appSettings.map { it.gridViewMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appSettings.value.gridViewMode)
    val gridColumns: StateFlow<Int> = appSettings.map { it.gridColumns }
        .stateIn(viewModelScope, SharingStarted.Eagerly, appSettings.value.gridColumns)

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    val selectedAlbumBucketId: StateFlow<Long?> = savedStateHandle.getStateFlow<Long?>(KEY_SELECTED_ALBUM_ID, null)

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _deleteEvents = MutableSharedFlow<DeleteEvent>()
    val deleteEvents: SharedFlow<DeleteEvent> = _deleteEvents.asSharedFlow()

    private val _favoriteSyncEvents = MutableSharedFlow<Pair<Photo, Boolean>>(extraBufferCapacity = 32)
    val favoriteSyncEvents: SharedFlow<Pair<Photo, Boolean>> = _favoriteSyncEvents.asSharedFlow()

    private var pendingDeletePhotos: List<Photo> = emptyList()

    private val rawPhotosState: StateFlow<UiState<List<Photo>>> = repository.observePhotos()
        .map<List<Photo>, UiState<List<Photo>>> { photos -> UiState.Success(photos) }
        .onStart { emit(UiState.Loading) }
        .catch { throwable ->
            emit(UiState.Error(throwable.localizedMessage ?: app.getString(R.string.error_load_title)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UiState.Loading,
        )

    val filteredPhotosState: StateFlow<UiState<List<Photo>>> = combine(
        rawPhotosState,
        searchQuery,
        favoritesOnlyFilter,
        mediaFilter,
        sortOrder,
    ) { state, query, favOnly, typeFilter, order ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim()
                val filtered = state.data.filter { photo ->
                    val matchesType = when (typeFilter) {
                        MediaFilter.ALL -> true
                        MediaFilter.PHOTOS -> photo.mediaType == MediaType.IMAGE
                        MediaFilter.VIDEOS -> photo.mediaType == MediaType.VIDEO
                    }
                    val matchesFav = !favOnly || photo.isFavorite
                    val matchesQuery = trimmed.isEmpty() ||
                        photo.displayName.contains(trimmed, ignoreCase = true) ||
                        photo.bucketDisplayName.contains(trimmed, ignoreCase = true) ||
                        photo.mimeType.contains(trimmed, ignoreCase = true)
                    matchesType && matchesFav && matchesQuery
                }
                UiState.Success(applySort(filtered, order))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState.Loading,
    )

    val filteredAlbumsState: StateFlow<UiState<List<Album>>> = combine(
        rawPhotosState,
        searchQuery,
    ) { state, query ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim()
                val albums = state.data
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
                    .filter { album ->
                        trimmed.isEmpty() || album.name.contains(trimmed, ignoreCase = true)
                    }
                    .sortedByDescending { it.latestDateSeconds }
                UiState.Success(albums)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState.Loading,
    )

    val favoritePhotosState: StateFlow<UiState<List<Photo>>> = combine(
        rawPhotosState,
        searchQuery,
        sortOrder,
    ) { state, query, order ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmed = query.trim()
                val favs = state.data.filter { photo ->
                    photo.isFavorite && (
                        trimmed.isEmpty() ||
                            photo.displayName.contains(trimmed, ignoreCase = true) ||
                            photo.bucketDisplayName.contains(trimmed, ignoreCase = true)
                        )
                }
                UiState.Success(applySort(favs, order))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = UiState.Loading,
    )

    val recentDeviceImages: StateFlow<List<Photo>> = rawPhotosState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.filter { it.isImage }.take(20)
                else -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    init {
        refreshCacheSize()
    }

    fun observeAlbumPhotos(bucketId: Long): Flow<UiState<List<Photo>>> {
        return combine(
            rawPhotosState,
            searchQuery,
            favoritesOnlyFilter,
            mediaFilter,
            sortOrder,
        ) { state, query, favOnly, typeFilter, order ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val trimmed = query.trim()
                    val list = state.data.filter { photo ->
                        val matchesType = when (typeFilter) {
                            MediaFilter.ALL -> true
                            MediaFilter.PHOTOS -> photo.mediaType == MediaType.IMAGE
                            MediaFilter.VIDEOS -> photo.mediaType == MediaType.VIDEO
                        }
                        photo.bucketId == bucketId &&
                            matchesType &&
                            (!favOnly || photo.isFavorite) &&
                            (trimmed.isEmpty() || photo.displayName.contains(trimmed, ignoreCase = true))
                    }
                    UiState.Success(applySort(list, order))
                }
            }
        }
    }

    fun observeViewerPhotos(source: ViewerSource, bucketId: Long): Flow<List<Photo>> {
        return when (source) {
            ViewerSource.ALL_PHOTOS -> filteredPhotosState.map { (it as? UiState.Success)?.data.orEmpty() }
            ViewerSource.ALBUM -> observeAlbumPhotos(bucketId).map { (it as? UiState.Success)?.data.orEmpty() }
            ViewerSource.FAVORITES -> favoritePhotosState.map { (it as? UiState.Success)?.data.orEmpty() }
        }
    }

    fun findMediaById(id: Long): Photo? {
        return (rawPhotosState.value as? UiState.Success)?.data?.firstOrNull { it.id == id }
    }

    private fun applySort(photos: List<Photo>, order: SortOrder): List<Photo> {
        return when (order) {
            SortOrder.NEWEST -> photos.sortedByDescending { it.effectiveDateSeconds }
            SortOrder.OLDEST -> photos.sortedBy { it.effectiveDateSeconds }
            SortOrder.NAME -> photos.sortedBy { it.displayName.lowercase() }
            SortOrder.LARGEST -> photos.sortedByDescending { it.sizeBytes }
        }
    }

    fun refreshPermissionAndGallery() {
        _permissionStatus.value = PermissionHelper.checkPermissionStatus(app)
        repository.triggerRefresh()
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun setSearchActive(active: Boolean) {
        savedStateHandle[KEY_SEARCH_ACTIVE] = active
        if (!active) {
            savedStateHandle[KEY_SEARCH_QUERY] = ""
        }
    }

    fun setMediaFilter(filter: MediaFilter) {
        savedStateHandle[KEY_MEDIA_FILTER] = filter.name
    }

    fun toggleFavoritesFilter() {
        savedStateHandle[KEY_FAVORITES_FILTER] = !favoritesOnlyFilter.value
    }

    fun setSortOrder(order: SortOrder) {
        repository.updateSettings { it.copy(defaultSortOrder = order) }
    }

    fun setGridDensity(density: GridDensity) {
        repository.updateSettings { it.copy(gridDensity = density) }
    }

    fun cycleGridDensity() {
        val next = when (gridDensity.value) {
            GridDensity.COZY -> GridDensity.STANDARD
            GridDensity.STANDARD -> GridDensity.COMPACT
            GridDensity.COMPACT -> GridDensity.COZY
        }
        setGridDensity(next)
    }

    fun setGridViewMode(mode: GridViewMode) {
        repository.updateSettings { it.copy(gridViewMode = mode) }
    }

    fun toggleGridViewMode() {
        val next = if (gridViewMode.value == GridViewMode.COMPACT) {
            GridViewMode.DETAILED
        } else {
            GridViewMode.COMPACT
        }
        setGridViewMode(next)
    }

    fun setGridColumns(columns: Int) {
        repository.updateSettings { it.copy(gridColumns = columns.coerceIn(2, 5)) }
    }

    fun cycleGridColumns() {
        val current = gridColumns.value.coerceIn(2, 5)
        val next = if (current >= 5) 2 else current + 1
        setGridColumns(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.updateSettings { it.copy(themeMode = mode) }
    }

    fun cycleThemeMode() {
        val next = when (themeMode.value) {
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        repository.updateSettings { it.copy(autoPlayVideos = enabled) }
    }

    fun setLoopVideos(enabled: Boolean) {
        repository.updateSettings { it.copy(loopVideos = enabled) }
    }

    fun setShowMediaBadges(enabled: Boolean) {
        repository.updateSettings { it.copy(showMediaBadges = enabled) }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = repository.getCacheSizeBytes()
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch {
            repository.clearCache()
            _cacheSizeBytes.value = repository.getCacheSizeBytes()
            _deleteEvents.emit(DeleteEvent.ShowMessage("Thumbnail cache cleared"))
        }
    }

    fun setSelectedAlbumBucketId(bucketId: Long?) {
        savedStateHandle[KEY_SELECTED_ALBUM_ID] = bucketId
    }

    fun togglePhotoSelection(photoId: Long) {
        val current = _selectedPhotoIds.value
        _selectedPhotoIds.value = if (photoId in current) current - photoId else current + photoId
    }

    fun selectAll(photos: List<Photo>) {
        _selectedPhotoIds.value = photos.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun currentSelectedPhotos(): List<Photo> {
        val ids = _selectedPhotoIds.value
        val all = (rawPhotosState.value as? UiState.Success)?.data.orEmpty()
        return all.filter { it.id in ids }
    }

    fun favoriteSelectedPhotos() {
        val targets = currentSelectedPhotos()
        if (targets.isEmpty()) return
        val shouldFavorite = targets.any { !it.isFavorite }
        viewModelScope.launch {
            repository.setFavoriteForMany(targets, shouldFavorite)
            targets.forEach { _favoriteSyncEvents.tryEmit(it to shouldFavorite) }
            clearSelection()
        }
    }

    fun deleteSelectedPhotos() {
        val targets = currentSelectedPhotos()
        if (targets.isEmpty()) return
        requestDeletePhotos(targets)
    }

    fun toggleFavorite(photo: Photo) {
        val nextFavorite = !photo.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(photo)
            _favoriteSyncEvents.tryEmit(photo to nextFavorite)
        }
    }

    suspend fun saveEditedImage(bitmap: Bitmap, originalName: String): Uri? {
        val base = originalName.substringBeforeLast('.').ifBlank { "AkiTech_Edit" }
        val fileName = "${base}_edited_${System.currentTimeMillis()}.jpg"
        return repository.saveEditedImage(bitmap, fileName)
    }

    suspend fun saveEditedVideo(sourceFile: File, originalName: String): Uri? {
        val base = originalName.substringBeforeLast('.').ifBlank { "AkiTech_Video" }
        val fileName = "${base}_edited_${System.currentTimeMillis()}.mp4"
        return repository.saveEditedVideo(sourceFile, fileName)
    }

    suspend fun saveGeneratedAudio(sourceFile: File, titlePrefix: String, mimeType: String): Uri? {
        val ext = when {
            mimeType.contains("wav", ignoreCase = true) -> "wav"
            mimeType.contains("aac", ignoreCase = true) -> "aac"
            mimeType.contains("ogg", ignoreCase = true) -> "ogg"
            else -> "mp3"
        }
        val fileName = "${titlePrefix}_${System.currentTimeMillis()}.$ext"
        return repository.saveGeneratedAudio(sourceFile, fileName, mimeType)
    }

    fun notifyMessage(message: String) {
        viewModelScope.launch {
            _deleteEvents.emit(DeleteEvent.ShowMessage(message))
        }
    }

    fun requestDeletePhoto(photo: Photo) {
        requestDeletePhotos(listOf(photo))
    }

    private fun requestDeletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            pendingDeletePhotos = photos
            val uris = photos.map { it.contentUri }
            when (val outcome = repository.requestDeleteMultiple(uris)) {
                is DeleteOutcome.RequiresConfirmation -> {
                    _deleteEvents.emit(DeleteEvent.LaunchSystemConfirm(outcome.intentSender, photos))
                }
                is DeleteOutcome.RequiresLegacyDialog -> {
                    _deleteEvents.emit(DeleteEvent.ShowLegacyConfirm(photos))
                }
                is DeleteOutcome.Deleted -> {
                    repository.removeFavorites(photos.map { it.id })
                    repository.triggerRefresh()
                    clearSelection()
                    pendingDeletePhotos = emptyList()
                    _deleteEvents.emit(DeleteEvent.ShowMessage(app.getString(R.string.photo_deleted_message)))
                }
                is DeleteOutcome.Failed -> {
                    pendingDeletePhotos = emptyList()
                    _deleteEvents.emit(DeleteEvent.ShowMessage(outcome.message))
                }
            }
        }
    }

    fun confirmLegacyDelete(photos: List<Photo>) {
        viewModelScope.launch {
            when (val outcome = repository.performLegacyDeleteMultiple(photos.map { it.contentUri })) {
                is DeleteOutcome.Deleted -> {
                    repository.removeFavorites(photos.map { it.id })
                    repository.triggerRefresh()
                    clearSelection()
                    pendingDeletePhotos = emptyList()
                    _deleteEvents.emit(DeleteEvent.ShowMessage(app.getString(R.string.photo_deleted_message)))
                }
                is DeleteOutcome.RequiresConfirmation -> {
                    _deleteEvents.emit(DeleteEvent.LaunchSystemConfirm(outcome.intentSender, photos))
                }
                is DeleteOutcome.RequiresLegacyDialog -> Unit
                is DeleteOutcome.Failed -> {
                    pendingDeletePhotos = emptyList()
                    _deleteEvents.emit(DeleteEvent.ShowMessage(outcome.message))
                }
            }
        }
    }

    fun onSystemDeleteResult(confirmed: Boolean) {
        val targets = pendingDeletePhotos
        pendingDeletePhotos = emptyList()
        viewModelScope.launch {
            if (confirmed && targets.isNotEmpty()) {
                repository.removeFavorites(targets.map { it.id })
                repository.triggerRefresh()
                clearSelection()
                _deleteEvents.emit(DeleteEvent.ShowMessage(app.getString(R.string.photo_deleted_message)))
            }
        }
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SEARCH_ACTIVE = "search_active"
        private const val KEY_FAVORITES_FILTER = "favorites_only_filter"
        private const val KEY_MEDIA_FILTER = "media_filter"
        private const val KEY_SELECTED_ALBUM_ID = "selected_album_bucket_id"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AkiTechGalleryApp
                val savedStateHandle = createSavedStateHandle()
                GalleryViewModel(
                    app = app,
                    repository = app.container.galleryRepository,
                    savedStateHandle = savedStateHandle,
                )
            }
        }
    }
}

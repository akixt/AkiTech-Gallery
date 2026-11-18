package com.akitech.gallery.domain.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class GalleryTab {
    PHOTOS,
    ALBUMS,
    AI_STUDIO,
    FAVORITES,
    SETTINGS,
}

enum class MediaFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    SYSTEM("System"),
    LIGHT("Light"),
}

enum class PermissionStatus {
    UNKNOWN,
    GRANTED,
    LIMITED,
    DENIED,
}

enum class ViewerSource {
    ALL_PHOTOS,
    ALBUM,
    FAVORITES,
}

enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name A–Z"),
    LARGEST("Largest size"),
}

enum class GridDensity(val label: String) {
    COZY("Cozy"),
    STANDARD("Standard"),
    COMPACT("Compact"),
}

enum class GridViewMode(val label: String) {
    COMPACT("Compact"),
    DETAILED("Detailed"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val gridDensity: GridDensity = GridDensity.STANDARD,
    val gridViewMode: GridViewMode = GridViewMode.COMPACT,
    val gridColumns: Int = 3,
    val defaultSortOrder: SortOrder = SortOrder.NEWEST,
    val autoPlayVideos: Boolean = true,
    val loopVideos: Boolean = true,
    val showMediaBadges: Boolean = true,
)

data class PhotoSection(
    val title: String,
    val photos: List<Photo>,
)

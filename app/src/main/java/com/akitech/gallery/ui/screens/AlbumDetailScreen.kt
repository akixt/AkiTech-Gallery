package com.akitech.gallery.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.ui.components.EmptyStateView
import com.akitech.gallery.ui.components.ErrorStateView
import com.akitech.gallery.ui.components.LoadingStateView

@Composable
fun AlbumDetailScreen(
    uiState: UiState<List<Photo>>,
    searchQuery: String,
    favoritesOnly: Boolean,
    sortOrder: SortOrder,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    showMediaBadges: Boolean,
    selectedIds: Set<Long>,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Column(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(
                message = uiState.message,
                onRetry = onRetry,
            )
            is UiState.Success -> {
                val photos = uiState.data
                if (photos.isEmpty()) {
                    val hasFilter = searchQuery.isNotBlank() || favoritesOnly
                    EmptyStateView(
                        icon = if (hasFilter) Icons.Rounded.SearchOff else Icons.Rounded.PhotoLibrary,
                        title = stringResource(
                            if (hasFilter) R.string.empty_search_title else R.string.empty_photos_title,
                        ),
                        subtitle = stringResource(
                            if (hasFilter) R.string.empty_search_subtitle else R.string.empty_photos_subtitle,
                        ),
                        actionLabel = if (hasFilter) "Clear Filters" else null,
                        onActionClick = if (hasFilter) onClearFilters else null,
                    )
                } else {
                    PhotoGridContent(
                        photos = photos,
                        gridColumns = gridColumns,
                        gridViewMode = gridViewMode,
                        showMediaBadges = showMediaBadges,
                        selectedIds = selectedIds,
                        showDateSections = sortOrder == SortOrder.NEWEST || sortOrder == SortOrder.OLDEST,
                        bottomContentPadding = bottomContentPadding,
                        onPhotoClick = onPhotoClick,
                        onPhotoLongClick = onPhotoLongClick,
                    )
                }
            }
        }
    }
}

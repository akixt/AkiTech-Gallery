package com.akitech.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.ui.components.EmptyStateView
import com.akitech.gallery.ui.components.ErrorStateView
import com.akitech.gallery.ui.components.LoadingStateView

@Composable
fun FavoritesScreen(
    uiState: UiState<List<Photo>>,
    searchQuery: String,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    showMediaBadges: Boolean,
    selectedIds: Set<Long>,
    bottomContentPadding: Dp,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onRetry: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(
                message = uiState.message,
                onRetry = onRetry,
            )
            is UiState.Success -> {
                val favorites = uiState.data
                if (favorites.isEmpty()) {
                    val hasSearch = searchQuery.isNotBlank()
                    EmptyStateView(
                        icon = if (hasSearch) Icons.Rounded.SearchOff else Icons.Rounded.FavoriteBorder,
                        title = stringResource(
                            if (hasSearch) R.string.empty_search_title else R.string.empty_favorites_title,
                        ),
                        subtitle = stringResource(
                            if (hasSearch) R.string.empty_search_subtitle else R.string.empty_favorites_subtitle,
                        ),
                        actionLabel = if (hasSearch) "Clear Search" else null,
                        onActionClick = if (hasSearch) onClearSearch else null,
                    )
                } else {
                    PhotoGridContent(
                        photos = favorites,
                        gridColumns = gridColumns,
                        gridViewMode = gridViewMode,
                        showMediaBadges = showMediaBadges,
                        selectedIds = selectedIds,
                        bottomContentPadding = bottomContentPadding,
                        onPhotoClick = onPhotoClick,
                        onPhotoLongClick = onPhotoLongClick,
                        headerContent = {
                            if (searchQuery.isBlank() && selectedIds.isEmpty()) {
                                FavoritesSummaryBanner(
                                    count = favorites.size,
                                    onBrowseClick = { onPhotoClick(favorites.first()) },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesSummaryBanner(
    count: Int,
    onBrowseClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                shape = shape,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Saved Memories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$count favorite ${if (count == 1) "photo" else "photos"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onBrowseClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "View")
                }
            }
        }
    }
}

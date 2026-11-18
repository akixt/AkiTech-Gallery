package com.akitech.gallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akitech.gallery.MediaStoreLazyGrid
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.PhotoSection
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.ui.components.EmptyStateView
import com.akitech.gallery.ui.components.ErrorStateView
import com.akitech.gallery.ui.components.LoadingStateView
import com.akitech.gallery.ui.components.PhotoGridItem
import com.akitech.gallery.ui.util.Formatters

@Composable
fun PhotosScreen(
    uiState: UiState<List<Photo>>,
    searchQuery: String,
    favoritesOnly: Boolean,
    sortOrder: SortOrder,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    showMediaBadges: Boolean,
    selectedIds: Set<Long>,
    bottomContentPadding: Dp,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    onRetry: () -> Unit,
    onClearFilters: () -> Unit,
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
                    val showHero = searchQuery.isBlank() &&
                        !favoritesOnly &&
                        sortOrder == SortOrder.NEWEST &&
                        selectedIds.isEmpty() &&
                        photos.size >= 4
                    val showSections = sortOrder == SortOrder.NEWEST || sortOrder == SortOrder.OLDEST

                    PhotoGridContent(
                        photos = photos,
                        gridColumns = gridColumns,
                        gridViewMode = gridViewMode,
                        showMediaBadges = showMediaBadges,
                        selectedIds = selectedIds,
                        showHeroSpotlight = showHero,
                        showDateSections = showSections,
                        bottomContentPadding = bottomContentPadding,
                        onPhotoClick = onPhotoClick,
                        onPhotoLongClick = onPhotoLongClick,
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoGridContent(
    photos: List<Photo>,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    selectedIds: Set<Long>,
    bottomContentPadding: Dp,
    onPhotoClick: (Photo) -> Unit,
    onPhotoLongClick: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    showMediaBadges: Boolean = true,
    showHeroSpotlight: Boolean = false,
    showDateSections: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val isSelectionMode = selectedIds.isNotEmpty()
    val isDetailedView = gridViewMode == GridViewMode.DETAILED

    val sections = remember(photos, showDateSections) {
        if (!showDateSections) {
            emptyList()
        } else {
            val grouped = LinkedHashMap<String, MutableList<Photo>>()
            for (photo in photos) {
                val label = Formatters.formatSectionDate(photo.effectiveDateSeconds)
                grouped.getOrPut(label) { mutableListOf() }.add(photo)
            }
            grouped.entries.map { PhotoSection(title = it.key, photos = it.value) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val baseColumns = gridColumns.coerceIn(2, 5)
        val columns = when {
            maxWidth >= 840.dp -> (baseColumns + 2).coerceAtMost(7)
            maxWidth >= 600.dp -> (baseColumns + 1).coerceAtMost(6)
            else -> baseColumns
        }

        val spacing = when {
            isDetailedView -> if (baseColumns >= 4) 8.dp else 10.dp
            baseColumns <= 2 -> 10.dp
            baseColumns == 3 -> 7.dp
            else -> 4.dp
        }
        val cornerRadius = when {
            isDetailedView -> if (baseColumns >= 4) 12.dp else 16.dp
            baseColumns <= 2 -> 18.dp
            baseColumns == 3 -> 14.dp
            else -> 10.dp
        }

        val heroHeight = if (maxWidth >= 600.dp) 220.dp else 184.dp

        MediaStoreLazyGrid(
            photos = photos,
            columns = columns,
            spacing = spacing,
            bottomContentPadding = bottomContentPadding,
            gridState = gridState,
            sections = if (showDateSections) sections else emptyList(),
            headerContent = headerContent,
            spotlightContent = if (showHeroSpotlight) {
                { heroPhoto ->
                    LatestCaptureSpotlightCard(
                        photo = heroPhoto,
                        height = heroHeight,
                        onClick = { onPhotoClick(heroPhoto) },
                    )
                }
            } else {
                null
            },
            sectionHeaderContent = if (showDateSections) {
                { section ->
                    SectionHeader(
                        title = section.title,
                        count = section.photos.size,
                    )
                }
            } else {
                null
            },
            itemContent = { photo ->
                PhotoGridItem(
                    photo = photo,
                    isSelectionMode = isSelectionMode,
                    isSelected = photo.id in selectedIds,
                    showBadges = showMediaBadges,
                    isDetailedView = isDetailedView,
                    cornerRadius = cornerRadius,
                    onClick = { onPhotoClick(photo) },
                    onLongClick = { onPhotoLongClick(photo) },
                )
            },
        )
    }
}

@Composable
private fun LatestCaptureSpotlightCard(
    photo: Photo,
    height: Dp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(height)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .testTag("latest_capture_spotlight"),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.contentUri)
                .size(1024)
                .crossfade(true)
                .build(),
            contentDescription = "Latest capture: ${photo.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.86f),
                        ),
                    ),
                ),
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.52f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LATEST CAPTURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = photo.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${photo.bucketDisplayName} • ${Formatters.formatCompactDate(photo.effectiveDateSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

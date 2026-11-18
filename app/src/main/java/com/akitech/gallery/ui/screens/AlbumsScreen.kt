package com.akitech.gallery.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.Album
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.ui.components.EmptyStateView
import com.akitech.gallery.ui.components.ErrorStateView
import com.akitech.gallery.ui.components.LoadingStateView
import com.akitech.gallery.ui.util.Formatters

@Composable
fun AlbumsScreen(
    uiState: UiState<List<Album>>,
    searchQuery: String,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    showMediaBadges: Boolean,
    selectedIds: Set<Long>,
    selectedAlbumBucketId: Long?,
    selectedAlbumPhotosState: UiState<List<Photo>>?,
    bottomContentPadding: Dp,
    onAlbumClick: (Album, isTwoPane: Boolean) -> Unit,
    onPhotoClick: (Photo, bucketId: Long) -> Unit,
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
                val albums = uiState.data
                if (albums.isEmpty()) {
                    val hasSearch = searchQuery.isNotBlank()
                    EmptyStateView(
                        icon = if (hasSearch) Icons.Rounded.SearchOff else Icons.Rounded.FolderCopy,
                        title = stringResource(
                            if (hasSearch) R.string.empty_search_title else R.string.empty_albums_title,
                        ),
                        subtitle = stringResource(
                            if (hasSearch) R.string.empty_search_subtitle else R.string.empty_albums_subtitle,
                        ),
                        actionLabel = if (hasSearch) "Clear Search" else null,
                        onActionClick = if (hasSearch) onClearSearch else null,
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isTwoPane = maxWidth >= 840.dp

                        if (isTwoPane) {
                            LaunchedEffect(albums, selectedAlbumBucketId) {
                                if (selectedAlbumBucketId == null && albums.isNotEmpty()) {
                                    onAlbumClick(albums.first(), true)
                                }
                            }

                            Row(modifier = Modifier.fillMaxSize()) {
                                AlbumsGridContent(
                                    albums = albums,
                                    columns = 2,
                                    selectedBucketId = selectedAlbumBucketId,
                                    bottomContentPadding = 24.dp,
                                    onAlbumClick = { album -> onAlbumClick(album, true) },
                                    modifier = Modifier
                                        .weight(0.42f)
                                        .fillMaxHeight(),
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .fillMaxHeight(),
                                ) {
                                    val detailState = selectedAlbumPhotosState
                                    val activeAlbum = albums.firstOrNull { it.bucketId == selectedAlbumBucketId }
                                    if (detailState != null && activeAlbum != null) {
                                        when (detailState) {
                                            is UiState.Loading -> LoadingStateView()
                                            is UiState.Error -> ErrorStateView(
                                                message = detailState.message,
                                                onRetry = onRetry,
                                            )
                                            is UiState.Success -> {
                                                PhotoGridContent(
                                                    photos = detailState.data,
                                                    gridColumns = gridColumns,
                                                    gridViewMode = gridViewMode,
                                                    showMediaBadges = showMediaBadges,
                                                    selectedIds = selectedIds,
                                                    bottomContentPadding = 24.dp,
                                                    onPhotoClick = { photo -> onPhotoClick(photo, activeAlbum.bucketId) },
                                                    onPhotoLongClick = onPhotoLongClick,
                                                    headerContent = {
                                                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                                            Text(
                                                                text = activeAlbum.name,
                                                                style = MaterialTheme.typography.headlineSmall,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                            )
                                                            Text(
                                                                text = "${detailState.data.size} items • Updated ${Formatters.formatCompactDate(activeAlbum.latestDateSeconds)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val columns = if (maxWidth >= 600.dp) 3 else 2
                            AlbumsGridContent(
                                albums = albums,
                                columns = columns,
                                selectedBucketId = null,
                                bottomContentPadding = bottomContentPadding,
                                onAlbumClick = { album -> onAlbumClick(album, false) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsGridContent(
    albums: List<Album>,
    columns: Int,
    selectedBucketId: Long?,
    bottomContentPadding: Dp,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("albums_grid"),
    ) {
        items(
            items = albums,
            key = { it.bucketId },
            contentType = { "album_card" },
        ) { album ->
            AlbumCard(
                album = album,
                isHighlighted = album.bucketId == selectedBucketId,
                onClick = { onAlbumClick(album) },
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    isHighlighted: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "album_card_scale",
    )

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(
                width = if (isHighlighted) 2.dp else 1.dp,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            )
            .testTag("album_card_${album.bucketId}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(album.coverUri)
                    .size(560)
                    .crossfade(true)
                    .build(),
                contentDescription = "${album.name} album, ${album.photoCount} photos",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.56f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .border(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50),
                    ),
            ) {
                Text(
                    text = "${album.photoCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatCompactDate(album.latestDateSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }
    }
}

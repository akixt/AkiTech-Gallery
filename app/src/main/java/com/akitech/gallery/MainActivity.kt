package com.akitech.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.akitech.gallery.ai.configureAppCheck
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.PhotoSection
import com.akitech.gallery.ui.navigation.GalleryAppRoot
import com.akitech.gallery.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels { GalleryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            GalleryAppRoot(viewModel = viewModel)
        }
    }
}

/**
 * Primary [LazyVerticalGrid] component wired from [MainActivity] to render MediaStore
 * photos and videos with responsive columns, optional date sections, and Coil thumbnails.
 */
@Composable
fun MediaStoreLazyGrid(
    photos: List<Photo>,
    columns: Int,
    spacing: Dp,
    bottomContentPadding: Dp,
    itemContent: @Composable (Photo) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    sections: List<PhotoSection> = emptyList(),
    headerContent: (@Composable () -> Unit)? = null,
    spotlightContent: (@Composable (Photo) -> Unit)? = null,
    sectionHeaderContent: (@Composable (PhotoSection) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceAtLeast(1)),
        state = gridState,
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 6.dp,
            bottom = bottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .fillMaxSize()
            .testTag("photos_grid"),
    ) {
        if (headerContent != null) {
            item(
                key = "custom_header",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "header",
            ) {
                headerContent()
            }
        }

        if (spotlightContent != null && photos.isNotEmpty()) {
            val heroPhoto = photos.first()
            item(
                key = "hero_spotlight_${heroPhoto.id}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "hero_spotlight",
            ) {
                spotlightContent(heroPhoto)
            }
        }

        if (sections.isNotEmpty() && sectionHeaderContent != null) {
            sections.forEach { section ->
                item(
                    key = "section_${section.title}",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header",
                ) {
                    sectionHeaderContent(section)
                }
                items(
                    items = section.photos,
                    key = { it.id },
                    contentType = { "photo_grid_item" },
                ) { photo ->
                    itemContent(photo)
                }
            }
        } else {
            items(
                items = photos,
                key = { it.id },
                contentType = { "photo_grid_item" },
            ) { photo ->
                itemContent(photo)
            }
        }
    }
}

/**
 * Efficient Coil thumbnail loader for MediaStore [Photo] items inside [MediaStoreLazyGrid].
 * Uses bounded target dimensions plus versioned memory/disk cache keys to avoid OOMs.
 */
@Composable
fun MediaStoreCoilThumbnail(
    photo: Photo,
    contentDescription: String,
    modifier: Modifier = Modifier,
    thumbnailSizePx: Int = 384,
) {
    val context = LocalContext.current
    val cacheKey = "mediastore_thumb_${photo.id}_${photo.dateModifiedSeconds}"

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(photo.contentUri)
            .apply {
                if (photo.isVideo) {
                    videoFrameMillis(500L)
                }
            }
            .size(thumbnailSizePx)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

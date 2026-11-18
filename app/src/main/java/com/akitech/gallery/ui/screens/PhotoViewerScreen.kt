package com.akitech.gallery.ui.screens

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.ui.components.PhotoDetailsSheet
import com.akitech.gallery.ui.util.Formatters
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PhotoViewerScreen(
    photos: List<Photo>,
    initialPhotoId: Long,
    autoPlayVideos: Boolean,
    loopVideos: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    onSharePhoto: (Photo) -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onEditMedia: (Photo) -> Unit,
    onAiStudioPhoto: (Photo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(photos.size) {
        if (photos.isEmpty()) {
            onBack()
        }
    }

    if (photos.isEmpty()) return

    val initialIndex = remember(initialPhotoId, photos) {
        photos.indexOfFirst { it.id == initialPhotoId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { photos.size },
    )
    val filmstripState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val coroutineScope = rememberCoroutineScope()

    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showDetailsSheet by rememberSaveable { mutableStateOf(false) }
    var isSlideshowActive by rememberSaveable { mutableStateOf(false) }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    val safePage = pagerState.currentPage.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val currentPhoto = photos[safePage]

    LaunchedEffect(safePage) {
        activeZoomScale = 1f
        if (photos.size > 1) {
            filmstripState.animateScrollToItem((safePage - 2).coerceAtLeast(0))
        }
    }

    // Slideshow auto-advance for images
    LaunchedEffect(isSlideshowActive, safePage, photos.size) {
        if (isSlideshowActive && photos.size > 1 && !currentPhoto.isVideo) {
            delay(3_500L)
            val next = (safePage + 1) % photos.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_viewer_screen"),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { index -> photos[index].id },
            modifier = Modifier
                .fillMaxSize()
                .testTag("photo_viewer_pager"),
        ) { pageIndex ->
            val photo = photos[pageIndex]
            val isCurrentPage = pagerState.settledPage == pageIndex

            if (photo.isVideo) {
                VideoPlayerPage(
                    video = photo,
                    isCurrentPage = isCurrentPage,
                    showHud = showChrome,
                    autoPlay = autoPlayVideos,
                    defaultLoop = loopVideos,
                    hasFilmstrip = photos.size > 1,
                    onToggleHud = { showChrome = !showChrome },
                )
            } else {
                ZoomablePhotoPage(
                    photo = photo,
                    isCurrentPage = isCurrentPage,
                    onSingleTap = { showChrome = !showChrome },
                    onScaleChanged = { newScale ->
                        if (pagerState.currentPage == pageIndex) {
                            activeZoomScale = newScale
                        }
                    },
                )
            }
        }

        // Top bar overlay
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.38f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("viewer_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "${safePage + 1} / ${photos.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("viewer_photo_counter"),
                        )
                        Text(
                            text = currentPhoto.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photos.size > 1) {
                            IconButton(
                                onClick = { isSlideshowActive = !isSlideshowActive },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("viewer_slideshow_button"),
                            ) {
                                Icon(
                                    imageVector = if (isSlideshowActive) {
                                        Icons.Rounded.PauseCircleOutline
                                    } else {
                                        Icons.Rounded.Slideshow
                                    },
                                    contentDescription = if (isSlideshowActive) {
                                        "Pause slideshow"
                                    } else {
                                        "Start slideshow"
                                    },
                                    tint = if (isSlideshowActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White
                                    },
                                )
                            }
                        }

                        val heartScale by animateFloatAsState(
                            targetValue = if (currentPhoto.isFavorite) 1.18f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "favorite_heart_scale",
                        )

                        IconButton(
                            onClick = { onToggleFavorite(currentPhoto) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("viewer_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (currentPhoto.isFavorite) {
                                    Icons.Rounded.Favorite
                                } else {
                                    Icons.Rounded.FavoriteBorder
                                },
                                contentDescription = stringResource(
                                    if (currentPhoto.isFavorite) {
                                        R.string.cd_favorite_remove
                                    } else {
                                        R.string.cd_favorite_add
                                    },
                                ),
                                tint = if (currentPhoto.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.graphicsLayer {
                                    scaleX = heartScale
                                    scaleY = heartScale
                                },
                            )
                        }
                    }
                }
            }
        }

        // Live zoom factor pill for photos
        AnimatedVisibility(
            visible = !currentPhoto.isVideo && activeZoomScale > 1.06f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                ),
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f×", activeZoomScale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }

        // Bottom filmstrip + floating glassmorphic action bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.90f),
                        ),
                    ),
                )
                .navigationBarsPadding()
                .padding(bottom = 14.dp, top = 12.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (photos.size > 1) {
                    val context = LocalContext.current
                    LazyRow(
                        state = filmstripState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        itemsIndexed(
                            items = photos,
                            key = { _, item -> "strip_${item.id}" },
                        ) { index, item ->
                            val isSelected = index == safePage
                            val thumbSize = if (isSelected) 52.dp else 44.dp
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.8.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.25f)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        isSlideshowActive = false
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.contentUri)
                                        .apply {
                                            if (item.isVideo) videoFrameMillis(500L)
                                        }
                                        .size(160)
                                        .build(),
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (item.isVideo) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(18.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFF111319).copy(alpha = 0.92f),
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(30.dp),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ViewerActionItem(
                            icon = Icons.Rounded.Share,
                            label = "Share",
                            contentDescription = stringResource(R.string.cd_share),
                            tint = Color.White,
                            onClick = { onSharePhoto(currentPhoto) },
                            testTag = "viewer_share_button",
                        )
                        ViewerActionItem(
                            icon = Icons.Rounded.Tune,
                            label = "Edit",
                            contentDescription = stringResource(R.string.cd_edit),
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                isSlideshowActive = false
                                onEditMedia(currentPhoto)
                            },
                            testTag = "viewer_edit_button",
                        )
                        if (!currentPhoto.isVideo) {
                            ViewerActionItem(
                                icon = Icons.Rounded.AutoAwesome,
                                label = "AI",
                                contentDescription = "Open in AI Studio",
                                tint = MaterialTheme.colorScheme.secondary,
                                onClick = {
                                    isSlideshowActive = false
                                    onAiStudioPhoto(currentPhoto)
                                },
                                testTag = "viewer_ai_studio_button",
                            )
                        }
                        ViewerActionItem(
                            icon = Icons.Rounded.Info,
                            label = "Details",
                            contentDescription = stringResource(R.string.cd_details),
                            tint = Color.White,
                            onClick = { showDetailsSheet = true },
                            testTag = "viewer_details_button",
                        )
                        ViewerActionItem(
                            icon = Icons.Rounded.DeleteOutline,
                            label = "Delete",
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = Color(0xFFF87171),
                            onClick = { onDeletePhoto(currentPhoto) },
                            testTag = "viewer_delete_button",
                        )
                    }
                }
            }
        }
    }

    if (showDetailsSheet) {
        PhotoDetailsSheet(
            photo = currentPhoto,
            onDismiss = { showDetailsSheet = false },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerPage(
    video: Photo,
    isCurrentPage: Boolean,
    showHud: Boolean,
    autoPlay: Boolean,
    defaultLoop: Boolean,
    hasFilmstrip: Boolean,
    onToggleHud: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember(video.id) { mutableStateOf(false) }
    var isMuted by remember(video.id) { mutableStateOf(false) }
    var isLooping by remember(video.id, defaultLoop) { mutableStateOf(defaultLoop) }
    var playbackSpeed by remember(video.id) { mutableFloatStateOf(1f) }
    var positionMs by remember(video.id) { mutableLongStateOf(0L) }
    var durationMs by remember(video.id) { mutableLongStateOf(video.durationMs.coerceAtLeast(1L)) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.contentUri))
            repeatMode = if (defaultLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    LaunchedEffect(isCurrentPage, autoPlay) {
        if (isCurrentPage) {
            if (autoPlay) {
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.pause()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && exoPlayer.duration > 0L) {
                    durationMs = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isCurrentPage, isPlaying) {
        while (isCurrentPage) {
            if (!isScrubbing) {
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0L) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(200L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = { onToggleHud() },
                    onDoubleTap = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(
                    R.layout.view_texture_player,
                    null,
                    false,
                ) as PlayerView).apply {
                    player = exoPlayer
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) {
                    view.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Center playback controls (-10s, Play/Pause, +10s)
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                ) {
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            positionMs = target
                        },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    shadowElevation = 10.dp,
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0L)
                                }
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(66.dp)
                            .testTag("video_play_pause_button"),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause video" else "Play video",
                            tint = Color(0xFF1C0C00),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                ) {
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(target)
                            positionMs = target
                        },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        // Video Timeline & Speed/Mute/Loop Dock positioned safely above bottom action bar
        AnimatedVisibility(
            visible = showHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (hasFilmstrip) 156.dp else 94.dp,
                ),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111319).copy(alpha = 0.84f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    val activePos = if (isScrubbing) scrubPositionMs.toLong() else positionMs
                    val maxSlider = durationMs.toFloat().coerceAtLeast(1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Formatters.formatDuration(activePos),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Slider(
                            value = activePos.toFloat().coerceIn(0f, maxSlider),
                            onValueChange = {
                                isScrubbing = true
                                scrubPositionMs = it
                            },
                            onValueChangeFinished = {
                                val target = scrubPositionMs.toLong()
                                exoPlayer.seekTo(target)
                                positionMs = target
                                isScrubbing = false
                            },
                            valueRange = 0f..maxSlider,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        Text(
                            text = Formatters.formatDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = if (isMuted) {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    },
                                    contentDescription = if (isMuted) "Unmute video" else "Mute video",
                                    tint = if (isMuted) {
                                        Color(0xFFF87171)
                                    } else {
                                        Color.White
                                    },
                                )
                            }

                            IconButton(
                                onClick = { isLooping = !isLooping },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    contentDescription = if (isLooping) "Disable loop" else "Enable loop",
                                    tint = if (isLooping) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.55f)
                                    },
                                )
                            }
                        }

                        // Playback speed cycler
                        val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val idx = speeds.indexOf(playbackSpeed)
                                    playbackSpeed = speeds[(idx + 1) % speeds.size]
                                }
                                .minimumInteractiveComponentSize(),
                        ) {
                            Text(
                                text = "${playbackSpeed}×",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerActionItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .testTag(testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun ZoomablePhotoPage(
    photo: Photo,
    isCurrentPage: Boolean,
    onSingleTap: () -> Unit,
    onScaleChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2.5f
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            val maxX = (containerSize.width * (targetScale - 1f)) / 2f
                            val maxY = (containerSize.height * (targetScale - 1f)) / 2f
                            scale = targetScale
                            offset = Offset(
                                x = ((centerX - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxX, maxX),
                                y = ((centerY - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxY, maxY),
                            )
                        }
                    },
                )
            }
            .pointerInput(photo.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val pointerCount = event.changes.count { it.pressed }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (pointerCount >= 2 || scale > 1.02f) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                val newOffset = if (newScale <= 1.02f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
                                    )
                                }
                                scale = newScale
                                offset = newOffset

                                if (pointerCount >= 2 || (scale > 1.02f && panChange != Offset.Zero)) {
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) {
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.contentUri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

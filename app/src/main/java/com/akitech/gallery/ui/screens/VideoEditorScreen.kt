package com.akitech.gallery.ui.screens

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.ui.util.Formatters
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VideoEditTab(val label: String, val icon: ImageVector) {
    TRIM("Trim & Audio", Icons.Rounded.ContentCut),
    ROTATE("Rotate", Icons.Rounded.ScreenRotation),
    FILTER("Filter", Icons.Rounded.AutoFixHigh),
}

private enum class VideoFilterPreset(val label: String) {
    ORIGINAL("Original"),
    NOIR("B&W Noir"),
    VIVID("Vivid"),
    CYAN_COOL("Cyan Cool"),
    WARM_GLOW("Warm Glow"),
}

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    video: Photo,
    onCancel: () -> Unit,
    onSaveVideo: suspend (File) -> Boolean,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(VideoEditTab.TRIM) }
    var selectedFilter by remember { mutableStateOf(VideoFilterPreset.ORIGINAL) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var muteAudio by remember { mutableStateOf(false) }

    var totalDurationMs by remember(video.id) {
        mutableLongStateOf(video.durationMs.coerceAtLeast(1_000L))
    }
    var trimStartMs by remember(video.id) { mutableLongStateOf(0L) }
    var trimEndMs by remember(video.id) { mutableLongStateOf(video.durationMs.coerceAtLeast(1_000L)) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var activeTransformer by remember { mutableStateOf<Transformer?>(null) }
    var activeTempFile by remember { mutableStateOf<File?>(null) }

    BackHandler {
        if (isExporting) {
            activeTransformer?.cancel()
            activeTransformer = null
            activeTempFile?.delete()
            activeTempFile = null
            isExporting = false
        } else {
            onCancel()
        }
    }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    // Build Media3 Effect chain shared by ExoPlayer live preview and Transformer export
    val videoEffects = remember(rotationDegrees, selectedFilter) {
        buildVideoEffects(rotationDegrees = rotationDegrees, filter = selectedFilter)
    }

    LaunchedEffect(video.contentUri, trimStartMs, trimEndMs, videoEffects) {
        val safeEnd = trimEndMs.coerceAtLeast(trimStartMs + 500L)
        val clippedItem = MediaItem.Builder()
            .setUri(video.contentUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartMs)
                    .setEndPositionMs(safeEnd)
                    .build(),
            )
            .build()
        exoPlayer.setVideoEffects(videoEffects)
        exoPlayer.setMediaItem(clippedItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(muteAudio) {
        exoPlayer.volume = if (muteAudio) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val discovered = exoPlayer.duration
                    if (discovered > 0L && video.durationMs <= 0L && totalDurationMs == 1_000L) {
                        totalDurationMs = discovered
                        trimEndMs = discovered
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activeTransformer?.cancel()
            activeTempFile?.delete()
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(200L)
        }
    }

    fun startExport() {
        if (isExporting) return
        isExporting = true
        exportProgress = 0f
        exoPlayer.pause()

        val outFile = File(context.cacheDir, "akitech_export_${System.currentTimeMillis()}.mp4")
        activeTempFile = outFile

        val safeEnd = trimEndMs.coerceAtLeast(trimStartMs + 500L)
        val mediaItem = MediaItem.Builder()
            .setUri(video.contentUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartMs)
                    .setEndPositionMs(safeEnd)
                    .build(),
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(muteAudio)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    activeTransformer = null
                    scope.launch {
                        val saved = onSaveVideo(outFile)
                        outFile.delete()
                        activeTempFile = null
                        isExporting = false
                        if (saved) {
                            onCancel()
                        } else {
                            onError("Could not save exported video to MediaStore")
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    activeTransformer = null
                    outFile.delete()
                    activeTempFile = null
                    isExporting = false
                    onError(exportException.localizedMessage ?: "Video export failed")
                }
            })
            .build()

        activeTransformer = transformer
        transformer.start(editedMediaItem, outFile.absolutePath)

        scope.launch {
            val progressHolder = ProgressHolder()
            while (activeTransformer === transformer) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    exportProgress = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                }
                delay(150L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .testTag("video_editor_screen"),
    ) {
        // Editor Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = {
                    if (isExporting) {
                        activeTransformer?.cancel()
                        activeTransformer = null
                        activeTempFile?.delete()
                        activeTempFile = null
                        isExporting = false
                    } else {
                        onCancel()
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_editor_close_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close video editor",
                    tint = Color.White,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Video Studio",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = "${Formatters.formatDuration(trimEndMs - trimStartMs)} clip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        selectedFilter = VideoFilterPreset.ORIGINAL
                        rotationDegrees = 0
                        muteAudio = false
                        trimStartMs = 0L
                        trimEndMs = totalDurationMs
                    },
                    enabled = !isExporting,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset video edits",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }

                Button(
                    onClick = { startExport() },
                    enabled = !isExporting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("video_editor_export_button"),
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
            }
        }

        // Video Preview Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
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

                // Play/Pause Floating Control
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("video_editor_play_pause"),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                            tint = Color.White,
                        )
                    }
                }

                // Live timestamp pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                ) {
                    val clipLen = (trimEndMs - trimStartMs).coerceAtLeast(0L)
                    Text(
                        text = "${Formatters.formatDuration(currentPosMs)} / ${Formatters.formatDuration(clipLen)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                if (isExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF111319),
                            modifier = Modifier
                                .padding(24.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(22.dp),
                                ),
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Exporting Video… ${(exportProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { exportProgress },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rendering offline on device with Media3 Transformer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Control Dock
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFF111319),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 14.dp, bottom = 14.dp),
            ) {
                when (activeTab) {
                    VideoEditTab.TRIM -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Start: ${Formatters.formatDuration(trimStartMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "End: ${Formatters.formatDuration(trimEndMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            val maxFloat = totalDurationMs.toFloat().coerceAtLeast(1000f)
                            RangeSlider(
                                value = trimStartMs.toFloat()..trimEndMs.toFloat(),
                                onValueChange = { range ->
                                    val minSpan = minOf(500L, totalDurationMs)
                                    val newStart = range.start.toLong().coerceIn(0L, totalDurationMs - minSpan)
                                    val newEnd = range.endInclusive.toLong().coerceIn(newStart + minSpan, totalDurationMs)
                                    trimStartMs = newStart
                                    trimEndMs = newEnd
                                },
                                valueRange = 0f..maxFloat,
                                enabled = !isExporting && totalDurationMs > 500L,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                                ),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (muteAudio) {
                                            Icons.AutoMirrored.Rounded.VolumeOff
                                        } else {
                                            Icons.AutoMirrored.Rounded.VolumeUp
                                        },
                                        contentDescription = null,
                                        tint = if (muteAudio) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Remove Original Audio",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                    )
                                }
                                Switch(
                                    checked = muteAudio,
                                    onCheckedChange = { muteAudio = it },
                                    enabled = !isExporting,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }

                    VideoEditTab.ROTATE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            val angles = listOf(0, 90, 180, 270)
                            angles.forEach { angle ->
                                val selected = rotationDegrees == angle
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color(0xFF1F232E)
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.14f)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .clickable(enabled = !isExporting) { rotationDegrees = angle },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            imageVector = if (angle == 270) {
                                                Icons.AutoMirrored.Rounded.RotateLeft
                                            } else {
                                                Icons.AutoMirrored.Rounded.RotateRight
                                            },
                                            contentDescription = "Rotate $angle degrees",
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White
                                            },
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${angle}°",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VideoEditTab.FILTER -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            VideoFilterPreset.entries.forEach { preset ->
                                val selected = selectedFilter == preset
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color(0xFF1F232E)
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.14f)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .clickable(enabled = !isExporting) { selectedFilter = preset },
                                ) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.88f)
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    VideoEditTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        TextButton(
                            onClick = { activeTab = tab },
                            enabled = !isExporting,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun buildVideoEffects(
    rotationDegrees: Int,
    filter: VideoFilterPreset,
): List<Effect> {
    val effects = mutableListOf<Effect>()
    if (rotationDegrees % 360 != 0) {
        effects.add(
            ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(rotationDegrees.toFloat())
                .build(),
        )
    }
    when (filter) {
        VideoFilterPreset.ORIGINAL -> Unit
        VideoFilterPreset.NOIR -> effects.add(RgbFilter.createGrayscaleFilter())
        VideoFilterPreset.VIVID -> effects.add(Contrast(0.28f))
        VideoFilterPreset.CYAN_COOL -> effects.add(
            RgbAdjustment.Builder()
                .setRedScale(0.85f)
                .setBlueScale(1.22f)
                .build(),
        )
        VideoFilterPreset.WARM_GLOW -> effects.add(
            RgbAdjustment.Builder()
                .setRedScale(1.18f)
                .setBlueScale(0.84f)
                .build(),
        )
    }
    return effects
}

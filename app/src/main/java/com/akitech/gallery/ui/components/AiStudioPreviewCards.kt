package com.akitech.gallery.ui.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.akitech.gallery.R
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.domain.model.AiTaskState
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.GeneratedAudioResult
import com.akitech.gallery.domain.model.GeneratedImageResult
import com.akitech.gallery.domain.model.GeneratedVideoResult
import com.akitech.gallery.domain.model.GeneratedVisionResult
import com.akitech.gallery.ui.util.Formatters
import kotlinx.coroutines.delay

@Composable
fun AiVisionResultCard(
    state: AiTaskState<GeneratedVisionResult>,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AiResultContainer(
        title = "Firebase AI Insights",
        subtitle = AiMediaRepository.MODEL_GEMINI_FLASH,
        modifier = modifier,
    ) {
        when (state) {
            is AiTaskState.Idle -> AiPlaceholderBody(
                icon = Icons.Rounded.AutoAwesome,
                text = "Select any photo or ask a creative question to stream real-time insights from Firebase AI.",
            )
            is AiTaskState.Loading -> AiLoadingBody(status = state.statusMessage)
            is AiTaskState.Error -> AiErrorBody(message = state.message)
            is AiTaskState.Streaming -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.partialText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(14.dp)
                            .testTag("ai_vision_streaming_text"),
                    )
                }
            }
            is AiTaskState.Success -> {
                val result = state.data
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = result.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(14.dp)
                                .testTag("ai_vision_result_text"),
                        )
                    }
                    FilledTonalButton(
                        onClick = { onCopyText(result.text) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_copy_vision_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Insights")
                    }
                }
            }
        }
    }
}

@Composable
fun AiImageResultCard(
    state: AiTaskState<GeneratedImageResult>,
    onSaveToGallery: (GeneratedImageResult) -> Unit,
    onAnimateGeneratedImage: (GeneratedImageResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    AiResultContainer(
        title = "Image Preview",
        subtitle = AiMediaRepository.MODEL_IMAGE_NANO_BANANA,
        modifier = modifier,
    ) {
        when (state) {
            is AiTaskState.Idle -> AiPlaceholderBody(
                icon = Icons.Rounded.AutoAwesome,
                text = "Enter a prompt (and optionally attach a photo) to create or edit an image.",
            )
            is AiTaskState.Loading -> AiLoadingBody(status = state.statusMessage)
            is AiTaskState.Streaming -> AiLoadingBody(status = "Rendering image...")
            is AiTaskState.Error -> AiErrorBody(message = state.message)
            is AiTaskState.Success -> {
                val result = state.data
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        bitmap = result.bitmap.asImageBitmap(),
                        contentDescription = "AI generated image: ${result.prompt}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .testTag("ai_generated_image_preview"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onSaveToGallery(result) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_save_image_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                        FilledTonalButton(
                            onClick = { onAnimateGeneratedImage(result) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_animate_result_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MovieCreation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Animate")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AiVideoResultCard(
    state: AiTaskState<GeneratedVideoResult>,
    onSaveToGallery: (GeneratedVideoResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    AiResultContainer(
        title = "Motion Reel Output",
        subtitle = "${AiMediaRepository.MODEL_GEMINI_FLASH} • MP4",
        modifier = modifier,
    ) {
        when (state) {
            is AiTaskState.Idle -> AiPlaceholderBody(
                icon = Icons.Rounded.MovieCreation,
                text = "Pick a photo and let Firebase AI direct a cinematic 16:9 or 9:16 MP4 motion reel.",
            )
            is AiTaskState.Loading -> AiLoadingBody(status = state.statusMessage)
            is AiTaskState.Streaming -> AiLoadingBody(status = "Encoding video...")
            is AiTaskState.Error -> AiErrorBody(message = state.message)
            is AiTaskState.Success -> {
                val result = state.data
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var isPlaying by remember(result.videoFile) { mutableStateOf(true) }

                val exoPlayer = remember(result.videoFile) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(result.videoFile.toUri()))
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        prepare()
                    }
                }

                DisposableEffect(exoPlayer) {
                    val listener = object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }
                    }
                    exoPlayer.addListener(listener)
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        exoPlayer.removeListener(listener)
                        exoPlayer.release()
                    }
                }

                val ratioFloat = if (result.aspectRatio == AiVideoAspectRatio.PORTRAIT_9_16) {
                    9f / 16f
                } else {
                    16f / 9f
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratioFloat)
                            .clip(RoundedCornerShape(16.dp))
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
                                if (view.player !== exoPlayer) view.player = exoPlayer
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.56f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause video" else "Play video",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    if (!result.directorNote.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "AI Director (${result.motionStyle ?: "motion"}): ${result.directorNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Button(
                        onClick = { onSaveToGallery(result) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_save_video_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Motion Video (${result.aspectRatio.shortLabel})")
                    }
                }
            }
        }
    }
}

@Composable
fun AiAudioResultCard(
    state: AiTaskState<GeneratedAudioResult>,
    onSaveToLibrary: (GeneratedAudioResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    AiResultContainer(
        title = "Studio Voice Audio",
        subtitle = AiMediaRepository.MODEL_AUDIO_TTS,
        modifier = modifier,
    ) {
        when (state) {
            is AiTaskState.Idle -> AiPlaceholderBody(
                icon = Icons.Rounded.GraphicEq,
                text = "Pick a studio voice and enter a script (or tap AI Write Script) to synthesize speech.",
            )
            is AiTaskState.Loading -> AiLoadingBody(status = state.statusMessage)
            is AiTaskState.Streaming -> AiLoadingBody(status = "Synthesizing audio...")
            is AiTaskState.Error -> AiErrorBody(message = state.message)
            is AiTaskState.Success -> {
                val result = state.data
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                var isPlaying by remember(result.audioFile) { mutableStateOf(false) }
                var positionMs by remember(result.audioFile) { mutableLongStateOf(0L) }
                var durationMs by remember(result.audioFile) { mutableLongStateOf(1L) }
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubMs by remember { mutableFloatStateOf(0f) }

                val exoPlayer = remember(result.audioFile) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(result.audioFile.toUri()))
                        prepare()
                    }
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
                        if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        exoPlayer.removeListener(listener)
                        exoPlayer.release()
                    }
                }

                LaunchedEffect(exoPlayer, isPlaying) {
                    while (true) {
                        if (!isScrubbing) {
                            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                            if (exoPlayer.duration > 0L) durationMs = exoPlayer.duration
                        }
                        delay(250L)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
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
                                            .size(52.dp)
                                            .testTag("ai_audio_play_button"),
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${result.voice.label} • ${result.voice.styleHint}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = result.modelId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val activePos = if (isScrubbing) scrubMs.toLong() else positionMs
                            val maxSlider = durationMs.toFloat().coerceAtLeast(1f)
                            Slider(
                                value = activePos.toFloat().coerceIn(0f, maxSlider),
                                onValueChange = {
                                    isScrubbing = true
                                    scrubMs = it
                                },
                                onValueChangeFinished = {
                                    val target = scrubMs.toLong()
                                    exoPlayer.seekTo(target)
                                    positionMs = target
                                    isScrubbing = false
                                },
                                valueRange = 0f..maxSlider,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = Formatters.formatDuration(activePos),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = Formatters.formatDuration(durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onSaveToLibrary(result) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_save_audio_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Audio to Music Library")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiResultContainer(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = shape,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun AiPlaceholderBody(
    icon: ImageVector,
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AiLoadingBody(status: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AiErrorBody(message: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_error_banner"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

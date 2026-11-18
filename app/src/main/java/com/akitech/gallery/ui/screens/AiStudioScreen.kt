package com.akitech.gallery.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiMusicVariant
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiTaskState
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.ui.components.AiAudioResultCard
import com.akitech.gallery.ui.components.AiImageResultCard
import com.akitech.gallery.ui.components.AiVideoResultCard
import com.akitech.gallery.ui.viewmodel.AiStudioViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AiStudioScreen(
    aiViewModel: AiStudioViewModel,
    recentPhotos: List<Photo>,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activeMode by aiViewModel.activeMode.collectAsStateWithLifecycle()

    val imagePrompt by aiViewModel.imagePrompt.collectAsStateWithLifecycle()
    val imageSourceUri by aiViewModel.imageSourceUri.collectAsStateWithLifecycle()
    val imageAspectRatio by aiViewModel.imageAspectRatio.collectAsStateWithLifecycle()
    val imageTaskState by aiViewModel.imageTaskState.collectAsStateWithLifecycle()

    val animateSourceUri by aiViewModel.animateSourceUri.collectAsStateWithLifecycle()
    val animatePrompt by aiViewModel.animatePrompt.collectAsStateWithLifecycle()
    val videoPrompt by aiViewModel.videoPrompt.collectAsStateWithLifecycle()
    val videoAspectRatio by aiViewModel.videoAspectRatio.collectAsStateWithLifecycle()
    val videoDurationSeconds by aiViewModel.videoDurationSeconds.collectAsStateWithLifecycle()
    val videoTaskState by aiViewModel.videoTaskState.collectAsStateWithLifecycle()

    val musicPrompt by aiViewModel.musicPrompt.collectAsStateWithLifecycle()
    val musicMood by aiViewModel.musicMood.collectAsStateWithLifecycle()
    val musicLyrics by aiViewModel.musicLyrics.collectAsStateWithLifecycle()
    val musicVariant by aiViewModel.musicVariant.collectAsStateWithLifecycle()
    val musicTaskState by aiViewModel.musicTaskState.collectAsStateWithLifecycle()

    val imageEditPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) aiViewModel.onImageSourceSelected(uri)
    }

    val animatePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) aiViewModel.onAnimateSourceSelected(uri)
    }

    val galleryOnlyImages = recentPhotos.filter { it.isImage }.take(12)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_studio_screen"),
    ) {
        // Sub-mode switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiStudioMode.entries.forEach { mode ->
                val selected = activeMode == mode
                val icon = when (mode) {
                    AiStudioMode.IMAGE_STUDIO -> Icons.Rounded.AutoAwesome
                    AiStudioMode.ANIMATE_PHOTO -> Icons.Rounded.MovieCreation
                    AiStudioMode.TEXT_TO_VIDEO -> Icons.Rounded.Videocam
                    AiStudioMode.MUSIC_STUDIO -> Icons.Rounded.MusicNote
                }
                FilterChip(
                    selected = selected,
                    onClick = { aiViewModel.setActiveMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("ai_mode_${mode.name.lowercase()}"),
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isTwoPane = maxWidth >= 840.dp

            val controlsContent: @Composable () -> Unit = {
                when (activeMode) {
                    AiStudioMode.IMAGE_STUDIO -> ImageStudioControls(
                        prompt = imagePrompt,
                        sourceUri = imageSourceUri,
                        aspectRatio = imageAspectRatio,
                        isLoading = imageTaskState is AiTaskState.Loading,
                        recentImages = galleryOnlyImages,
                        onPromptChange = aiViewModel::onImagePromptChanged,
                        onPickPhoto = {
                            imageEditPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onSelectGalleryPhoto = { aiViewModel.onImageSourceSelected(it.contentUri) },
                        onClearPhoto = { aiViewModel.onImageSourceSelected(null) },
                        onAspectRatioChange = aiViewModel::onImageAspectRatioChanged,
                        onGenerateClick = aiViewModel::generateOrEditImage,
                    )

                    AiStudioMode.ANIMATE_PHOTO -> AnimatePhotoControls(
                        sourceUri = animateSourceUri,
                        prompt = animatePrompt,
                        aspectRatio = videoAspectRatio,
                        durationSeconds = videoDurationSeconds,
                        isLoading = videoTaskState is AiTaskState.Loading,
                        recentImages = galleryOnlyImages,
                        onPickPhoto = {
                            animatePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onSelectGalleryPhoto = { aiViewModel.onAnimateSourceSelected(it.contentUri) },
                        onClearPhoto = { aiViewModel.onAnimateSourceSelected(null) },
                        onPromptChange = aiViewModel::onAnimatePromptChanged,
                        onAspectRatioChange = aiViewModel::onVideoAspectRatioChanged,
                        onDurationChange = aiViewModel::onVideoDurationChanged,
                        onGenerateClick = aiViewModel::animatePhotoToVideo,
                    )

                    AiStudioMode.TEXT_TO_VIDEO -> TextToVideoControls(
                        prompt = videoPrompt,
                        aspectRatio = videoAspectRatio,
                        durationSeconds = videoDurationSeconds,
                        isLoading = videoTaskState is AiTaskState.Loading,
                        onPromptChange = aiViewModel::onVideoPromptChanged,
                        onAspectRatioChange = aiViewModel::onVideoAspectRatioChanged,
                        onDurationChange = aiViewModel::onVideoDurationChanged,
                        onGenerateClick = aiViewModel::generateTextToVideo,
                    )

                    AiStudioMode.MUSIC_STUDIO -> MusicStudioControls(
                        prompt = musicPrompt,
                        mood = musicMood,
                        lyrics = musicLyrics,
                        variant = musicVariant,
                        isLoading = musicTaskState is AiTaskState.Loading,
                        onPromptChange = aiViewModel::onMusicPromptChanged,
                        onMoodChange = aiViewModel::onMusicMoodChanged,
                        onLyricsChange = aiViewModel::onMusicLyricsChanged,
                        onVariantChange = aiViewModel::onMusicVariantChanged,
                        onGenerateClick = aiViewModel::generateMusic,
                    )
                }
            }

            val previewContent: @Composable () -> Unit = {
                when (activeMode) {
                    AiStudioMode.IMAGE_STUDIO -> AiImageResultCard(
                        state = imageTaskState,
                        onSaveToGallery = aiViewModel::saveGeneratedImageToGallery,
                        onAnimateGeneratedImage = { generated ->
                            val tempFile = File(context.cacheDir, "ai_animate_ref.jpg")
                            FileOutputStream(tempFile).use { out ->
                                generated.bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            aiViewModel.launchWithPhoto(tempFile.toUri(), AiStudioMode.ANIMATE_PHOTO)
                        },
                    )
                    AiStudioMode.ANIMATE_PHOTO,
                    AiStudioMode.TEXT_TO_VIDEO -> AiVideoResultCard(
                        state = videoTaskState,
                        onSaveToGallery = aiViewModel::saveGeneratedVideoToGallery,
                    )
                    AiStudioMode.MUSIC_STUDIO -> AiAudioResultCard(
                        state = musicTaskState,
                        onSaveToLibrary = aiViewModel::saveGeneratedAudioToLibrary,
                    )
                }
            }

            if (isTwoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = bottomContentPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        controlsContent()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = bottomContentPadding),
                    ) {
                        previewContent()
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = bottomContentPadding,
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        controlsContent()
                        previewContent()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageStudioControls(
    prompt: String,
    sourceUri: Uri?,
    aspectRatio: AiImageAspectRatio,
    isLoading: Boolean,
    recentImages: List<Photo>,
    onPromptChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onSelectGalleryPhoto: (Photo) -> Unit,
    onClearPhoto: () -> Unit,
    onAspectRatioChange: (AiImageAspectRatio) -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = if (sourceUri != null) "Edit Photo with AI" else "Create Image from Text",
        modelBadge = "gemini-nano-banana-2.1",
    ) {
        SourcePhotoSelector(
            label = "Source Photo (Optional — attach to edit an existing photo)",
            sourceUri = sourceUri,
            recentImages = recentImages,
            onPickPhoto = onPickPhoto,
            onSelectGalleryPhoto = onSelectGalleryPhoto,
            onClearPhoto = onClearPhoto,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiImageAspectRatio.entries.forEach { ratio ->
                val selected = ratio == aspectRatio
                FilterChip(
                    selected = selected,
                    onClick = { onAspectRatioChange(ratio) },
                    label = { Text(ratio.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = {
                Text(
                    if (sourceUri != null) {
                        "Edit instruction (e.g., golden sunset lighting, remove background clutter)"
                    } else {
                        "Describe the image you want to generate"
                    },
                )
            },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_image_prompt_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = if (sourceUri != null) {
                listOf("Golden hour cinematic glow", "Studio portrait lighting", "Vibrant watercolor illustration")
            } else {
                listOf("Futuristic Ethiopian highlands at sunrise", "Obsidian & solar amber abstract wave", "Photorealistic mountain lake reflection")
            },
            onSelect = onPromptChange,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGenerateClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("ai_image_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (sourceUri != null) "Apply AI Edit" else "Generate Image",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AnimatePhotoControls(
    sourceUri: Uri?,
    prompt: String,
    aspectRatio: AiVideoAspectRatio,
    durationSeconds: Int,
    isLoading: Boolean,
    recentImages: List<Photo>,
    onPickPhoto: () -> Unit,
    onSelectGalleryPhoto: (Photo) -> Unit,
    onClearPhoto: () -> Unit,
    onPromptChange: (String) -> Unit,
    onAspectRatioChange: (AiVideoAspectRatio) -> Unit,
    onDurationChange: (Int) -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = "Animate Photo to Video",
        modelBadge = "gemini-omni-1.2-flash • Interactions",
    ) {
        SourcePhotoSelector(
            label = "Photo to Animate (Required)",
            sourceUri = sourceUri,
            recentImages = recentImages,
            onPickPhoto = onPickPhoto,
            onSelectGalleryPhoto = onSelectGalleryPhoto,
            onClearPhoto = onClearPhoto,
        )

        Spacer(modifier = Modifier.height(14.dp))
        VideoAspectRatioSelector(
            selected = aspectRatio,
            onSelect = onAspectRatioChange,
            testTagPrefix = "animate_ratio",
        )

        Spacer(modifier = Modifier.height(14.dp))
        VideoDurationSlider(
            durationSeconds = durationSeconds,
            onDurationChange = onDurationChange,
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Motion prompt (optional)") },
            placeholder = { Text("Slow drone push-in, gentle breeze, cinematic lighting...") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_animate_prompt_input"),
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGenerateClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("animate_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MovieCreation,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Animate Photo (${aspectRatio.shortLabel})",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TextToVideoControls(
    prompt: String,
    aspectRatio: AiVideoAspectRatio,
    durationSeconds: Int,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onAspectRatioChange: (AiVideoAspectRatio) -> Unit,
    onDurationChange: (Int) -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = "Generate Video from Text",
        modelBadge = "gemini-omni-1.2-flash • Interactions",
    ) {
        VideoAspectRatioSelector(
            selected = aspectRatio,
            onSelect = onAspectRatioChange,
            testTagPrefix = "video_ratio",
        )

        Spacer(modifier = Modifier.height(14.dp))
        VideoDurationSlider(
            durationSeconds = durationSeconds,
            onDurationChange = onDurationChange,
        )

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Describe the video scene, camera movement & sound") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_video_prompt_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = listOf(
                "Cinematic drone sweep over misty mountains at golden hour",
                "Neon rain reflections on a quiet Tokyo street at night",
                "Ocean waves crashing gently against dark volcanic cliffs",
            ),
            onSelect = onPromptChange,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGenerateClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("text_video_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Generate Video (${aspectRatio.shortLabel})",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MusicStudioControls(
    prompt: String,
    mood: String,
    lyrics: String,
    variant: AiMusicVariant,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onLyricsChange: (String) -> Unit,
    onVariantChange: (AiMusicVariant) -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = "Generate Music Track",
        modelBadge = variant.modelId,
    ) {
        Text(
            text = "Track Length & Model",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiMusicVariant.entries.forEach { item ->
                val selected = item == variant
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { onVariantChange(item) }
                        .minimumInteractiveComponentSize()
                        .testTag(
                            if (item == AiMusicVariant.CLIP) "music_model_clip" else "music_model_pro",
                        ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.modelId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Describe sound, instruments, tempo & vocals") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_music_prompt_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = listOf(
                "Upbeat Afrobeats groove with warm brass & log drums",
                "Ambient chill lo-fi synthwave for late night editing",
                "Acoustic guitar & soft strings instrumental",
            ),
            onSelect = onPromptChange,
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = mood,
            onValueChange = onMoodChange,
            label = { Text("Style / BPM / Mood (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = lyrics,
            onValueChange = onLyricsChange,
            label = { Text("Custom lyrics or [Verse]/[Chorus] notes (optional)") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGenerateClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("music_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (variant == AiMusicVariant.CLIP) "Compose 30s Clip" else "Compose Full Track",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SourcePhotoSelector(
    label: String,
    sourceUri: Uri?,
    recentImages: List<Photo>,
    onPickPhoto: () -> Unit,
    onSelectGalleryPhoto: (Photo) -> Unit,
    onClearPhoto: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (sourceUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(sourceUri)
                        .size(720)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected source photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.68f),
                    ) {
                        IconButton(
                            onClick = onClearPhoto,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear selected photo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onPickPhoto,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ai_pick_photo_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload / Choose Photo")
            }
        }

        if (recentImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(
                    items = recentImages,
                    key = { "ai_pick_${it.id}" },
                ) { photo ->
                    val isSelected = sourceUri == photo.contentUri
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelectGalleryPhoto(photo) },
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.contentUri)
                                .size(160)
                                .build(),
                            contentDescription = photo.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoAspectRatioSelector(
    selected: AiVideoAspectRatio,
    onSelect: (AiVideoAspectRatio) -> Unit,
    testTagPrefix: String,
) {
    Column {
        Text(
            text = "Video Aspect Ratio",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiVideoAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == selected
                val tagSuffix = if (ratio == AiVideoAspectRatio.LANDSCAPE_16_9) "16_9" else "9_16"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(ratio) }
                        .testTag("${testTagPrefix}_$tagSuffix"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = ratio.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoDurationSlider(
    durationSeconds: Int,
    onDurationChange: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${durationSeconds}s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = durationSeconds.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = 3f..15f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickIdeaChips(
    ideas: List<String>,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ideas.forEach { idea ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(idea) },
            ) {
                Text(
                    text = idea,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun StudioControlCard(
    title: String,
    modelBadge: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier
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
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = modelBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

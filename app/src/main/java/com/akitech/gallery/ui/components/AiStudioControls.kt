package com.akitech.gallery.ui.components

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.AiVoiceOption
import com.akitech.gallery.domain.model.Photo

@Composable
fun VisionCoachControls(
    prompt: String,
    sourceUri: Uri?,
    isLoading: Boolean,
    recentImages: List<Photo>,
    onPromptChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onSelectGalleryPhoto: (Photo) -> Unit,
    onClearPhoto: () -> Unit,
    onAnalyzeClick: () -> Unit,
) {
    StudioControlCard(
        title = "Vision AI & Photo Coach",
        modelBadge = AiMediaRepository.MODEL_GEMINI_FLASH,
    ) {
        SourcePhotoSelector(
            label = "Photo to Analyze (Optional)",
            sourceUri = sourceUri,
            recentImages = recentImages,
            onPickPhoto = onPickPhoto,
            onSelectGalleryPhoto = onSelectGalleryPhoto,
            onClearPhoto = onClearPhoto,
        )

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = {
                Text(
                    if (sourceUri != null) {
                        "Ask about this photo or leave blank for full analysis"
                    } else {
                        "Ask Gemini anything about photography, editing, or captions"
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
                .testTag("ai_vision_prompt_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = if (sourceUri != null) {
                listOf(
                    "Suggest pro lighting, color & crop edits",
                    "Write 3 engaging social media captions & hashtags",
                    "Describe scene mood and camera framing",
                )
            } else {
                listOf(
                    "Best golden hour portrait camera settings",
                    "Creative color grading recipe for landscapes",
                    "Short cinematic travel video script ideas",
                )
            },
            onSelect = onPromptChange,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onAnalyzeClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("ai_vision_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (sourceUri != null) "Analyze Photo with Firebase AI" else "Ask Firebase AI",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageStudioControls(
    prompt: String,
    sourceUri: Uri?,
    aspectRatio: AiImageAspectRatio,
    isLoading: Boolean,
    isEnhancingPrompt: Boolean,
    recentImages: List<Photo>,
    onPromptChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onSelectGalleryPhoto: (Photo) -> Unit,
    onClearPhoto: () -> Unit,
    onAspectRatioChange: (AiImageAspectRatio) -> Unit,
    onEnhancePrompt: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = if (sourceUri != null) "Edit Photo with AI" else "Create Image from Text",
        modelBadge = AiMediaRepository.MODEL_IMAGE_NANO_BANANA,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Prompt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onEnhancePrompt,
                enabled = !isEnhancingPrompt && !isLoading,
                modifier = Modifier.testTag("ai_enhance_prompt_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isEnhancingPrompt) "Enhancing..." else "Enhance (Flash)")
            }
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = {
                Text(
                    if (sourceUri != null) {
                        "Edit instruction (e.g., golden sunset lighting)"
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
                listOf("Ethiopian highlands at sunrise", "Obsidian & solar amber abstract wave", "Mountain lake reflection")
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
fun AnimatePhotoControls(
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
        title = "AI Motion Director (MP4)",
        modelBadge = AiMediaRepository.MODEL_GEMINI_FLASH,
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
            label = { Text("Camera motion & mood direction (optional)") },
            placeholder = { Text("Slow cinematic push-in, warm golden sunset tone...") },
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

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = listOf(
                "Slow zoom in with warm golden glow",
                "Gentle horizontal pan with cool cyan cinema tone",
                "Dramatic B&W noir push-in",
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
                .testTag("animate_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MovieCreation,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Render Motion Video (${aspectRatio.shortLabel})",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceStudioControls(
    script: String,
    selectedVoice: AiVoiceOption,
    isLoading: Boolean,
    isDraftingScript: Boolean,
    onScriptChange: (String) -> Unit,
    onVoiceSelected: (AiVoiceOption) -> Unit,
    onDraftScriptClick: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    StudioControlCard(
        title = "Studio Voice & Narration",
        modelBadge = AiMediaRepository.MODEL_AUDIO_TTS,
    ) {
        Text(
            text = "Voice Persona",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiVoiceOption.entries.forEach { voice ->
                val selected = voice == selectedVoice
                FilterChip(
                    selected = selected,
                    onClick = { onVoiceSelected(voice) },
                    label = { Text(voice.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.testTag("ai_voice_${voice.name.lowercase()}"),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${selectedVoice.label}: ${selectedVoice.styleHint}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Narration Script",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onDraftScriptClick,
                enabled = !isDraftingScript && !isLoading,
                modifier = Modifier.testTag("ai_draft_script_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isDraftingScript) "Writing..." else "AI Write Script")
            }
        }

        OutlinedTextField(
            value = script,
            onValueChange = onScriptChange,
            label = { Text("Enter narration, story, or expressive voice lines") },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_audio_script_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))
        QuickIdeaChips(
            ideas = listOf(
                "Welcome to AkiTech Gallery, where every frame tells a cinematic story.",
                "[excited] Golden sunrise over Bale Mountains — nature at its finest!",
                "[calm] Take a deep breath and revisit your favorite captured memories.",
            ),
            onSelect = onScriptChange,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onGenerateClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("audio_generate_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Synthesize ${selectedVoice.label} Voice",
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
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.68f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    IconButton(
                        onClick = onClearPhoto,
                        modifier = Modifier.size(48.dp),
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

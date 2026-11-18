package com.akitech.gallery.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiTaskState
import com.akitech.gallery.domain.model.FirebaseConnectionStatus
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.ui.components.AiAudioResultCard
import com.akitech.gallery.ui.components.AiImageResultCard
import com.akitech.gallery.ui.components.AiVideoResultCard
import com.akitech.gallery.ui.components.AiVisionResultCard
import com.akitech.gallery.ui.components.AnimatePhotoControls
import com.akitech.gallery.ui.components.ImageStudioControls
import com.akitech.gallery.ui.components.VisionCoachControls
import com.akitech.gallery.ui.components.VoiceStudioControls
import com.akitech.gallery.ui.viewmodel.AiStudioViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AiStudioScreen(
    aiViewModel: AiStudioViewModel,
    recentPhotos: List<Photo>,
    bottomContentPadding: Dp,
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        aiViewModel.ensureConnectionChecked()
    }

    val connectionStatus by aiViewModel.connectionStatus.collectAsStateWithLifecycle()
    val activeMode by aiViewModel.activeMode.collectAsStateWithLifecycle()

    val visionSourceUri by aiViewModel.visionSourceUri.collectAsStateWithLifecycle()
    val visionPrompt by aiViewModel.visionPrompt.collectAsStateWithLifecycle()
    val visionTaskState by aiViewModel.visionTaskState.collectAsStateWithLifecycle()

    val imagePrompt by aiViewModel.imagePrompt.collectAsStateWithLifecycle()
    val imageSourceUri by aiViewModel.imageSourceUri.collectAsStateWithLifecycle()
    val imageAspectRatio by aiViewModel.imageAspectRatio.collectAsStateWithLifecycle()
    val isEnhancingPrompt by aiViewModel.isEnhancingPrompt.collectAsStateWithLifecycle()
    val imageTaskState by aiViewModel.imageTaskState.collectAsStateWithLifecycle()

    val animateSourceUri by aiViewModel.animateSourceUri.collectAsStateWithLifecycle()
    val animatePrompt by aiViewModel.animatePrompt.collectAsStateWithLifecycle()
    val videoAspectRatio by aiViewModel.videoAspectRatio.collectAsStateWithLifecycle()
    val videoDurationSeconds by aiViewModel.videoDurationSeconds.collectAsStateWithLifecycle()
    val videoTaskState by aiViewModel.videoTaskState.collectAsStateWithLifecycle()

    val audioScript by aiViewModel.audioScript.collectAsStateWithLifecycle()
    val selectedVoice by aiViewModel.selectedVoice.collectAsStateWithLifecycle()
    val isDraftingScript by aiViewModel.isDraftingScript.collectAsStateWithLifecycle()
    val audioTaskState by aiViewModel.audioTaskState.collectAsStateWithLifecycle()

    val visionPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) aiViewModel.onVisionSourceSelected(uri)
    }

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
        FirebaseConnectionBanner(
            status = connectionStatus,
            onVerifyClick = aiViewModel::checkFirebaseConnection,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

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
                    AiStudioMode.VISION_COACH -> Icons.Rounded.Psychology
                    AiStudioMode.IMAGE_STUDIO -> Icons.Rounded.AutoAwesome
                    AiStudioMode.ANIMATE_PHOTO -> Icons.Rounded.MovieCreation
                    AiStudioMode.VOICE_AUDIO -> Icons.Rounded.GraphicEq
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
                    AiStudioMode.VISION_COACH -> VisionCoachControls(
                        prompt = visionPrompt,
                        sourceUri = visionSourceUri,
                        isLoading = visionTaskState is AiTaskState.Loading || visionTaskState is AiTaskState.Streaming,
                        recentImages = galleryOnlyImages,
                        onPromptChange = aiViewModel::onVisionPromptChanged,
                        onPickPhoto = {
                            visionPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onSelectGalleryPhoto = { aiViewModel.onVisionSourceSelected(it.contentUri) },
                        onClearPhoto = { aiViewModel.onVisionSourceSelected(null) },
                        onAnalyzeClick = aiViewModel::runVisionAnalysis,
                    )

                    AiStudioMode.IMAGE_STUDIO -> ImageStudioControls(
                        prompt = imagePrompt,
                        sourceUri = imageSourceUri,
                        aspectRatio = imageAspectRatio,
                        isLoading = imageTaskState is AiTaskState.Loading,
                        isEnhancingPrompt = isEnhancingPrompt,
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
                        onEnhancePrompt = aiViewModel::enhanceImagePrompt,
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

                    AiStudioMode.VOICE_AUDIO -> VoiceStudioControls(
                        script = audioScript,
                        selectedVoice = selectedVoice,
                        isLoading = audioTaskState is AiTaskState.Loading,
                        isDraftingScript = isDraftingScript,
                        onScriptChange = aiViewModel::onAudioScriptChanged,
                        onVoiceSelected = aiViewModel::onVoiceSelected,
                        onDraftScriptClick = aiViewModel::draftVoiceScriptWithGemini,
                        onGenerateClick = aiViewModel::generateVoiceAudio,
                    )
                }
            }

            val previewContent: @Composable () -> Unit = {
                when (activeMode) {
                    AiStudioMode.VISION_COACH -> AiVisionResultCard(
                        state = visionTaskState,
                        onCopyText = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            onShowMessage("Copied AI insights to clipboard")
                        },
                    )
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
                    AiStudioMode.ANIMATE_PHOTO -> AiVideoResultCard(
                        state = videoTaskState,
                        onSaveToGallery = aiViewModel::saveGeneratedVideoToGallery,
                    )
                    AiStudioMode.VOICE_AUDIO -> AiAudioResultCard(
                        state = audioTaskState,
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

@Composable
private fun FirebaseConnectionBanner(
    status: FirebaseConnectionStatus,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        is FirebaseConnectionStatus.Connected -> Color(0xFF10B981).copy(alpha = 0.14f)
        is FirebaseConnectionStatus.Disconnected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when (status) {
        is FirebaseConnectionStatus.Connected -> Color(0xFF10B981).copy(alpha = 0.45f)
        is FirebaseConnectionStatus.Disconnected -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }
    val accentColor = when (status) {
        is FirebaseConnectionStatus.Connected -> Color(0xFF34D399)
        is FirebaseConnectionStatus.Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("firebase_connection_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                when (status) {
                    is FirebaseConnectionStatus.Checking -> {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    is FirebaseConnectionStatus.Connected -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Connected to Firebase",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    is FirebaseConnectionStatus.Disconnected -> {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = "Firebase connection issue",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    is FirebaseConnectionStatus.Unknown -> {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    val title = when (status) {
                        is FirebaseConnectionStatus.Unknown -> "Firebase AI Logic"
                        is FirebaseConnectionStatus.Checking -> "Checking Firebase AI Logic..."
                        is FirebaseConnectionStatus.Connected -> "Connected • ${status.projectId}"
                        is FirebaseConnectionStatus.Disconnected -> "Firebase AI Check Failed"
                    }
                    val subtitle = when (status) {
                        is FirebaseConnectionStatus.Unknown -> "Tap Verify to check connection"
                        is FirebaseConnectionStatus.Checking -> "Verifying App Check & Gemini 3.8..."
                        is FirebaseConnectionStatus.Connected -> "${status.appCheckMode} • ${status.modelId}"
                        is FirebaseConnectionStatus.Disconnected -> status.message
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(
                onClick = onVerifyClick,
                enabled = status !is FirebaseConnectionStatus.Checking,
                modifier = Modifier.testTag("firebase_verify_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Verify Firebase AI connection",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verify")
            }
        }
    }
}

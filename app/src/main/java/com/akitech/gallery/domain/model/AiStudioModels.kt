package com.akitech.gallery.domain.model

import android.graphics.Bitmap
import java.io.File

enum class AiStudioMode(val label: String) {
    IMAGE_STUDIO("Create / Edit"),
    ANIMATE_PHOTO("Animate Photo"),
    TEXT_TO_VIDEO("Text to Video"),
    MUSIC_STUDIO("Music Studio"),
}

enum class AiImageAspectRatio(val label: String) {
    SQUARE_1_1("1:1"),
    LANDSCAPE_16_9("16:9"),
    PORTRAIT_9_16("9:16"),
    STANDARD_4_3("4:3"),
    PORTRAIT_3_4("3:4"),
}

enum class AiVideoAspectRatio(
    val apiValue: String,
    val label: String,
    val shortLabel: String,
) {
    LANDSCAPE_16_9("16:9", "16:9 Landscape", "16:9"),
    PORTRAIT_9_16("9:16", "9:16 Portrait", "9:16"),
}

enum class AiMusicVariant(
    val modelId: String,
    val label: String,
    val subtitle: String,
) {
    CLIP(
        modelId = "lyria-3-clip-preview",
        label = "Clip (≤30s)",
        subtitle = "lyria-3-clip-preview • Fast 30s loop or motif",
    ),
    PRO(
        modelId = "lyria-3-pro-preview",
        label = "Full Track (Pro)",
        subtitle = "lyria-3-pro-preview • Complete multi-section song",
    ),
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data class Loading(val statusMessage: String) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class GeneratedImageResult(
    val bitmap: Bitmap,
    val prompt: String,
    val aspectRatio: AiImageAspectRatio,
    val isEdit: Boolean,
)

data class GeneratedVideoResult(
    val videoFile: File,
    val prompt: String,
    val aspectRatio: AiVideoAspectRatio,
    val isImageAnimation: Boolean,
    val interactionId: String? = null,
)

data class GeneratedAudioResult(
    val audioFile: File,
    val mimeType: String,
    val prompt: String,
    val variant: AiMusicVariant,
    val notesOrLyrics: String? = null,
)

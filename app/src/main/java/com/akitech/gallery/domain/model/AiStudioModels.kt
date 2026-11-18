package com.akitech.gallery.domain.model

import android.graphics.Bitmap
import java.io.File

enum class AiStudioMode(val label: String) {
    VISION_COACH("Vision AI"),
    IMAGE_STUDIO("Create / Edit"),
    ANIMATE_PHOTO("Animate Photo"),
    VOICE_AUDIO("Voice & Audio"),
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

enum class AiVoiceOption(
    val voiceId: String,
    val label: String,
    val styleHint: String,
) {
    CHARON("Charon", "Charon", "Warm documentary narrator"),
    KORE("Kore", "Kore", "Clear studio host"),
    AOEDE("Aoede", "Aoede", "Expressive storyteller"),
    PUCK("Puck", "Puck", "Upbeat & energetic"),
    FENRIR("Fenrir", "Fenrir", "Deep dramatic trailer"),
    ZEPHYR("Zephyr", "Zephyr", "Calm & conversational"),
}

sealed interface FirebaseConnectionStatus {
    data object Unknown : FirebaseConnectionStatus
    data object Checking : FirebaseConnectionStatus
    data class Connected(
        val projectId: String,
        val appCheckMode: String,
        val modelId: String,
    ) : FirebaseConnectionStatus

    data class Disconnected(
        val message: String,
        val projectId: String?,
    ) : FirebaseConnectionStatus
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data class Loading(val statusMessage: String) : AiTaskState<Nothing>
    data class Streaming(val partialText: String) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

data class GeneratedVisionResult(
    val text: String,
    val prompt: String,
    val modelId: String,
    val hasSourcePhoto: Boolean,
)

data class GeneratedImageResult(
    val bitmap: Bitmap,
    val prompt: String,
    val aspectRatio: AiImageAspectRatio,
    val isEdit: Boolean,
)

data class PhotoMotionPlan(
    val motionStyle: String,
    val startScale: Float,
    val endScale: Float,
    val panStartX: Float,
    val panStartY: Float,
    val panEndX: Float,
    val panEndY: Float,
    val colorMood: String,
    val directorNote: String,
)

data class GeneratedVideoResult(
    val videoFile: File,
    val prompt: String,
    val aspectRatio: AiVideoAspectRatio,
    val directorNote: String? = null,
    val motionStyle: String? = null,
)

data class GeneratedAudioResult(
    val audioFile: File,
    val mimeType: String,
    val prompt: String,
    val voice: AiVoiceOption,
    val modelId: String,
)

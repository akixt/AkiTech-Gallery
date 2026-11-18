package com.akitech.gallery.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.akitech.gallery.BuildConfig
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.AiVoiceOption
import com.akitech.gallery.domain.model.FirebaseConnectionStatus
import com.akitech.gallery.domain.model.GeneratedAudioResult
import com.akitech.gallery.domain.model.GeneratedImageResult
import com.akitech.gallery.domain.model.GeneratedVideoResult
import com.akitech.gallery.domain.model.PhotoMotionPlan
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiMediaRepository(
    private val context: Context,
) {
    private val firebaseAi: FirebaseAI by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        Firebase.ai(backend = GenerativeBackend.googleAI())
    }

    private val visionModel: GenerativeModel by lazy {
        firebaseAi.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
        )
    }

    private val motionDirectorModel: GenerativeModel by lazy {
        val motionSchema = Schema.obj(
            properties = mapOf(
                "motionStyle" to Schema.enumeration(
                    values = listOf("zoom_in", "zoom_out", "pan_left", "pan_right", "ken_burns"),
                    description = "Camera movement preset best fitting the photo composition",
                ),
                "startScale" to Schema.double(
                    description = "Starting zoom factor between 1.0 and 1.35",
                ),
                "endScale" to Schema.double(
                    description = "Ending zoom factor between 1.0 and 1.45",
                ),
                "panStartX" to Schema.double(
                    description = "Horizontal start pan ratio between -0.35 and 0.35",
                ),
                "panStartY" to Schema.double(
                    description = "Vertical start pan ratio between -0.35 and 0.35",
                ),
                "panEndX" to Schema.double(
                    description = "Horizontal end pan ratio between -0.35 and 0.35",
                ),
                "panEndY" to Schema.double(
                    description = "Vertical end pan ratio between -0.35 and 0.35",
                ),
                "colorMood" to Schema.enumeration(
                    values = listOf("natural", "warm_gold", "cool_cyan", "noir_cinema"),
                    description = "Color grade mood matching the prompt and image",
                ),
                "directorNote" to Schema.string(
                    description = "Short 1-sentence director summary of camera motion and mood",
                ),
            ),
        )
        firebaseAi.generativeModel(
            modelName = MODEL_GEMINI_FLASH,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = motionSchema
            },
        )
    }

    private val aiCacheDir: File
        get() = context.cacheDir.resolve("ai_studio").apply { mkdirs() }

    suspend fun verifyFirebaseConnection(): FirebaseConnectionStatus = withContext(Dispatchers.IO) {
        val projectId = runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getInstance().options.projectId
        }.getOrNull()

        val appCheckMode = if (BuildConfig.DEBUG) "Debug App Check" else "reCAPTCHA Enterprise"

        try {
            // Verify App Check token can be requested without throwing
            val appCheckError = runCatching {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }.exceptionOrNull()

            if (appCheckError != null) {
                Log.w(TAG, "App Check token warning before AI ping", appCheckError)
            }

            val response = withTimeout(PING_TIMEOUT_MS) {
                visionModel.generateContent("Reply with OK.")
            }

            val reply = response.text?.trim().orEmpty()
            if (reply.isBlank()) {
                return@withContext FirebaseConnectionStatus.Disconnected(
                    message = "Firebase AI Logic returned an empty verification response.",
                    projectId = projectId,
                )
            }

            FirebaseConnectionStatus.Connected(
                projectId = projectId ?: "Configured",
                appCheckMode = appCheckMode,
                modelId = MODEL_GEMINI_FLASH,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic connection check failed", e)
            FirebaseConnectionStatus.Disconnected(
                message = formatFirebaseError(e),
                projectId = projectId,
            )
        }
    }

    fun streamVisionAnalysis(
        prompt: String,
        sourceImageUri: Uri?,
    ): Flow<String> = flow {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty() || sourceImageUri != null) {
            "Enter a question/instruction or select a photo to analyze."
        }
        val effectivePrompt = cleanPrompt.ifEmpty {
            "Analyze this photo in detail: describe the scene, lighting, composition, suggested color/crop edits, and a short social caption."
        }

        val inputBitmap = if (sourceImageUri != null) {
            decodeScaledBitmap(sourceImageUri, maxDimension = 1536)
                ?: throw IllegalStateException("Could not decode the selected photo.")
        } else {
            null
        }

        val stream = if (inputBitmap != null) {
            visionModel.generateContentStream(
                content {
                    image(inputBitmap)
                    text(effectivePrompt)
                },
            )
        } else {
            visionModel.generateContentStream(effectivePrompt)
        }

        withTimeout(VISION_TIMEOUT_MS) {
            stream.collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun enhancePromptWithGemini(
        rawIdea: String,
        targetTask: String,
    ): String = withContext(Dispatchers.IO) {
        val clean = rawIdea.trim()
        require(clean.isNotEmpty()) { "Enter a short idea first so Gemini can enhance it." }
        val response = withTimeout(PING_TIMEOUT_MS) {
            visionModel.generateContent(
                "Rewrite the following user idea into a vivid, concise, high-quality single-paragraph $targetTask prompt (max 60 words, return ONLY the prompt text without quotes):\n\n$clean",
            )
        }
        response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini returned an empty prompt.")
    }

    suspend fun generateOrEditImage(
        prompt: String,
        sourceImageUri: Uri?,
        aspectRatio: AiImageAspectRatio,
    ): GeneratedImageResult = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty()) { "Please enter an image prompt." }

        val mappedAspect = when (aspectRatio) {
            AiImageAspectRatio.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
            AiImageAspectRatio.LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
            AiImageAspectRatio.PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
            AiImageAspectRatio.STANDARD_4_3 -> AspectRatio.LANDSCAPE_4x3
            AiImageAspectRatio.PORTRAIT_3_4 -> AspectRatio.PORTRAIT_3x4
        }

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(mappedAspect)
                .build()
        }

        val model = firebaseAi.generativeModel(
            modelName = MODEL_IMAGE_NANO_BANANA,
            generationConfig = config,
        )

        val inputBitmap = if (sourceImageUri != null) {
            decodeScaledBitmap(sourceImageUri, maxDimension = 1536)
                ?: throw IllegalStateException("Could not read the selected source image.")
        } else {
            null
        }

        val response = withTimeout(IMAGE_TIMEOUT_MS) {
            if (inputBitmap != null) {
                model.generateContent(
                    content {
                        image(inputBitmap)
                        text(cleanPrompt)
                    },
                )
            } else {
                model.generateContent(cleanPrompt)
            }
        }

        val candidateParts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        val imagePart = candidateParts.filterIsInstance<ImagePart>().firstOrNull()
        val outputBitmap = imagePart?.image ?: run {
            val inlineImage = response.inlineDataParts.firstOrNull {
                it.mimeType.startsWith("image/", ignoreCase = true)
            } ?: candidateParts.filterIsInstance<InlineDataPart>().firstOrNull {
                it.mimeType.startsWith("image/", ignoreCase = true)
            }
            inlineImage?.inlineData?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }

        if (outputBitmap == null) {
            val textHint = response.text?.takeIf { it.isNotBlank() }
            throw IllegalStateException(
                textHint ?: "No image was returned by $MODEL_IMAGE_NANO_BANANA.",
            )
        }

        GeneratedImageResult(
            bitmap = outputBitmap,
            prompt = cleanPrompt,
            aspectRatio = aspectRatio,
            isEdit = inputBitmap != null,
        )
    }

    suspend fun animatePhotoWithAiDirector(
        prompt: String,
        sourceImageUri: Uri,
        aspectRatio: AiVideoAspectRatio,
        durationSeconds: Int,
        onProgress: (String) -> Unit,
    ): GeneratedVideoResult = withContext(Dispatchers.IO) {
        val effectivePrompt = prompt.trim().ifEmpty {
            "Smooth cinematic push-in with natural golden lighting."
        }

        onProgress("Loading photo & consulting $MODEL_GEMINI_FLASH via Firebase AI...")
        val bitmap = decodeScaledBitmap(sourceImageUri, maxDimension = 1920)
            ?: throw IllegalStateException("Could not decode the selected photo.")

        val directorResponse = withTimeout(VISION_TIMEOUT_MS) {
            motionDirectorModel.generateContent(
                content {
                    image(bitmap)
                    text(
                        "Direct a smooth Ken Burns motion video for this photo in ${aspectRatio.apiValue} framing for ${durationSeconds}s. User direction: $effectivePrompt",
                    )
                },
            )
        }

        val planJsonText = directorResponse.text
            ?: throw IllegalStateException("Firebase AI Director returned an empty motion plan.")

        val plan = parseMotionPlan(JSONObject(planJsonText))

        onProgress("Rendering ${aspectRatio.shortLabel} MP4 (${plan.motionStyle})...")
        val outFile = File(aiCacheDir, "ai_motion_${System.currentTimeMillis()}.mp4")
        try {
            PhotoMotionVideoEncoder.encode(
                bitmap = bitmap,
                plan = plan,
                aspectRatio = aspectRatio,
                durationSeconds = durationSeconds,
                outputFile = outFile,
                onProgress = { pct ->
                    onProgress("Encoding ${aspectRatio.shortLabel} MP4 ($pct%)...")
                },
            )
        } finally {
            bitmap.recycle()
        }

        if (!outFile.exists() || outFile.length() == 0L) {
            throw IllegalStateException("Encoded MP4 file was empty.")
        }

        GeneratedVideoResult(
            videoFile = outFile,
            prompt = effectivePrompt,
            aspectRatio = aspectRatio,
            directorNote = plan.directorNote,
            motionStyle = plan.motionStyle,
        )
    }

    suspend fun generateVoiceAudio(
        scriptText: String,
        voice: AiVoiceOption,
        onProgress: (String) -> Unit,
    ): GeneratedAudioResult = withContext(Dispatchers.IO) {
        val cleanScript = scriptText.trim()
        require(cleanScript.isNotEmpty()) { "Enter narration or dialogue text to synthesize." }

        onProgress("Synthesizing ${voice.label} audio via $MODEL_AUDIO_TTS...")

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO)
            speechConfig = SpeechConfig(voice = Voice(voice.voiceId))
        }

        val model = firebaseAi.generativeModel(
            modelName = MODEL_AUDIO_TTS,
            generationConfig = config,
        )

        val response = withTimeout(AUDIO_TIMEOUT_MS) {
            model.generateContent(cleanScript)
        }

        val candidateParts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        val audioPart = response.inlineDataParts.firstOrNull {
            it.mimeType.startsWith("audio/", ignoreCase = true)
        } ?: candidateParts.filterIsInstance<InlineDataPart>().firstOrNull {
            it.mimeType.startsWith("audio/", ignoreCase = true)
        } ?: response.inlineDataParts.firstOrNull()

        if (audioPart == null || audioPart.inlineData.isEmpty()) {
            val textFeedback = response.text?.takeIf { it.isNotBlank() }
            throw IllegalStateException(
                textFeedback ?: "No audio data was returned by $MODEL_AUDIO_TTS.",
            )
        }

        val prepared = AudioWavHelper.preparePlayableAudio(
            rawBytes = audioPart.inlineData,
            rawMimeType = audioPart.mimeType,
        )

        val outFile = File(aiCacheDir, "ai_audio_${System.currentTimeMillis()}.${prepared.fileExtension}")
        outFile.writeBytes(prepared.bytes)

        GeneratedAudioResult(
            audioFile = outFile,
            mimeType = prepared.mimeType,
            prompt = cleanScript,
            voice = voice,
            modelId = MODEL_AUDIO_TTS,
        )
    }

    fun parseMotionPlan(json: JSONObject): PhotoMotionPlan {
        val startScale = json.optDouble("startScale", 1.0).toFloat().coerceIn(1.0f, 1.4f)
        val endScale = json.optDouble("endScale", 1.24).toFloat().coerceIn(1.0f, 1.5f)
        return PhotoMotionPlan(
            motionStyle = json.optString("motionStyle", "ken_burns"),
            startScale = startScale,
            endScale = endScale,
            panStartX = json.optDouble("panStartX", -0.15).toFloat().coerceIn(-0.35f, 0.35f),
            panStartY = json.optDouble("panStartY", 0.0).toFloat().coerceIn(-0.35f, 0.35f),
            panEndX = json.optDouble("panEndX", 0.15).toFloat().coerceIn(-0.35f, 0.35f),
            panEndY = json.optDouble("panEndY", 0.0).toFloat().coerceIn(-0.35f, 0.35f),
            colorMood = json.optString("colorMood", "natural"),
            directorNote = json.optString("directorNote", "Smooth cinematic Ken Burns motion."),
        )
    }

    fun formatFirebaseError(error: Throwable): String {
        val rawMsg = error.localizedMessage ?: error.message ?: "Firebase AI request failed."
        return when (error) {
            is QuotaExceededException -> {
                "Firebase AI quota or billing tier limit reached: $rawMsg"
            }
            is ServiceDisabledException -> {
                "Firebase AI Logic API is disabled for this project: $rawMsg"
            }
            is ServerException -> {
                "Firebase AI Logic server error: $rawMsg"
            }
            is FirebaseAIException -> {
                if (rawMsg.contains("403") || rawMsg.contains("AppCheck", ignoreCase = true)) {
                    "Firebase App Check or permission check failed (403): $rawMsg"
                } else {
                    rawMsg
                }
            }
            else -> rawMsg
        }
    }

    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        if (maxSide > maxDimension) {
            while (maxSide / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val currentMax = maxOf(decoded.width, decoded.height)
        if (currentMax <= maxDimension) return decoded
        val scale = maxDimension.toFloat() / currentMax.toFloat()
        val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
        val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    companion object {
        private const val TAG = "AiMediaRepository"
        const val MODEL_GEMINI_FLASH = "gemini-3.8-flash"
        const val MODEL_IMAGE_NANO_BANANA = "gemini-nano-banana-2.1"
        const val MODEL_AUDIO_TTS = "gemini-3.1-flash-tts-preview"
        private const val PING_TIMEOUT_MS = 30_000L
        private const val VISION_TIMEOUT_MS = 60_000L
        private const val IMAGE_TIMEOUT_MS = 60_000L
        private const val AUDIO_TIMEOUT_MS = 60_000L
    }
}

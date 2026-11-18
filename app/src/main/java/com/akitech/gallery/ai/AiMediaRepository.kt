package com.akitech.gallery.ai

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiMusicVariant
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.GeneratedAudioResult
import com.akitech.gallery.domain.model.GeneratedImageResult
import com.akitech.gallery.domain.model.GeneratedVideoResult
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiMediaRepository(
    private val context: Context,
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private val aiCacheDir: File
        get() = context.cacheDir.resolve("ai_studio").apply { mkdirs() }

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

        val model = Firebase.ai.generativeModel(
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

    suspend fun generateVideoViaInteractions(
        prompt: String,
        sourceImageUri: Uri?,
        aspectRatio: AiVideoAspectRatio,
        durationSeconds: Int = 8,
        onProgress: (String) -> Unit,
    ): GeneratedVideoResult = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty() || sourceImageUri != null) {
            "Please enter a prompt or select a photo to animate."
        }
        val effectivePrompt = cleanPrompt.ifEmpty {
            "Animate this photo smoothly with cinematic natural motion and realistic lighting."
        }

        val firebaseApp = FirebaseApp.getInstance()
        val apiKey = firebaseApp.options.apiKey
        require(apiKey.isNotBlank()) { "Firebase API key is missing from configuration." }

        val appCheckToken = runCatching {
            FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
        }.getOrNull()

        onProgress(
            if (sourceImageUri != null) {
                "Preparing image for $MODEL_VIDEO_OMNI..."
            } else {
                "Sending request to $MODEL_VIDEO_OMNI..."
            },
        )

        val requestPayload = JSONObject().apply {
            put("model", MODEL_VIDEO_OMNI)
            if (sourceImageUri != null) {
                val bitmap = decodeScaledBitmap(sourceImageUri, maxDimension = 1280)
                    ?: throw IllegalStateException("Could not decode the selected photo.")
                val jpegBytes = ByteArrayOutputStream().use { baos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, baos)
                    baos.toByteArray()
                }
                val encodedImage = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                put(
                    "input",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("type", "image")
                                put("data", encodedImage)
                                put("mime_type", "image/jpeg")
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("type", "text")
                                put("text", effectivePrompt)
                            },
                        )
                    },
                )
            } else {
                put("input", effectivePrompt)
            }
            put(
                "response_format",
                JSONObject().apply {
                    put("type", "video")
                    put("aspect_ratio", aspectRatio.apiValue)
                    put("resolution", "720p")
                    put("duration", "${durationSeconds.coerceIn(3, 15)}s")
                },
            )
        }

        val certSha1 = getAppSignatureSha1()

        val initialJson = withTimeout(VIDEO_TIMEOUT_MS) {
            executeInteractionPost(
                payload = requestPayload,
                apiKey = apiKey,
                appCheckToken = appCheckToken,
                certSha1 = certSha1,
            )
        }

        val interactionId = initialJson.optString("id").takeIf { it.isNotBlank() }
        var currentJson = initialJson

        val finalJson = withTimeout(VIDEO_TIMEOUT_MS) {
            while (true) {
                val status = currentJson.optString("status").lowercase()
                if (status == "failed" || status == "error" || status == "cancelled") {
                    val errMsg = currentJson.optJSONObject("error")?.optString("message")
                        ?: currentJson.optString("status_message")
                        ?: "Video generation failed ($status)."
                    throw IllegalStateException(errMsg)
                }
                if (status == "completed" || status == "succeeded" || hasVideoPayload(currentJson)) {
                    break
                }
                if (interactionId == null) {
                    throw IllegalStateException("Interactions API returned status '$status' without an interaction ID.")
                }
                onProgress("Rendering ${aspectRatio.shortLabel} video on $MODEL_VIDEO_OMNI...")
                delay(3_000L)
                currentJson = executeInteractionGet(
                    interactionId = interactionId,
                    apiKey = apiKey,
                    appCheckToken = appCheckToken,
                    certSha1 = certSha1,
                )
            }
            currentJson
        }

        onProgress("Downloading generated video...")
        val videoBytes = extractVideoBytes(
            responseJson = finalJson,
            apiKey = apiKey,
            appCheckToken = appCheckToken,
            certSha1 = certSha1,
        ) ?: throw IllegalStateException(
            "Interaction completed without a video payload. Response keys: ${finalJson.keys().asSequence().toList()}",
        )

        val outFile = File(aiCacheDir, "ai_video_${System.currentTimeMillis()}.mp4")
        outFile.writeBytes(videoBytes)

        GeneratedVideoResult(
            videoFile = outFile,
            prompt = effectivePrompt,
            aspectRatio = aspectRatio,
            isImageAnimation = sourceImageUri != null,
            interactionId = interactionId,
        )
    }

    suspend fun generateMusic(
        prompt: String,
        genreOrMood: String,
        lyrics: String,
        variant: AiMusicVariant,
        onProgress: (String) -> Unit,
    ): GeneratedAudioResult = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim()
        require(cleanPrompt.isNotEmpty()) { "Please describe the music track you want to create." }

        val fullPrompt = buildString {
            append(cleanPrompt)
            if (genreOrMood.isNotBlank()) {
                append("\nStyle / Mood: ").append(genreOrMood.trim())
            }
            if (lyrics.isNotBlank()) {
                append("\nLyrics:\n").append(lyrics.trim())
            }
        }

        onProgress("Composing with ${variant.modelId}...")

        val config = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO, ResponseModality.TEXT)
        }
        val model = Firebase.ai.generativeModel(
            modelName = variant.modelId,
            generationConfig = config,
        )

        val response = withTimeout(MUSIC_TIMEOUT_MS) {
            model.generateContent(fullPrompt)
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
                textFeedback ?: "No audio data was returned by ${variant.modelId}.",
            )
        }

        val mime = audioPart.mimeType.ifBlank { "audio/mpeg" }
        val ext = when {
            mime.contains("wav", ignoreCase = true) -> "wav"
            mime.contains("aac", ignoreCase = true) -> "aac"
            mime.contains("ogg", ignoreCase = true) -> "ogg"
            else -> "mp3"
        }
        val outFile = File(aiCacheDir, "ai_music_${System.currentTimeMillis()}.$ext")
        outFile.writeBytes(audioPart.inlineData)

        GeneratedAudioResult(
            audioFile = outFile,
            mimeType = mime,
            prompt = cleanPrompt,
            variant = variant,
            notesOrLyrics = response.text?.takeIf { it.isNotBlank() },
        )
    }

    private fun executeInteractionPost(
        payload: JSONObject,
        apiKey: String,
        appCheckToken: String?,
        certSha1: String?,
    ): JSONObject {
        val reqBuilder = Request.Builder()
            .url(INTERACTIONS_ENDPOINT)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("X-Android-Package", context.packageName)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

        if (!appCheckToken.isNullOrBlank()) {
            reqBuilder.addHeader("X-Firebase-AppCheck", appCheckToken)
        }
        if (!certSha1.isNullOrBlank()) {
            reqBuilder.addHeader("X-Android-Cert", certSha1)
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsedError = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                Log.e(TAG, "Interactions POST failed (${response.code}): $raw")
                throw IllegalStateException(
                    parsedError?.takeIf { it.isNotBlank() }
                        ?: "Interactions API error ${response.code}: ${raw.take(240)}",
                )
            }
            return JSONObject(raw)
        }
    }

    private fun executeInteractionGet(
        interactionId: String,
        apiKey: String,
        appCheckToken: String?,
        certSha1: String?,
    ): JSONObject {
        val reqBuilder = Request.Builder()
            .url("$INTERACTIONS_ENDPOINT/$interactionId")
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("X-Android-Package", context.packageName)
            .get()

        if (!appCheckToken.isNullOrBlank()) {
            reqBuilder.addHeader("X-Firebase-AppCheck", appCheckToken)
        }
        if (!certSha1.isNullOrBlank()) {
            reqBuilder.addHeader("X-Android-Cert", certSha1)
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsedError = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                Log.e(TAG, "Interactions GET failed (${response.code}): $raw")
                throw IllegalStateException(
                    parsedError?.takeIf { it.isNotBlank() }
                        ?: "Polling failed (${response.code}): ${raw.take(240)}",
                )
            }
            return JSONObject(raw)
        }
    }

    private fun hasVideoPayload(json: JSONObject): Boolean {
        if (json.optJSONObject("output_video") != null) return true
        val outputs = json.optJSONArray("outputs")
        if (outputs != null && outputs.length() > 0) {
            for (i in 0 until outputs.length()) {
                val item = outputs.optJSONObject(i) ?: continue
                if (item.optString("type").equals("video", ignoreCase = true) ||
                    item.optString("mime_type").startsWith("video/", ignoreCase = true)
                ) {
                    return true
                }
            }
        }
        val steps = json.optJSONArray("steps")
        if (steps != null) {
            for (i in 0 until steps.length()) {
                val step = steps.optJSONObject(i) ?: continue
                val content = step.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val part = content.optJSONObject(j) ?: continue
                    if (part.optString("type").equals("video", ignoreCase = true) ||
                        part.optString("mime_type").startsWith("video/", ignoreCase = true)
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun extractVideoBytes(
        responseJson: JSONObject,
        apiKey: String,
        appCheckToken: String?,
        certSha1: String?,
    ): ByteArray? {
        val candidateObjects = buildList {
            responseJson.optJSONObject("output_video")?.let(::add)
            responseJson.optJSONArray("outputs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let(::add)
                }
            }
            responseJson.optJSONArray("steps")?.let { steps ->
                for (i in 0 until steps.length()) {
                    val step = steps.optJSONObject(i) ?: continue
                    val content = step.optJSONArray("content") ?: continue
                    for (j in 0 until content.length()) {
                        content.optJSONObject(j)?.let(::add)
                    }
                }
            }
        }

        val videoObj = candidateObjects.firstOrNull { obj ->
            obj.optString("type").equals("video", ignoreCase = true) ||
                obj.optString("mime_type").startsWith("video/", ignoreCase = true) ||
                obj.has("data") ||
                obj.has("uri")
        } ?: return null

        val base64Data = videoObj.optString("data").takeIf { it.isNotBlank() }
        if (base64Data != null) {
            return Base64.decode(base64Data, Base64.DEFAULT)
        }

        val downloadUri = videoObj.optString("uri").takeIf { it.isNotBlank() } ?: return null
        val reqBuilder = Request.Builder()
            .url(downloadUri)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("X-Android-Package", context.packageName)
            .get()
        if (!appCheckToken.isNullOrBlank()) {
            reqBuilder.addHeader("X-Firebase-AppCheck", appCheckToken)
        }
        if (!certSha1.isNullOrBlank()) {
            reqBuilder.addHeader("X-Android-Cert", certSha1)
        }

        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Failed to download generated video (HTTP ${resp.code}).")
            }
            return resp.body?.bytes()
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

    @Suppress("DEPRECATION")
    private fun getAppSignatureSha1(): String? {
        return runCatching {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners
            } else {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                info.signatures
            }
            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
            val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
            digest.joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "AiMediaRepository"
        const val MODEL_IMAGE_NANO_BANANA = "gemini-nano-banana-2.1"
        const val MODEL_VIDEO_OMNI = "gemini-omni-1.2-flash"
        const val MODEL_MUSIC_CLIP = "lyria-3-clip-preview"
        const val MODEL_MUSIC_PRO = "lyria-3-pro-preview"
        private const val INTERACTIONS_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/interactions"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val IMAGE_TIMEOUT_MS = 60_000L
        private const val VIDEO_TIMEOUT_MS = 180_000L
        private const val MUSIC_TIMEOUT_MS = 120_000L
    }
}

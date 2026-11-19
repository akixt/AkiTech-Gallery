package com.akitech.gallery.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.akitech.gallery.AkiTechGalleryApp
import com.akitech.gallery.R
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.data.repository.GalleryRepository
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiTaskState
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.AiVoiceOption
import com.akitech.gallery.domain.model.FirebaseConnectionStatus
import com.akitech.gallery.domain.model.GeneratedAudioResult
import com.akitech.gallery.domain.model.GeneratedImageResult
import com.akitech.gallery.domain.model.GeneratedVideoResult
import com.akitech.gallery.domain.model.GeneratedVisionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiStudioViewModel(
    private val app: Application,
    private val aiRepository: AiMediaRepository,
    private val galleryRepository: GalleryRepository,
) : AndroidViewModel(app) {

    private val _connectionStatus = MutableStateFlow<FirebaseConnectionStatus>(FirebaseConnectionStatus.Unknown)
    val connectionStatus: StateFlow<FirebaseConnectionStatus> = _connectionStatus.asStateFlow()

    private val _activeMode = MutableStateFlow(AiStudioMode.VISION_COACH)
    val activeMode: StateFlow<AiStudioMode> = _activeMode.asStateFlow()

    // Vision & Creative Coach state
    private val _visionSourceUri = MutableStateFlow<Uri?>(null)
    val visionSourceUri: StateFlow<Uri?> = _visionSourceUri.asStateFlow()

    private val _visionPrompt = MutableStateFlow("")
    val visionPrompt: StateFlow<String> = _visionPrompt.asStateFlow()

    private val _visionTaskState = MutableStateFlow<AiTaskState<GeneratedVisionResult>>(AiTaskState.Idle)
    val visionTaskState: StateFlow<AiTaskState<GeneratedVisionResult>> = _visionTaskState.asStateFlow()

    // Image Studio state
    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _imageSourceUri = MutableStateFlow<Uri?>(null)
    val imageSourceUri: StateFlow<Uri?> = _imageSourceUri.asStateFlow()

    private val _imageAspectRatio = MutableStateFlow(AiImageAspectRatio.SQUARE_1_1)
    val imageAspectRatio: StateFlow<AiImageAspectRatio> = _imageAspectRatio.asStateFlow()

    private val _isEnhancingPrompt = MutableStateFlow(false)
    val isEnhancingPrompt: StateFlow<Boolean> = _isEnhancingPrompt.asStateFlow()

    private val _imageTaskState = MutableStateFlow<AiTaskState<GeneratedImageResult>>(AiTaskState.Idle)
    val imageTaskState: StateFlow<AiTaskState<GeneratedImageResult>> = _imageTaskState.asStateFlow()

    // Animate Photo (AI Motion Director + MP4 Encoder) state
    private val _animateSourceUri = MutableStateFlow<Uri?>(null)
    val animateSourceUri: StateFlow<Uri?> = _animateSourceUri.asStateFlow()

    private val _animatePrompt = MutableStateFlow("")
    val animatePrompt: StateFlow<String> = _animatePrompt.asStateFlow()

    private val _videoAspectRatio = MutableStateFlow(AiVideoAspectRatio.LANDSCAPE_16_9)
    val videoAspectRatio: StateFlow<AiVideoAspectRatio> = _videoAspectRatio.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(6)
    val videoDurationSeconds: StateFlow<Int> = _videoDurationSeconds.asStateFlow()

    private val _videoTaskState = MutableStateFlow<AiTaskState<GeneratedVideoResult>>(AiTaskState.Idle)
    val videoTaskState: StateFlow<AiTaskState<GeneratedVideoResult>> = _videoTaskState.asStateFlow()

    // Voice & Audio Studio state
    private val _audioScript = MutableStateFlow("")
    val audioScript: StateFlow<String> = _audioScript.asStateFlow()

    private val _selectedVoice = MutableStateFlow(AiVoiceOption.CHARON)
    val selectedVoice: StateFlow<AiVoiceOption> = _selectedVoice.asStateFlow()

    private val _isDraftingScript = MutableStateFlow(false)
    val isDraftingScript: StateFlow<Boolean> = _isDraftingScript.asStateFlow()

    private val _audioTaskState = MutableStateFlow<AiTaskState<GeneratedAudioResult>>(AiTaskState.Idle)
    val audioTaskState: StateFlow<AiTaskState<GeneratedAudioResult>> = _audioTaskState.asStateFlow()

    data class CompletedAiCreation(
        val mode: AiStudioMode,
        val modelId: String,
        val prompt: String,
        val outputSummary: String,
    )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _completedCreations = MutableSharedFlow<CompletedAiCreation>(extraBufferCapacity = 8)
    val completedCreations: SharedFlow<CompletedAiCreation> = _completedCreations.asSharedFlow()

    fun ensureConnectionChecked() {
        if (_connectionStatus.value is FirebaseConnectionStatus.Unknown) {
            checkFirebaseConnection()
        }
    }

    fun checkFirebaseConnection() {
        if (_connectionStatus.value is FirebaseConnectionStatus.Checking) return
        viewModelScope.launch {
            _connectionStatus.value = FirebaseConnectionStatus.Checking
            _connectionStatus.value = aiRepository.verifyFirebaseConnection()
        }
    }

    fun setActiveMode(mode: AiStudioMode) {
        _activeMode.value = mode
    }

    fun loadPromptFromHistory(mode: AiStudioMode, prompt: String) {
        _activeMode.value = mode
        when (mode) {
            AiStudioMode.VISION_COACH -> _visionPrompt.value = prompt
            AiStudioMode.IMAGE_STUDIO -> _imagePrompt.value = prompt
            AiStudioMode.ANIMATE_PHOTO -> _animatePrompt.value = prompt
            AiStudioMode.VOICE_AUDIO -> _audioScript.value = prompt
        }
    }

    fun launchWithPhoto(uri: Uri, targetMode: AiStudioMode) {
        _activeMode.value = targetMode
        when (targetMode) {
            AiStudioMode.VISION_COACH -> _visionSourceUri.value = uri
            AiStudioMode.IMAGE_STUDIO -> _imageSourceUri.value = uri
            AiStudioMode.ANIMATE_PHOTO -> _animateSourceUri.value = uri
            AiStudioMode.VOICE_AUDIO -> Unit
        }
    }

    fun onVisionSourceSelected(uri: Uri?) {
        _visionSourceUri.value = uri
    }

    fun onVisionPromptChanged(value: String) {
        _visionPrompt.value = value
    }

    fun onImagePromptChanged(value: String) {
        _imagePrompt.value = value
    }

    fun onImageSourceSelected(uri: Uri?) {
        _imageSourceUri.value = uri
    }

    fun onImageAspectRatioChanged(ratio: AiImageAspectRatio) {
        _imageAspectRatio.value = ratio
    }

    fun onAnimateSourceSelected(uri: Uri?) {
        _animateSourceUri.value = uri
    }

    fun onAnimatePromptChanged(value: String) {
        _animatePrompt.value = value
    }

    fun onVideoAspectRatioChanged(ratio: AiVideoAspectRatio) {
        _videoAspectRatio.value = ratio
    }

    fun onVideoDurationChanged(seconds: Int) {
        _videoDurationSeconds.value = seconds.coerceIn(3, 15)
    }

    fun onAudioScriptChanged(value: String) {
        _audioScript.value = value
    }

    fun onVoiceSelected(voice: AiVoiceOption) {
        _selectedVoice.value = voice
    }

    fun runVisionAnalysis() {
        val sourceUri = _visionSourceUri.value
        val prompt = _visionPrompt.value.trim()
        if (prompt.isEmpty() && sourceUri == null) {
            _visionTaskState.value = AiTaskState.Error("Enter a question or select a photo to analyze.")
            return
        }

        viewModelScope.launch {
            _visionTaskState.value = AiTaskState.Loading(
                "Connecting to Firebase AI (${AiMediaRepository.MODEL_GEMINI_FLASH})...",
            )
            val accumulated = StringBuilder()
            try {
                aiRepository.streamVisionAnalysis(
                    prompt = prompt,
                    sourceImageUri = sourceUri,
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _visionTaskState.value = AiTaskState.Streaming(accumulated.toString())
                }
                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _visionTaskState.value = AiTaskState.Success(
                        GeneratedVisionResult(
                            text = finalText,
                            prompt = prompt,
                            modelId = AiMediaRepository.MODEL_GEMINI_FLASH,
                            hasSourcePhoto = sourceUri != null,
                        ),
                    )
                    _completedCreations.tryEmit(
                        CompletedAiCreation(
                            mode = AiStudioMode.VISION_COACH,
                            modelId = AiMediaRepository.MODEL_GEMINI_FLASH,
                            prompt = prompt.ifBlank { "Photo scene & edit analysis" },
                            outputSummary = finalText.take(600),
                        ),
                    )
                } else {
                    _visionTaskState.value = AiTaskState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Vision stream timed out", e)
                _visionTaskState.value = AiTaskState.Error("Vision request timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision generation failed", e)
                _visionTaskState.value = AiTaskState.Error(aiRepository.formatFirebaseError(e))
            }
        }
    }

    fun enhanceImagePrompt() {
        val current = _imagePrompt.value.trim()
        if (current.isEmpty()) {
            viewModelScope.launch { _events.emit("Enter a short idea first to enhance it with Gemini.") }
            return
        }
        viewModelScope.launch {
            _isEnhancingPrompt.value = true
            try {
                val enhanced = aiRepository.enhancePromptWithGemini(
                    rawIdea = current,
                    targetTask = "image generation/editing",
                )
                _imagePrompt.value = enhanced
            } catch (e: TimeoutCancellationException) {
                _events.emit("Prompt enhancement timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Prompt enhance failed", e)
                _events.emit(aiRepository.formatFirebaseError(e))
            } finally {
                _isEnhancingPrompt.value = false
            }
        }
    }

    fun generateOrEditImage() {
        val prompt = _imagePrompt.value.trim()
        if (prompt.isEmpty()) {
            _imageTaskState.value = AiTaskState.Error("Enter a prompt to create or edit an image.")
            return
        }
        val sourceUri = _imageSourceUri.value
        val aspectRatio = _imageAspectRatio.value

        viewModelScope.launch {
            _imageTaskState.value = AiTaskState.Loading(
                if (sourceUri != null) {
                    "Editing photo via Firebase AI (${AiMediaRepository.MODEL_IMAGE_NANO_BANANA})..."
                } else {
                    "Generating image via Firebase AI (${AiMediaRepository.MODEL_IMAGE_NANO_BANANA})..."
                },
            )
            try {
                val result = aiRepository.generateOrEditImage(
                    prompt = prompt,
                    sourceImageUri = sourceUri,
                    aspectRatio = aspectRatio,
                )
                _imageTaskState.value = AiTaskState.Success(result)
                _completedCreations.tryEmit(
                    CompletedAiCreation(
                        mode = AiStudioMode.IMAGE_STUDIO,
                        modelId = AiMediaRepository.MODEL_IMAGE_NANO_BANANA,
                        prompt = prompt,
                        outputSummary = "${if (result.isEdit) "Edited photo" else "Generated image"} (${aspectRatio.label})",
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                _imageTaskState.value = AiTaskState.Error("Image generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageTaskState.value = AiTaskState.Error(aiRepository.formatFirebaseError(e))
            }
        }
    }

    fun animatePhotoToVideo() {
        val sourceUri = _animateSourceUri.value
        if (sourceUri == null) {
            _videoTaskState.value = AiTaskState.Error("Select a photo to animate first.")
            return
        }
        val prompt = _animatePrompt.value
        val ratio = _videoAspectRatio.value
        val duration = _videoDurationSeconds.value

        viewModelScope.launch {
            _videoTaskState.value = AiTaskState.Loading(
                "Directing motion plan via Firebase AI (${AiMediaRepository.MODEL_GEMINI_FLASH})...",
            )
            try {
                val result = aiRepository.animatePhotoWithAiDirector(
                    prompt = prompt,
                    sourceImageUri = sourceUri,
                    aspectRatio = ratio,
                    durationSeconds = duration,
                    onProgress = { msg -> _videoTaskState.value = AiTaskState.Loading(msg) },
                )
                _videoTaskState.value = AiTaskState.Success(result)
                _completedCreations.tryEmit(
                    CompletedAiCreation(
                        mode = AiStudioMode.ANIMATE_PHOTO,
                        modelId = AiMediaRepository.MODEL_GEMINI_FLASH,
                        prompt = result.prompt,
                        outputSummary = "${ratio.shortLabel} ${duration}s MP4 (${result.motionStyle ?: "ken_burns"})",
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                _videoTaskState.value = AiTaskState.Error("Photo animation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo animation failed", e)
                _videoTaskState.value = AiTaskState.Error(aiRepository.formatFirebaseError(e))
            }
        }
    }

    fun draftVoiceScriptWithGemini() {
        val topic = _audioScript.value.trim().ifEmpty {
            "A cinematic welcome narration for AkiTech Gallery memories"
        }
        viewModelScope.launch {
            _isDraftingScript.value = true
            try {
                val drafted = aiRepository.enhancePromptWithGemini(
                    rawIdea = topic,
                    targetTask = "expressive 2-sentence voiceover narration script",
                )
                _audioScript.value = drafted
            } catch (e: TimeoutCancellationException) {
                _events.emit("Script drafting timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Script drafting failed", e)
                _events.emit(aiRepository.formatFirebaseError(e))
            } finally {
                _isDraftingScript.value = false
            }
        }
    }

    fun generateVoiceAudio() {
        val script = _audioScript.value.trim()
        if (script.isEmpty()) {
            _audioTaskState.value = AiTaskState.Error("Enter narration or voice text to synthesize.")
            return
        }
        val voice = _selectedVoice.value

        viewModelScope.launch {
            _audioTaskState.value = AiTaskState.Loading(
                "Synthesizing ${voice.label} via Firebase AI (${AiMediaRepository.MODEL_AUDIO_TTS})...",
            )
            try {
                val result = aiRepository.generateVoiceAudio(
                    scriptText = script,
                    voice = voice,
                    onProgress = { msg -> _audioTaskState.value = AiTaskState.Loading(msg) },
                )
                _audioTaskState.value = AiTaskState.Success(result)
                _completedCreations.tryEmit(
                    CompletedAiCreation(
                        mode = AiStudioMode.VOICE_AUDIO,
                        modelId = AiMediaRepository.MODEL_AUDIO_TTS,
                        prompt = script,
                        outputSummary = "Voice: ${voice.label} (${result.mimeType})",
                    ),
                )
            } catch (e: TimeoutCancellationException) {
                _audioTaskState.value = AiTaskState.Error("Audio synthesis timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio synthesis failed", e)
                _audioTaskState.value = AiTaskState.Error(aiRepository.formatFirebaseError(e))
            }
        }
    }

    fun saveGeneratedImageToGallery(result: GeneratedImageResult) {
        viewModelScope.launch {
            val prefix = if (result.isEdit) "AkiTech_AI_Edit" else "AkiTech_AI_Image"
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val uri = galleryRepository.saveEditedImage(result.bitmap, fileName)
            _events.emit(
                if (uri != null) {
                    app.getString(R.string.ai_saved_image)
                } else {
                    "Could not save generated image to gallery"
                },
            )
        }
    }

    fun saveGeneratedVideoToGallery(result: GeneratedVideoResult) {
        viewModelScope.launch {
            val fileName = "AkiTech_Motion_${System.currentTimeMillis()}.mp4"
            val uri = galleryRepository.saveEditedVideo(result.videoFile, fileName)
            _events.emit(
                if (uri != null) {
                    app.getString(R.string.ai_saved_video)
                } else {
                    "Could not save generated video to gallery"
                },
            )
        }
    }

    fun saveGeneratedAudioToLibrary(result: GeneratedAudioResult) {
        viewModelScope.launch {
            val ext = result.audioFile.extension.ifBlank { "wav" }
            val fileName = "AkiTech_Voice_${result.voice.voiceId}_${System.currentTimeMillis()}.$ext"
            val uri = galleryRepository.saveGeneratedAudio(result.audioFile, fileName, result.mimeType)
            _events.emit(
                if (uri != null) {
                    app.getString(R.string.ai_saved_audio)
                } else {
                    "Could not save audio to Music library"
                },
            )
        }
    }

    companion object {
        private const val TAG = "AiStudioViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AkiTechGalleryApp
                AiStudioViewModel(
                    app = app,
                    aiRepository = app.container.aiMediaRepository,
                    galleryRepository = app.container.galleryRepository,
                )
            }
        }
    }
}

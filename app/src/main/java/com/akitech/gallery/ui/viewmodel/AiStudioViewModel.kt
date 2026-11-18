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
import com.akitech.gallery.domain.model.AiMusicVariant
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiTaskState
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.GeneratedAudioResult
import com.akitech.gallery.domain.model.GeneratedImageResult
import com.akitech.gallery.domain.model.GeneratedVideoResult
import com.google.firebase.ai.type.QuotaExceededException
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

    private val _activeMode = MutableStateFlow(AiStudioMode.IMAGE_STUDIO)
    val activeMode: StateFlow<AiStudioMode> = _activeMode.asStateFlow()

    // Image Studio state
    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _imageSourceUri = MutableStateFlow<Uri?>(null)
    val imageSourceUri: StateFlow<Uri?> = _imageSourceUri.asStateFlow()

    private val _imageAspectRatio = MutableStateFlow(AiImageAspectRatio.SQUARE_1_1)
    val imageAspectRatio: StateFlow<AiImageAspectRatio> = _imageAspectRatio.asStateFlow()

    private val _imageTaskState = MutableStateFlow<AiTaskState<GeneratedImageResult>>(AiTaskState.Idle)
    val imageTaskState: StateFlow<AiTaskState<GeneratedImageResult>> = _imageTaskState.asStateFlow()

    // Animate Photo & Text-to-Video state
    private val _animateSourceUri = MutableStateFlow<Uri?>(null)
    val animateSourceUri: StateFlow<Uri?> = _animateSourceUri.asStateFlow()

    private val _animatePrompt = MutableStateFlow("")
    val animatePrompt: StateFlow<String> = _animatePrompt.asStateFlow()

    private val _videoPrompt = MutableStateFlow("")
    val videoPrompt: StateFlow<String> = _videoPrompt.asStateFlow()

    private val _videoAspectRatio = MutableStateFlow(AiVideoAspectRatio.LANDSCAPE_16_9)
    val videoAspectRatio: StateFlow<AiVideoAspectRatio> = _videoAspectRatio.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(8)
    val videoDurationSeconds: StateFlow<Int> = _videoDurationSeconds.asStateFlow()

    private val _videoTaskState = MutableStateFlow<AiTaskState<GeneratedVideoResult>>(AiTaskState.Idle)
    val videoTaskState: StateFlow<AiTaskState<GeneratedVideoResult>> = _videoTaskState.asStateFlow()

    // Music Studio state
    private val _musicPrompt = MutableStateFlow("")
    val musicPrompt: StateFlow<String> = _musicPrompt.asStateFlow()

    private val _musicMood = MutableStateFlow("")
    val musicMood: StateFlow<String> = _musicMood.asStateFlow()

    private val _musicLyrics = MutableStateFlow("")
    val musicLyrics: StateFlow<String> = _musicLyrics.asStateFlow()

    private val _musicVariant = MutableStateFlow(AiMusicVariant.CLIP)
    val musicVariant: StateFlow<AiMusicVariant> = _musicVariant.asStateFlow()

    private val _musicTaskState = MutableStateFlow<AiTaskState<GeneratedAudioResult>>(AiTaskState.Idle)
    val musicTaskState: StateFlow<AiTaskState<GeneratedAudioResult>> = _musicTaskState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun setActiveMode(mode: AiStudioMode) {
        _activeMode.value = mode
    }

    fun launchWithPhoto(uri: Uri, targetMode: AiStudioMode) {
        _activeMode.value = targetMode
        when (targetMode) {
            AiStudioMode.IMAGE_STUDIO -> _imageSourceUri.value = uri
            AiStudioMode.ANIMATE_PHOTO -> _animateSourceUri.value = uri
            else -> Unit
        }
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

    fun onVideoPromptChanged(value: String) {
        _videoPrompt.value = value
    }

    fun onVideoAspectRatioChanged(ratio: AiVideoAspectRatio) {
        _videoAspectRatio.value = ratio
    }

    fun onVideoDurationChanged(seconds: Int) {
        _videoDurationSeconds.value = seconds.coerceIn(3, 15)
    }

    fun onMusicPromptChanged(value: String) {
        _musicPrompt.value = value
    }

    fun onMusicMoodChanged(value: String) {
        _musicMood.value = value
    }

    fun onMusicLyricsChanged(value: String) {
        _musicLyrics.value = value
    }

    fun onMusicVariantChanged(variant: AiMusicVariant) {
        _musicVariant.value = variant
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
                    "Editing image with ${AiMediaRepository.MODEL_IMAGE_NANO_BANANA}..."
                } else {
                    "Generating image with ${AiMediaRepository.MODEL_IMAGE_NANO_BANANA}..."
                },
            )
            try {
                val result = aiRepository.generateOrEditImage(
                    prompt = prompt,
                    sourceImageUri = sourceUri,
                    aspectRatio = aspectRatio,
                )
                _imageTaskState.value = AiTaskState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _imageTaskState.value = AiTaskState.Error("Image generation timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                _imageTaskState.value = AiTaskState.Error("Quota or rate limit exceeded. Please try again shortly.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Image generation failed", e)
                _imageTaskState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Image generation failed.",
                )
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
                "Animating photo (${ratio.shortLabel}) via ${AiMediaRepository.MODEL_VIDEO_OMNI}...",
            )
            try {
                val result = aiRepository.generateVideoViaInteractions(
                    prompt = prompt,
                    sourceImageUri = sourceUri,
                    aspectRatio = ratio,
                    durationSeconds = duration,
                    onProgress = { msg -> _videoTaskState.value = AiTaskState.Loading(msg) },
                )
                _videoTaskState.value = AiTaskState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _videoTaskState.value = AiTaskState.Error("Video animation timed out after 3 minutes.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Photo animation failed", e)
                _videoTaskState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Could not animate photo.",
                )
            }
        }
    }

    fun generateTextToVideo() {
        val prompt = _videoPrompt.value.trim()
        if (prompt.isEmpty()) {
            _videoTaskState.value = AiTaskState.Error("Enter a scene description to generate a video.")
            return
        }
        val ratio = _videoAspectRatio.value
        val duration = _videoDurationSeconds.value

        viewModelScope.launch {
            _videoTaskState.value = AiTaskState.Loading(
                "Generating ${ratio.shortLabel} video via ${AiMediaRepository.MODEL_VIDEO_OMNI}...",
            )
            try {
                val result = aiRepository.generateVideoViaInteractions(
                    prompt = prompt,
                    sourceImageUri = null,
                    aspectRatio = ratio,
                    durationSeconds = duration,
                    onProgress = { msg -> _videoTaskState.value = AiTaskState.Loading(msg) },
                )
                _videoTaskState.value = AiTaskState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _videoTaskState.value = AiTaskState.Error("Video generation timed out after 3 minutes.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Text-to-video failed", e)
                _videoTaskState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Could not generate video.",
                )
            }
        }
    }

    fun generateMusic() {
        val prompt = _musicPrompt.value.trim()
        if (prompt.isEmpty()) {
            _musicTaskState.value = AiTaskState.Error("Describe the track, instruments, or vibe to generate music.")
            return
        }
        val mood = _musicMood.value
        val lyrics = _musicLyrics.value
        val variant = _musicVariant.value

        viewModelScope.launch {
            _musicTaskState.value = AiTaskState.Loading("Generating track with ${variant.modelId}...")
            try {
                val result = aiRepository.generateMusic(
                    prompt = prompt,
                    genreOrMood = mood,
                    lyrics = lyrics,
                    variant = variant,
                    onProgress = { msg -> _musicTaskState.value = AiTaskState.Loading(msg) },
                )
                _musicTaskState.value = AiTaskState.Success(result)
            } catch (e: TimeoutCancellationException) {
                _musicTaskState.value = AiTaskState.Error("Music generation timed out after 2 minutes.")
            } catch (e: QuotaExceededException) {
                _musicTaskState.value = AiTaskState.Error("Quota or rate limit exceeded. Please try again shortly.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Music generation failed", e)
                _musicTaskState.value = AiTaskState.Error(
                    e.localizedMessage ?: "Music generation failed.",
                )
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
            val prefix = if (result.isImageAnimation) "AkiTech_Animate" else "AkiTech_AI_Video"
            val fileName = "${prefix}_${System.currentTimeMillis()}.mp4"
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
            val ext = result.audioFile.extension.ifBlank { "mp3" }
            val fileName = "AkiTech_Music_${System.currentTimeMillis()}.$ext"
            val uri = galleryRepository.saveGeneratedAudio(result.audioFile, fileName, result.mimeType)
            _events.emit(
                if (uri != null) {
                    app.getString(R.string.ai_saved_audio)
                } else {
                    "Could not save track to Music library"
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

package com.akitech.gallery.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.akitech.gallery.R
import com.akitech.gallery.data.remote.CloudAccountRepository
import com.akitech.gallery.domain.model.AiCreationRecord
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.CloudFavorite
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.domain.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repository: CloudAccountRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load cloud profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val cloudFavoritesState: StateFlow<UiState<List<CloudFavorite>>> =
        repository.observeCloudFavorites(currentUserId)
            .map<List<CloudFavorite>, UiState<List<CloudFavorite>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud favorites", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load synced favorites"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val aiCreationsState: StateFlow<UiState<List<AiCreationRecord>>> =
        repository.observeAiCreations(currentUserId)
            .map<List<AiCreationRecord>, UiState<List<AiCreationRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing AI creations", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load AI Studio cloud log"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    fun syncInitialProfile(user: FirebaseUser, settings: AppSettings) {
        viewModelScope.launch {
            val res = repository.ensureUserProfile(user, settings)
            res.exceptionOrNull()?.let { err ->
                _events.emit("Cloud profile sync warning: ${err.localizedMessage ?: "Check Firestore connection"}")
            }
        }
    }

    fun syncPreferences(settings: AppSettings) {
        viewModelScope.launch {
            repository.syncPreferences(currentUserId, settings)
        }
    }

    fun syncFavorite(photo: Photo, isFavoriteNow: Boolean) {
        viewModelScope.launch {
            repository.syncFavorite(currentUserId, photo, isFavoriteNow)
        }
    }

    fun logAiCreation(
        mode: AiStudioMode,
        modelId: String,
        prompt: String,
        outputSummary: String,
    ) {
        viewModelScope.launch {
            val result = repository.logAiCreation(
                userId = currentUserId,
                mode = mode,
                modelId = modelId,
                prompt = prompt,
                outputSummary = outputSummary,
            )
            if (result.isSuccess) {
                _events.emit("Saved AI Studio log to your Google cloud account")
            }
        }
    }

    fun deleteAiCreation(creationId: String) {
        viewModelScope.launch {
            val result = repository.deleteAiCreation(currentUserId, creationId)
            if (result.isFailure) {
                _events.emit("Could not remove AI history item")
            }
        }
    }

    companion object {
        private const val TAG = "AccountViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AccountViewModel(
                    repository = CloudAccountRepository(db),
                    currentUserId = currentUserId,
                )
            }
        }
    }
}

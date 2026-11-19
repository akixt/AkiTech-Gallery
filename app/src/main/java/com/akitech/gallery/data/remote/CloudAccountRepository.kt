package com.akitech.gallery.data.remote

import android.content.Context
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.AiCreationRecord
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.CloudFavorite
import com.akitech.gallery.domain.model.MediaType
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudAccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for profile stream." }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                },
        )
    }

    fun observeCloudFavorites(userId: String): Flow<List<CloudFavorite>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for favorites stream." }
        val colRef = db.collection("users").document(uid).collection("favorites")
        emitAll(
            colRef.orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudFavorite::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                },
        )
    }

    fun observeAiCreations(userId: String): Flow<List<AiCreationRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for AI creations stream." }
        val colRef = db.collection("users").document(uid).collection("aiCreations")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiCreationRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                },
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser, settings: AppSettings): Result<Unit> {
        var docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Signed-in user changed; refusing cross-user profile write." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existingSnapshot = docRef.get().await()
            val safeName = user.displayName?.trim()?.takeIf { it.isNotBlank() }?.take(120)
                ?: user.email?.substringBefore('@')?.take(120)
                ?: "AkiTech Member"
            val safeEmail = user.email?.trim()?.takeIf { it.isNotBlank() }?.take(180)
            val safePhoto = user.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() }?.take(500)

            if (!existingSnapshot.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    email = safeEmail,
                    photoUrl = safePhoto,
                    preferredTheme = settings.themeMode.name,
                    gridColumns = settings.gridColumns.toLong(),
                    gridViewMode = settings.gridViewMode.name,
                )
                docRef.set(profile.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (safeEmail != null) updates["email"] = safeEmail
                if (safePhoto != null) updates["photoUrl"] = safePhoto
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncPreferences(userId: String, settings: AppSettings): Result<Unit> {
        var docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "preferredTheme" to settings.themeMode.name,
                    "gridColumns" to settings.gridColumns.coerceIn(2, 5).toLong(),
                    "gridViewMode" to settings.gridViewMode.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncFavorite(userId: String, photo: Photo, favorite: Boolean): Result<Unit> {
        val docId = "fav_${photo.id.coerceAtLeast(0L)}"
        var docPath = "users/$userId/favorites/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid)
                .collection("favorites").document(docId)
            docPath = docRef.path

            if (!favorite) {
                docRef.delete().await()
                return Result.success(Unit)
            }

            val existing = docRef.get().await()
            val safeDisplayName = photo.displayName.trim().ifBlank { "Media_${photo.id}" }.take(260)
            val safeMediaType = if (photo.isVideo) MediaType.VIDEO.name else MediaType.IMAGE.name
            val safeBucket = photo.bucketDisplayName.trim().take(120)
            val safeSize = photo.sizeBytes.coerceAtLeast(0L)

            if (!existing.exists()) {
                val payload = CloudFavorite(
                    userId = uid,
                    mediaId = photo.id.coerceAtLeast(0L),
                    displayName = safeDisplayName,
                    mediaType = safeMediaType,
                    bucketName = safeBucket,
                    sizeBytes = safeSize,
                ).toCreateMap()
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to safeDisplayName,
                        "mediaType" to safeMediaType,
                        "bucketName" to safeBucket,
                        "sizeBytes" to safeSize,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logAiCreation(
        userId: String,
        mode: AiStudioMode,
        modelId: String,
        prompt: String,
        outputSummary: String,
    ): Result<Unit> {
        val docId = "ai_${System.currentTimeMillis()}"
        var docPath = "users/$userId/aiCreations/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid)
                .collection("aiCreations").document(docId)
            docPath = docRef.path

            val record = AiCreationRecord(
                userId = uid,
                mode = mode.name,
                modelId = modelId,
                prompt = prompt,
                outputSummary = outputSummary,
            )
            docRef.set(record.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAiCreation(userId: String, creationId: String): Result<Unit> {
        var docPath = "users/$userId/aiCreations/$creationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(creationId.isNotBlank() && VALID_ID_REGEX.matches(creationId)) {
                "Invalid AI creation document ID."
            }
            val docRef = db.collection("users").document(uid)
                .collection("aiCreations").document(creationId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private companion object {
        val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}

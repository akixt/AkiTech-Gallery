package com.akitech.gallery.data.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.akitech.gallery.domain.model.MediaType
import com.akitech.gallery.domain.model.Photo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DeleteOutcome {
    data object Deleted : DeleteOutcome
    data class RequiresConfirmation(val intentSender: IntentSender) : DeleteOutcome
    data object RequiresLegacyDialog : DeleteOutcome
    data class Failed(val message: String) : DeleteOutcome
}

class MediaStoreDataSource(
    private val context: Context,
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun observeDeviceMedia(): Flow<List<Photo>> = callbackFlow {
        val triggerFlow = Channel<Unit>(capacity = Channel.CONFLATED)

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                triggerFlow.trySend(Unit)
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver,
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver,
        )

        val job = launch(Dispatchers.IO) {
            trySend(queryMedia())
            for (ignored in triggerFlow) {
                trySend(queryMedia())
            }
        }

        awaitClose {
            job.cancel()
            triggerFlow.close()
            contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    suspend fun queryMedia(): List<Photo> = withContext(Dispatchers.IO) {
        val items = mutableListOf<Photo>()
        val filesUri = MediaStore.Files.getContentUri("external")

        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.DATE_ADDED)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.WIDTH)
            add(MediaStore.Files.FileColumns.HEIGHT)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            add(MediaStore.Files.FileColumns.MEDIA_TYPE)
            add(MediaStore.Images.Media.BUCKET_ID)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            add(MediaStore.Video.VideoColumns.DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC, ${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        contentResolver.query(
            filesUri,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val mediaTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val durationCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val rawMediaType = cursor.getInt(mediaTypeCol)
                val isVideo = rawMediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val uri = ContentUris.withAppendedId(baseUri, id)
                val displayName = cursor.getString(nameCol)?.takeIf { it.isNotBlank() }
                    ?: if (isVideo) "VID_$id.mp4" else "IMG_$id.jpg"
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateModified = cursor.getLong(dateModifiedCol)
                val sizeBytes = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val mimeType = cursor.getString(mimeCol) ?: if (isVideo) "video/mp4" else "image/jpeg"
                val bucketId = if (bucketIdCol >= 0) cursor.getLong(bucketIdCol) else 0L
                val bucketName = if (bucketNameCol >= 0) {
                    cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "Device Media"
                } else {
                    "Device Media"
                }
                val durationMs = if (durationCol >= 0 && isVideo) cursor.getLong(durationCol) else 0L
                val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null

                items += Photo(
                    id = id,
                    contentUri = uri,
                    displayName = displayName,
                    dateAddedSeconds = dateAdded,
                    dateModifiedSeconds = dateModified,
                    sizeBytes = sizeBytes,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    bucketId = bucketId,
                    bucketDisplayName = bucketName,
                    relativePath = relativePath,
                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                    durationMs = durationMs,
                )
            }
        }

        items
    }

    suspend fun saveEditedImage(bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AkiTech Gallery")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                contentResolver.update(uri, publish, null, null)
            }
            uri
        } catch (t: Throwable) {
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun saveEditedVideo(sourceFile: File, fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/AkiTech Gallery")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                contentResolver.update(uri, publish, null, null)
            }
            uri
        } catch (t: Throwable) {
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun saveGeneratedAudio(
        sourceFile: File,
        fileName: String,
        mimeType: String = "audio/mpeg",
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/AkiTech Gallery")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                contentResolver.update(uri, publish, null, null)
            }
            uri
        } catch (t: Throwable) {
            contentResolver.delete(uri, null, null)
            null
        }
    }

    suspend fun requestDelete(uri: Uri): DeleteOutcome = requestDeleteMultiple(listOf(uri))

    suspend fun requestDeleteMultiple(uris: List<Uri>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteOutcome.Deleted
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                DeleteOutcome.RequiresConfirmation(pendingIntent.intentSender)
            } else {
                DeleteOutcome.RequiresLegacyDialog
            }
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                securityException is RecoverableSecurityException
            ) {
                DeleteOutcome.RequiresConfirmation(securityException.userAction.actionIntent.intentSender)
            } else {
                DeleteOutcome.Failed(securityException.localizedMessage ?: "Permission denied")
            }
        } catch (t: Throwable) {
            DeleteOutcome.Failed(t.localizedMessage ?: "Unable to delete media")
        }
    }

    suspend fun performLegacyDelete(uri: Uri): DeleteOutcome = performLegacyDeleteMultiple(listOf(uri))

    suspend fun performLegacyDeleteMultiple(uris: List<Uri>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteOutcome.Deleted
        try {
            var deletedCount = 0
            for (uri in uris) {
                deletedCount += contentResolver.delete(uri, null, null)
            }
            if (deletedCount > 0) DeleteOutcome.Deleted else DeleteOutcome.Failed("Media could not be deleted")
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                securityException is RecoverableSecurityException
            ) {
                DeleteOutcome.RequiresConfirmation(securityException.userAction.actionIntent.intentSender)
            } else {
                DeleteOutcome.Failed(securityException.localizedMessage ?: "Permission denied")
            }
        } catch (t: Throwable) {
            DeleteOutcome.Failed(t.localizedMessage ?: "Unable to delete media")
        }
    }
}

package com.akitech.gallery.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredTheme: String = ThemeMode.DARK.name,
    val gridColumns: Long = 3L,
    val gridViewMode: String = GridViewMode.COMPACT.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.trim().ifBlank { "AkiTech User" }.take(120),
            "email" to email?.trim()?.takeIf { it.isNotBlank() }?.take(180),
            "photoUrl" to photoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "preferredTheme" to preferredTheme.takeIf { it in ALLOWED_THEMES }.orEmpty().ifEmpty { ThemeMode.DARK.name },
            "gridColumns" to gridColumns.coerceIn(2L, 5L),
            "gridViewMode" to gridViewMode.takeIf { it in ALLOWED_VIEW_MODES }.orEmpty().ifEmpty { GridViewMode.COMPACT.name },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    private companion object {
        val ALLOWED_THEMES = setOf("DARK", "SYSTEM", "LIGHT")
        val ALLOWED_VIEW_MODES = setOf("COMPACT", "DETAILED")
    }
}

@Keep
@IgnoreExtraProperties
data class CloudFavorite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mediaId: Long = 0L,
    val displayName: String = "",
    val mediaType: String = MediaType.IMAGE.name,
    val bucketName: String = "",
    val sizeBytes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "mediaId" to mediaId.coerceAtLeast(0L),
        "displayName" to displayName.trim().ifBlank { "Media Item" }.take(260),
        "mediaType" to if (mediaType == MediaType.VIDEO.name) MediaType.VIDEO.name else MediaType.IMAGE.name,
        "bucketName" to bucketName.trim().take(120),
        "sizeBytes" to sizeBytes.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class AiCreationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = AiStudioMode.VISION_COACH.name,
    val modelId: String = "",
    val prompt: String = "",
    val outputSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun safeMode(): AiStudioMode =
        enumValues<AiStudioMode>().firstOrNull { it.name == mode } ?: AiStudioMode.VISION_COACH

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "mode" to safeMode().name,
        "modelId" to modelId.trim().ifBlank { "gemini-3.8-flash" }.take(80),
        "prompt" to prompt.trim().ifBlank { "AI Studio Prompt" }.take(1000),
        "outputSummary" to outputSummary.trim().ifBlank { "Completed" }.take(1500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

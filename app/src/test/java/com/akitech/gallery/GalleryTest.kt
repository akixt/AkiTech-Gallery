package com.akitech.gallery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.ai.AudioWavHelper
import com.akitech.gallery.data.local.FavoriteEntity
import com.akitech.gallery.data.local.GalleryDatabase
import com.akitech.gallery.domain.model.AiCreationRecord
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.AiVoiceOption
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.CloudFavorite
import com.akitech.gallery.domain.model.GalleryTab
import com.akitech.gallery.domain.model.GridDensity
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.MediaFilter
import com.akitech.gallery.domain.model.MediaType
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode
import com.akitech.gallery.domain.model.UserProfile
import com.akitech.gallery.ui.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryTest {

    @Test
    fun `app_name and developer strings match AkiTech Gallery`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AkiTech Gallery", context.getString(R.string.app_name))
        assertEquals("Aklilu Tibebu", context.getString(R.string.developer_name))
        assertEquals("Aki Tech", context.getString(R.string.developer_studio))
        assertEquals("Madda Walabu University", context.getString(R.string.developer_university))
        assertEquals("Information Systems", context.getString(R.string.developer_department))
        assertEquals("4th Year", context.getString(R.string.developer_academic_level))
        assertEquals("Ethiopia", context.getString(R.string.developer_location))
        assertEquals("Aklilu Tibebu / Aki Tech", context.getString(R.string.developer_portfolio))
        assertEquals("akliluasebot@gmail.com", context.getString(R.string.developer_email))
        assertEquals("AI Studio", context.getString(R.string.tab_ai_studio))
    }

    @Test
    fun `formatFileSize formats bytes and megabytes`() {
        assertEquals("512 B", Formatters.formatFileSize(512L))
        assertTrue(Formatters.formatFileSize(2L * 1024L * 1024L).contains("2"))
    }

    @Test
    fun `formatDuration formats short and long durations`() {
        assertEquals("0:00", Formatters.formatDuration(0L))
        assertEquals("0:45", Formatters.formatDuration(45_000L))
        assertEquals("2:05", Formatters.formatDuration(125_000L))
        assertEquals("1:01:05", Formatters.formatDuration(3_665_000L))
    }

    @Test
    fun `formatResolution handles valid and unknown dimensions`() {
        assertEquals("1920 × 1080 px", Formatters.formatResolution(1920, 1080))
        assertEquals("Unknown", Formatters.formatResolution(0, 0))
    }

    @Test
    fun `formatSectionDate groups today and yesterday`() {
        val nowMillis = 1_700_000_000_000L
        assertEquals("Today", Formatters.formatSectionDate(nowMillis / 1000L, nowMillis))
        assertEquals(
            "Yesterday",
            Formatters.formatSectionDate((nowMillis - 24L * 60L * 60L * 1000L) / 1000L, nowMillis),
        )
    }

    @Test
    fun `models expose expected options and defaults`() {
        assertEquals(4, SortOrder.entries.size)
        assertEquals(3, GridDensity.entries.size)
        assertEquals(2, GridViewMode.entries.size)
        assertEquals(5, GalleryTab.entries.size)
        assertEquals(3, MediaFilter.entries.size)
        assertEquals(2, MediaType.entries.size)

        val defaults = AppSettings()
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(GridDensity.STANDARD, defaults.gridDensity)
        assertEquals(GridViewMode.COMPACT, defaults.gridViewMode)
        assertEquals(3, defaults.gridColumns)
        assertEquals(SortOrder.NEWEST, defaults.defaultSortOrder)
        assertTrue(defaults.autoPlayVideos)
        assertTrue(defaults.loopVideos)
        assertTrue(defaults.showMediaBadges)
    }

    @Test
    fun `ai studio models expose supported Firebase AI Logic identifiers and voices`() {
        assertEquals(4, AiStudioMode.entries.size)
        assertEquals(5, AiImageAspectRatio.entries.size)
        assertEquals(2, AiVideoAspectRatio.entries.size)
        assertEquals(6, AiVoiceOption.entries.size)
        assertEquals("16:9", AiVideoAspectRatio.LANDSCAPE_16_9.apiValue)
        assertEquals("9:16", AiVideoAspectRatio.PORTRAIT_9_16.apiValue)
        assertEquals("gemini-3.8-flash", AiMediaRepository.MODEL_GEMINI_FLASH)
        assertEquals("gemini-nano-banana-2.1", AiMediaRepository.MODEL_IMAGE_NANO_BANANA)
        assertEquals("gemini-3.1-flash-tts-preview", AiMediaRepository.MODEL_AUDIO_TTS)
        assertEquals("Charon", AiVoiceOption.CHARON.voiceId)
        assertEquals("Kore", AiVoiceOption.KORE.voiceId)
    }

    @Test
    fun `audio wav helper wraps raw pcm bytes into valid RIFF WAVE header`() {
        val pcmSample = ByteArray(16) { 1 }
        val result = AudioWavHelper.preparePlayableAudio(pcmSample, "audio/L16;rate=24000")
        assertEquals("wav", result.fileExtension)
        assertEquals("audio/wav", result.mimeType)
        assertEquals(44 + pcmSample.size, result.bytes.size)
        assertEquals('R'.code.toByte(), result.bytes[0])
        assertEquals('I'.code.toByte(), result.bytes[1])
        assertEquals('F'.code.toByte(), result.bytes[2])
        assertEquals('F'.code.toByte(), result.bytes[3])
        assertEquals('W'.code.toByte(), result.bytes[8])
        assertEquals('A'.code.toByte(), result.bytes[9])
        assertEquals('V'.code.toByte(), result.bytes[10])
        assertEquals('E'.code.toByte(), result.bytes[11])
    }

    @Test
    fun `cloud models omit id and null optionals and coerce bounds`() {
        val profileMap = UserProfile(
            id = "doc_should_omit",
            userId = "uid_123",
            displayName = "  Aklilu Tibebu  ",
            email = null,
            photoUrl = "   ",
            preferredTheme = "DARK",
            gridColumns = 99L,
            gridViewMode = "DETAILED",
        ).toCreateMap()
        assertFalse(profileMap.containsKey("id"))
        assertFalse(profileMap.containsKey("email"))
        assertFalse(profileMap.containsKey("photoUrl"))
        assertEquals(5L, profileMap["gridColumns"])
        assertEquals("Aklilu Tibebu", profileMap["displayName"])

        val favMap = CloudFavorite(
            id = "fav_42",
            userId = "uid_123",
            mediaId = 42L,
            displayName = "sunset.jpg",
            mediaType = "IMAGE",
            bucketName = "Camera",
            sizeBytes = 1024L,
        ).toCreateMap()
        assertFalse(favMap.containsKey("id"))
        assertEquals(42L, favMap["mediaId"])

        val creation = AiCreationRecord(mode = "UNKNOWN_MODE")
        assertEquals(AiStudioMode.VISION_COACH, creation.safeMode())
    }

    @Test
    fun `gallery database inserts observes and deletes favorites`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = GalleryDatabase.getInstance(context).favoriteDao()
        val sample = FavoriteEntity(
            photoId = 42L,
            contentUri = "content://media/external/images/media/42",
            displayName = "sunset.jpg",
            addedAtMillis = 1_700_000_000_000L,
        )
        dao.insertFavorite(sample)
        assertTrue(dao.isFavorite(42L))
        assertTrue(dao.observeFavoriteIds().first().contains(42L))

        dao.deleteFavoriteById(42L)
        assertFalse(dao.isFavorite(42L))
        assertFalse(dao.observeFavoriteIds().first().contains(42L))
    }
}

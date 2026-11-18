package com.akitech.gallery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.akitech.gallery.ai.AiMediaRepository
import com.akitech.gallery.domain.model.AiImageAspectRatio
import com.akitech.gallery.domain.model.AiMusicVariant
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.AiVideoAspectRatio
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.GalleryTab
import com.akitech.gallery.domain.model.GridDensity
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.MediaFilter
import com.akitech.gallery.domain.model.MediaType
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode
import com.akitech.gallery.ui.util.Formatters
import org.junit.Assert.assertEquals
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
        assertEquals("Aklilu Asebot", context.getString(R.string.developer_name))
        assertEquals("AkiTech Studio", context.getString(R.string.developer_studio))
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
    fun `ai studio models expose required Gemini and Lyria identifiers`() {
        assertEquals(4, AiStudioMode.entries.size)
        assertEquals(5, AiImageAspectRatio.entries.size)
        assertEquals(2, AiVideoAspectRatio.entries.size)
        assertEquals("16:9", AiVideoAspectRatio.LANDSCAPE_16_9.apiValue)
        assertEquals("9:16", AiVideoAspectRatio.PORTRAIT_9_16.apiValue)
        assertEquals("gemini-nano-banana-2.1", AiMediaRepository.MODEL_IMAGE_NANO_BANANA)
        assertEquals("gemini-omni-1.2-flash", AiMediaRepository.MODEL_VIDEO_OMNI)
        assertEquals("lyria-3-clip-preview", AiMusicVariant.CLIP.modelId)
        assertEquals("lyria-3-pro-preview", AiMusicVariant.PRO.modelId)
    }
}

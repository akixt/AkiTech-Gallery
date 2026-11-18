package com.akitech.gallery.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.akitech.gallery.domain.model.ThemeMode

private val AkiTechDarkColorScheme = darkColorScheme(
    primary = BrandAmber,
    onPrimary = Color(0xFF1C0C00),
    primaryContainer = BrandAmberContainerDark,
    onPrimaryContainer = Color(0xFFFFDDA8),
    secondary = BrandSkyBlue,
    onSecondary = Color(0xFF001E2E),
    secondaryContainer = Color(0xFF102A40),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = BrandAmberGlow,
    onTertiary = Color(0xFF261700),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = Color(0xFF450A0A),
)

private val AkiTechLightColorScheme = lightColorScheme(
    primary = BrandAmberDeep,
    onPrimary = Color.White,
    primaryContainer = BrandAmberContainerLight,
    onPrimaryContainer = Color(0xFF3D1700),
    secondary = BrandSkyBlueDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = BrandAmber,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = Color.White,
)

private val AkiTechShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AkiTechGalleryTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    forceDarkSystemBars: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDarkTheme) AkiTechDarkColorScheme else AkiTechLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                val useDarkIcons = !isDarkTheme && !forceDarkSystemBars
                controller.isAppearanceLightStatusBars = useDarkIcons
                controller.isAppearanceLightNavigationBars = useDarkIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AkiTechTypography,
        shapes = AkiTechShapes,
        content = content,
    )
}

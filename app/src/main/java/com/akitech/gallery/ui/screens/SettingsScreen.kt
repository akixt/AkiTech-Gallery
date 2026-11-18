package com.akitech.gallery.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.AppSettings
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode
import com.akitech.gallery.ui.components.DeveloperInfoCard
import com.akitech.gallery.ui.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    cacheSizeBytes: Long,
    bottomContentPadding: Dp,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGridViewModeChange: (GridViewMode) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onAutoPlayVideosChange: (Boolean) -> Unit,
    onLoopVideosChange: (Boolean) -> Unit,
    onShowMediaBadgesChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
        ) {
            // Developer & Brand Spotlight Card
            item {
                DeveloperInfoCard(
                    onEmailClick = {
                        val email = context.getString(R.string.developer_email)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$email")
                            putExtra(Intent.EXTRA_SUBJECT, "AkiTech Gallery Feedback")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
            }

            // Appearance & Grid Settings
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Palette,
                    title = "Appearance & Layout",
                ) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedOptionRow(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        labelOf = { it.label },
                        onSelect = onThemeModeChange,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Grid View Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Compact shows pure visual tiles; Detailed adds file names, dates & sizes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedOptionRow(
                        options = GridViewMode.entries,
                        selected = settings.gridViewMode,
                        labelOf = { it.label },
                        onSelect = onGridViewModeChange,
                        testTagPrefix = "grid_view_mode",
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Photo Grid Columns",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Choose base column count (${settings.gridColumns} columns on phone, expands automatically on tablets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SegmentedOptionRow(
                        options = listOf(2, 3, 4, 5),
                        selected = settings.gridColumns.coerceIn(2, 5),
                        labelOf = { "$it Cols" },
                        onSelect = onGridColumnsChange,
                        testTagPrefix = "grid_columns",
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Default Sort Order",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SortOrder.entries.forEach { order ->
                            val selected = settings.defaultSortOrder == order
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onSortOrderChange(order) },
                            ) {
                                Text(
                                    text = order.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleRow(
                        title = "Show Media Badges",
                        subtitle = "Display video durations, GIF tags & favorite hearts on tiles",
                        checked = settings.showMediaBadges,
                        onCheckedChange = onShowMediaBadgesChange,
                    )
                }
            }

            // Video Player & Formats Card
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Movie,
                    title = "Multi-Format Player & Editors",
                ) {
                    SettingsToggleRow(
                        title = "Auto-Play Videos",
                        subtitle = "Start video playback automatically when opened in viewer",
                        checked = settings.autoPlayVideos,
                        onCheckedChange = onAutoPlayVideosChange,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleRow(
                        title = "Loop Video Playback",
                        subtitle = "Repeat videos continuously in the full-screen player",
                        checked = settings.loopVideos,
                        onCheckedChange = onLoopVideosChange,
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Supported Formats",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val formats = listOf(
                        "JPEG", "PNG", "WebP", "HEIC", "GIF", "BMP",
                        "MP4", "MKV", "WebM", "MOV", "3GP", "AVI",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        formats.forEach { fmt ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = fmt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Cache Card
            item {
                SettingsSectionCard(
                    icon = Icons.Rounded.Storage,
                    title = "Storage & Cache",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thumbnail Disk Cache",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Currently using ${Formatters.formatFileSize(cacheSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(
                            onClick = onClearCache,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("clear_cache_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Clear")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = shape,
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun <T> SegmentedOptionRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    testTagPrefix: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .clickable { onSelect(option) }
                    .let { mod ->
                        if (testTagPrefix != null) {
                            mod.testTag("${testTagPrefix}_${option.toString().lowercase()}")
                        } else {
                            mod
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelOf(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

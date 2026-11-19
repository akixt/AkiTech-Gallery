package com.akitech.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.GridViewMode
import com.akitech.gallery.domain.model.MediaFilter
import com.akitech.gallery.domain.model.SortOrder
import com.akitech.gallery.domain.model.ThemeMode

@Composable
fun GalleryTopBar(
    title: String?,
    searchHint: String,
    searchQuery: String,
    isSearchActive: Boolean,
    themeMode: ThemeMode,
    sortOrder: SortOrder,
    gridColumns: Int,
    gridViewMode: GridViewMode,
    mediaFilter: MediaFilter,
    showSearchControls: Boolean,
    showPhotoControls: Boolean,
    showMediaTypeFilters: Boolean,
    showFavoritesFilter: Boolean,
    favoritesOnly: Boolean,
    itemCountText: String?,
    selectedCount: Int,
    userPhotoUrl: String? = null,
    userDisplayName: String? = null,
    onBackClick: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCycleTheme: () -> Unit,
    onCycleGridColumns: () -> Unit,
    onToggleGridViewMode: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onMediaFilterChange: (MediaFilter) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
                        ),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            AnimatedContent(
                targetState = selectedCount > 0,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "top_bar_mode",
            ) { inSelectionMode ->
                if (inSelectionMode) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp),
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onClearSelection,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("selection_clear_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear selection",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Text(
                                    text = "$selectedCount selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onSelectAll,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SelectAll,
                                        contentDescription = "Select all",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                IconButton(
                                    onClick = onFavoriteSelected,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = "Toggle favorite for selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = onShareSelected,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = stringResource(R.string.cd_share),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                IconButton(
                                    onClick = onDeleteSelected,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.cd_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (onBackClick != null) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_bar_back_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = stringResource(R.string.cd_back),
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .border(
                                            width = 1.2.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(11.dp),
                                        ),
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.akitech_logo),
                                        contentDescription = stringResource(R.string.cd_akitech_logo),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                if (title != null) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                } else {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                append("Aki")
                                            }
                                            withStyle(
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            ) {
                                                append("Tech")
                                            }
                                            withStyle(
                                                SpanStyle(
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                            ) {
                                                append(" Gallery")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showPhotoControls) {
                                val isDetailed = gridViewMode == GridViewMode.DETAILED
                                IconButton(
                                    onClick = onToggleGridViewMode,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("view_mode_toggle_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isDetailed) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                                        contentDescription = "Grid view mode: ${gridViewMode.label}",
                                        tint = if (isDetailed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clickable(onClick = onCycleGridColumns)
                                        .minimumInteractiveComponentSize()
                                        .testTag("grid_columns_cycle_button"),
                                ) {
                                    Text(
                                        text = "${gridColumns}×",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("sort_menu_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                                            contentDescription = "Sort media",
                                            tint = if (sortOrder != SortOrder.NEWEST) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                    ) {
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(order.label) },
                                                trailingIcon = {
                                                    if (order == sortOrder) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    onSortOrderChange(order)
                                                    showSortMenu = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            if (showSearchControls) {
                                IconButton(
                                    onClick = onToggleSearch,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("search_toggle_button"),
                                ) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = if (isSearchActive) {
                                            stringResource(R.string.cd_clear_search)
                                        } else {
                                            stringResource(R.string.cd_search)
                                        },
                                        tint = if (isSearchActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }

                            val themeIcon = when (themeMode) {
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            }
                            IconButton(
                                onClick = onCycleTheme,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("theme_toggle_button"),
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = stringResource(R.string.cd_theme_toggle),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (onOpenSettings != null) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("settings_button"),
                                ) {
                                    if (!userPhotoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = userPhotoUrl,
                                            contentDescription = userDisplayName ?: stringResource(R.string.cd_settings),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                ),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = stringResource(R.string.cd_settings),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchActive && showSearchControls && selectedCount == 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = searchHint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("search_input_field"),
                )
            }

            if ((showMediaTypeFilters || showFavoritesFilter || itemCountText != null) && selectedCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showMediaTypeFilters) {
                        MediaFilter.entries.forEach { filter ->
                            val selected = mediaFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onMediaFilterChange(filter) },
                                label = {
                                    Text(
                                        text = filter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                leadingIcon = when (filter) {
                                    MediaFilter.PHOTOS -> {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                    }
                                    MediaFilter.VIDEOS -> {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.VideoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                            )
                                        }
                                    }
                                    MediaFilter.ALL -> null
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.testTag("media_filter_${filter.name.lowercase()}"),
                            )
                        }
                    }

                    if (showFavoritesFilter) {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = onToggleFavoritesFilter,
                            label = {
                                Text(
                                    text = stringResource(R.string.filter_favorites_only),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (favoritesOnly) {
                                        Icons.Rounded.Favorite
                                    } else {
                                        Icons.Rounded.FavoriteBorder
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.testTag("favorites_filter_chip"),
                        )
                    }

                    if (itemCountText != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = itemCountText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

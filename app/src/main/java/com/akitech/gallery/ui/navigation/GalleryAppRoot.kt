package com.akitech.gallery.ui.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.AiStudioMode
import com.akitech.gallery.domain.model.GalleryTab
import com.akitech.gallery.domain.model.PermissionStatus
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.domain.model.UiState
import com.akitech.gallery.domain.model.ViewerSource
import com.akitech.gallery.ui.components.GalleryTopBar
import com.akitech.gallery.ui.components.LimitedAccessBanner
import com.akitech.gallery.ui.components.PermissionExplanationScreen
import com.akitech.gallery.ui.screens.AiStudioScreen
import com.akitech.gallery.ui.screens.AlbumDetailScreen
import com.akitech.gallery.ui.screens.AlbumsScreen
import com.akitech.gallery.ui.screens.FavoritesScreen
import com.akitech.gallery.ui.screens.PhotoEditorScreen
import com.akitech.gallery.ui.screens.PhotoViewerScreen
import com.akitech.gallery.ui.screens.PhotosScreen
import com.akitech.gallery.ui.screens.SettingsScreen
import com.akitech.gallery.ui.screens.VideoEditorScreen
import com.akitech.gallery.ui.theme.AkiTechGalleryTheme
import com.akitech.gallery.ui.util.PermissionHelper
import com.akitech.gallery.ui.viewmodel.AiStudioViewModel
import com.akitech.gallery.ui.viewmodel.DeleteEvent
import com.akitech.gallery.ui.viewmodel.GalleryViewModel

object GalleryRoutes {
    const val PHOTOS = "photos"
    const val ALBUMS = "albums"
    const val AI_STUDIO = "ai_studio"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val ALBUM_DETAIL = "album/{bucketId}/{albumName}"
    const val VIEWER = "viewer/{source}/{bucketId}/{photoId}"
    const val PHOTO_EDITOR = "edit_photo/{photoId}"
    const val VIDEO_EDITOR = "edit_video/{photoId}"

    fun albumDetail(bucketId: Long, albumName: String): String {
        return "album/$bucketId/${Uri.encode(albumName)}"
    }

    fun viewer(source: ViewerSource, bucketId: Long, photoId: Long): String {
        return "viewer/${source.name}/$bucketId/$photoId"
    }

    fun photoEditor(photoId: Long): String = "edit_photo/$photoId"

    fun videoEditor(photoId: Long): String = "edit_video/$photoId"
}

private data class TopLevelDestination(
    val tab: GalleryTab,
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        tab = GalleryTab.PHOTOS,
        route = GalleryRoutes.PHOTOS,
        labelRes = R.string.tab_photos,
        selectedIcon = Icons.Rounded.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        testTag = "nav_photos",
    ),
    TopLevelDestination(
        tab = GalleryTab.ALBUMS,
        route = GalleryRoutes.ALBUMS,
        labelRes = R.string.tab_albums,
        selectedIcon = Icons.Rounded.FolderCopy,
        unselectedIcon = Icons.Outlined.FolderCopy,
        testTag = "nav_albums",
    ),
    TopLevelDestination(
        tab = GalleryTab.AI_STUDIO,
        route = GalleryRoutes.AI_STUDIO,
        labelRes = R.string.tab_ai_studio,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_ai_studio",
    ),
    TopLevelDestination(
        tab = GalleryTab.FAVORITES,
        route = GalleryRoutes.FAVORITES,
        labelRes = R.string.tab_favorites,
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_favorites",
    ),
    TopLevelDestination(
        tab = GalleryTab.SETTINGS,
        route = GalleryRoutes.SETTINGS,
        labelRes = R.string.tab_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings",
    ),
)

@Composable
fun GalleryAppRoot(
    viewModel: GalleryViewModel,
    aiViewModel: AiStudioViewModel = viewModel(factory = AiStudioViewModel.Factory),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnlyFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val gridViewMode by viewModel.gridViewMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()
    val selectedAlbumBucketId by viewModel.selectedAlbumBucketId.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    val photosUiState by viewModel.filteredPhotosState.collectAsStateWithLifecycle()
    val albumsUiState by viewModel.filteredAlbumsState.collectAsStateWithLifecycle()
    val favoritesUiState by viewModel.favoritePhotosState.collectAsStateWithLifecycle()

    var wasDeniedOnce by rememberSaveable { mutableStateOf(false) }
    var legacyDeleteTargets by remember { mutableStateOf<List<Photo>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.refreshPermissionAndGallery()
        if (PermissionHelper.checkPermissionStatus(context) == PermissionStatus.DENIED) {
            wasDeniedOnce = true
        }
    }

    val deleteSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionAndGallery()
    }

    LaunchedEffect(viewModel) {
        viewModel.deleteEvents.collect { event ->
            when (event) {
                is DeleteEvent.LaunchSystemConfirm -> {
                    deleteSenderLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build(),
                    )
                }
                is DeleteEvent.ShowLegacyConfirm -> {
                    legacyDeleteTargets = event.photos
                }
                is DeleteEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(aiViewModel) {
        aiViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: GalleryRoutes.PHOTOS
    val isFullScreenRoute = currentRoute.startsWith("viewer/") ||
        currentRoute.startsWith("edit_photo/") ||
        currentRoute.startsWith("edit_video/")

    BackHandler(enabled = selectedIds.isNotEmpty() && !isFullScreenRoute) {
        viewModel.clearSelection()
    }

    val sharePhotos: (List<Photo>) -> Unit = { photosToShare ->
        if (photosToShare.size == 1) {
            val photo = photosToShare.first()
            val fallbackMime = if (photo.isVideo) "video/*" else "image/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = photo.mimeType.ifBlank { fallbackMime }
                putExtra(Intent.EXTRA_STREAM, photo.contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.share_chooser_title)),
            )
        } else if (photosToShare.size > 1) {
            val uris = ArrayList(photosToShare.map { it.contentUri })
            val hasMixed = photosToShare.any { it.isVideo } && photosToShare.any { it.isImage }
            val multiType = when {
                hasMixed -> "*/*"
                photosToShare.all { it.isVideo } -> "video/*"
                else -> "image/*"
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = multiType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.share_chooser_title)),
            )
        }
    }

    AkiTechGalleryTheme(
        themeMode = themeMode,
        forceDarkSystemBars = isFullScreenRoute,
    ) {
        if (permissionStatus == PermissionStatus.DENIED || permissionStatus == PermissionStatus.UNKNOWN) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { innerPadding ->
                PermissionExplanationScreen(
                    wasDeniedOnce = wasDeniedOnce,
                    onGrantClick = {
                        permissionLauncher.launch(PermissionHelper.requiredPermissions())
                    },
                    onOpenSettingsClick = {
                        context.startActivity(PermissionHelper.openAppSettingsIntent(context))
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        } else {
            val isTopLevelRoute = currentRoute in setOf(
                GalleryRoutes.PHOTOS,
                GalleryRoutes.ALBUMS,
                GalleryRoutes.AI_STUDIO,
                GalleryRoutes.FAVORITES,
                GalleryRoutes.SETTINGS,
            )

            val navigateTopLevel: (String) -> Unit = { route ->
                viewModel.clearSelection()
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val ambientGlow = MaterialTheme.colorScheme.primary
            val secondaryGlow = MaterialTheme.colorScheme.secondary

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .drawBehind {
                        if (!isFullScreenRoute) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ambientGlow.copy(alpha = 0.11f),
                                        secondaryGlow.copy(alpha = 0.03f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * 0.85f, 0f),
                                    radius = size.minDimension * 0.85f,
                                ),
                                center = Offset(size.width * 0.85f, 0f),
                                radius = size.minDimension * 0.85f,
                            )
                        }
                    },
            ) {
                val useNavRail = maxWidth >= 600.dp
                val bottomGridPadding = if (!useNavRail && isTopLevelRoute) 108.dp else 28.dp

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(
                                bottom = if (!useNavRail && isTopLevelRoute) 88.dp else 8.dp,
                            ),
                        )
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (useNavRail && isTopLevelRoute) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                                    modifier = Modifier.fillMaxHeight(),
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    topLevelDestinations.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes),
                                                )
                                            },
                                            label = {
                                                Text(text = stringResource(dest.labelRes))
                                            },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            ),
                                            modifier = Modifier.testTag(dest.testTag),
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                if (!isFullScreenRoute) {
                                    val topBarTitle = when {
                                        currentRoute.startsWith("album/") ->
                                            Uri.decode(backStackEntry?.arguments?.getString("albumName").orEmpty())
                                        currentRoute == GalleryRoutes.AI_STUDIO -> "AkiTech AI Studio"
                                        currentRoute == GalleryRoutes.SETTINGS -> "Settings & About"
                                        else -> null
                                    }

                                    val searchHint = when {
                                        currentRoute == GalleryRoutes.ALBUMS -> stringResource(R.string.search_hint_albums)
                                        currentRoute == GalleryRoutes.FAVORITES -> stringResource(R.string.search_hint_favorites)
                                        else -> stringResource(R.string.search_hint_photos)
                                    }

                                    val isSettingsRoute = currentRoute == GalleryRoutes.SETTINGS
                                    val isAiStudioRoute = currentRoute == GalleryRoutes.AI_STUDIO
                                    val showSearchControls = !isSettingsRoute && !isAiStudioRoute
                                    val showPhotoControls = currentRoute != GalleryRoutes.ALBUMS &&
                                        !isSettingsRoute &&
                                        !isAiStudioRoute
                                    val showMediaTypeFilters = currentRoute == GalleryRoutes.PHOTOS ||
                                        currentRoute.startsWith("album/")
                                    val showFavoritesChip = currentRoute == GalleryRoutes.PHOTOS ||
                                        currentRoute.startsWith("album/")

                                    val activeBucketId = when {
                                        currentRoute.startsWith("album/") -> backStackEntry?.arguments?.getLong("bucketId")
                                        currentRoute == GalleryRoutes.ALBUMS -> selectedAlbumBucketId
                                        else -> null
                                    }
                                    val activeAlbumPhotosState = if (activeBucketId != null) {
                                        val s by remember(activeBucketId) {
                                            viewModel.observeAlbumPhotos(activeBucketId)
                                        }.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                                        s
                                    } else {
                                        null
                                    }

                                    val countText = when {
                                        currentRoute == GalleryRoutes.PHOTOS ->
                                            (photosUiState as? UiState.Success)?.data?.size?.let { "$it items" }
                                        currentRoute == GalleryRoutes.ALBUMS ->
                                            (albumsUiState as? UiState.Success)?.data?.size?.let { "$it albums" }
                                        currentRoute == GalleryRoutes.FAVORITES ->
                                            (favoritesUiState as? UiState.Success)?.data?.size?.let { "$it saved" }
                                        currentRoute.startsWith("album/") ->
                                            (activeAlbumPhotosState as? UiState.Success)?.data?.size?.let { "$it items" }
                                        else -> null
                                    }

                                    val activeSelectableList: List<Photo> = when {
                                        currentRoute == GalleryRoutes.FAVORITES ->
                                            (favoritesUiState as? UiState.Success)?.data.orEmpty()
                                        activeBucketId != null ->
                                            (activeAlbumPhotosState as? UiState.Success)?.data.orEmpty()
                                        else ->
                                            (photosUiState as? UiState.Success)?.data.orEmpty()
                                    }

                                    GalleryTopBar(
                                        title = topBarTitle,
                                        searchHint = searchHint,
                                        searchQuery = searchQuery,
                                        isSearchActive = isSearchActive,
                                        themeMode = themeMode,
                                        sortOrder = sortOrder,
                                        gridColumns = gridColumns,
                                        gridViewMode = gridViewMode,
                                        mediaFilter = mediaFilter,
                                        showSearchControls = showSearchControls,
                                        showPhotoControls = showPhotoControls,
                                        showMediaTypeFilters = showMediaTypeFilters,
                                        showFavoritesFilter = showFavoritesChip,
                                        favoritesOnly = favoritesOnly,
                                        itemCountText = countText,
                                        selectedCount = selectedIds.size,
                                        onBackClick = if (currentRoute.startsWith("album/")) {
                                            {
                                                viewModel.clearSelection()
                                                navController.popBackStack()
                                            }
                                        } else {
                                            null
                                        },
                                        onOpenSettings = if (!isSettingsRoute) {
                                            { navigateTopLevel(GalleryRoutes.SETTINGS) }
                                        } else {
                                            null
                                        },
                                        onToggleSearch = { viewModel.setSearchActive(!isSearchActive) },
                                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                                        onCycleTheme = viewModel::cycleThemeMode,
                                        onCycleGridColumns = viewModel::cycleGridColumns,
                                        onToggleGridViewMode = viewModel::toggleGridViewMode,
                                        onSortOrderChange = viewModel::setSortOrder,
                                        onMediaFilterChange = viewModel::setMediaFilter,
                                        onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                                        onClearSelection = viewModel::clearSelection,
                                        onSelectAll = { viewModel.selectAll(activeSelectableList) },
                                        onFavoriteSelected = viewModel::favoriteSelectedPhotos,
                                        onShareSelected = {
                                            sharePhotos(viewModel.currentSelectedPhotos())
                                            viewModel.clearSelection()
                                        },
                                        onDeleteSelected = viewModel::deleteSelectedPhotos,
                                    )

                                    if (permissionStatus == PermissionStatus.LIMITED && !isSettingsRoute && !isAiStudioRoute) {
                                        LimitedAccessBanner(
                                            onManageClick = {
                                                permissionLauncher.launch(PermissionHelper.requiredPermissions())
                                            },
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = GalleryRoutes.PHOTOS,
                                        enterTransition = {
                                            fadeIn() + scaleIn(initialScale = 0.97f)
                                        },
                                        exitTransition = {
                                            fadeOut()
                                        },
                                        popEnterTransition = {
                                            fadeIn() + scaleIn(initialScale = 0.97f)
                                        },
                                        popExitTransition = {
                                            fadeOut()
                                        },
                                    ) {
                                        composable(GalleryRoutes.PHOTOS) {
                                            PhotosScreen(
                                                uiState = photosUiState,
                                                searchQuery = searchQuery,
                                                favoritesOnly = favoritesOnly,
                                                sortOrder = sortOrder,
                                                gridColumns = gridColumns,
                                                gridViewMode = gridViewMode,
                                                showMediaBadges = appSettings.showMediaBadges,
                                                selectedIds = selectedIds,
                                                bottomContentPadding = bottomGridPadding,
                                                onPhotoClick = { photo ->
                                                    if (selectedIds.isNotEmpty()) {
                                                        viewModel.togglePhotoSelection(photo.id)
                                                    } else {
                                                        navController.navigate(
                                                            GalleryRoutes.viewer(ViewerSource.ALL_PHOTOS, -1L, photo.id),
                                                        )
                                                    }
                                                },
                                                onPhotoLongClick = { photo ->
                                                    viewModel.togglePhotoSelection(photo.id)
                                                },
                                                onRetry = viewModel::refreshPermissionAndGallery,
                                                onClearFilters = {
                                                    viewModel.setSearchActive(false)
                                                    if (favoritesOnly) viewModel.toggleFavoritesFilter()
                                                },
                                            )
                                        }

                                        composable(GalleryRoutes.ALBUMS) {
                                            val twoPaneAlbumPhotosState = if (selectedAlbumBucketId != null) {
                                                val state by remember(selectedAlbumBucketId) {
                                                    viewModel.observeAlbumPhotos(selectedAlbumBucketId!!)
                                                }.collectAsStateWithLifecycle(initialValue = UiState.Loading)
                                                state
                                            } else {
                                                null
                                            }

                                            AlbumsScreen(
                                                uiState = albumsUiState,
                                                searchQuery = searchQuery,
                                                gridColumns = gridColumns,
                                                gridViewMode = gridViewMode,
                                                showMediaBadges = appSettings.showMediaBadges,
                                                selectedIds = selectedIds,
                                                selectedAlbumBucketId = selectedAlbumBucketId,
                                                selectedAlbumPhotosState = twoPaneAlbumPhotosState,
                                                bottomContentPadding = bottomGridPadding,
                                                onAlbumClick = { album, isTwoPane ->
                                                    if (isTwoPane) {
                                                        viewModel.setSelectedAlbumBucketId(album.bucketId)
                                                    } else {
                                                        navController.navigate(
                                                            GalleryRoutes.albumDetail(album.bucketId, album.name),
                                                        )
                                                    }
                                                },
                                                onPhotoClick = { photo, bucketId ->
                                                    if (selectedIds.isNotEmpty()) {
                                                        viewModel.togglePhotoSelection(photo.id)
                                                    } else {
                                                        navController.navigate(
                                                            GalleryRoutes.viewer(ViewerSource.ALBUM, bucketId, photo.id),
                                                        )
                                                    }
                                                },
                                                onPhotoLongClick = { photo ->
                                                    viewModel.togglePhotoSelection(photo.id)
                                                },
                                                onRetry = viewModel::refreshPermissionAndGallery,
                                                onClearSearch = { viewModel.setSearchActive(false) },
                                            )
                                        }

                                        composable(GalleryRoutes.AI_STUDIO) {
                                            val recentPhotos = (photosUiState as? UiState.Success)?.data.orEmpty()
                                            AiStudioScreen(
                                                aiViewModel = aiViewModel,
                                                recentPhotos = recentPhotos,
                                                bottomContentPadding = bottomGridPadding,
                                            )
                                        }

                                        composable(GalleryRoutes.FAVORITES) {
                                            FavoritesScreen(
                                                uiState = favoritesUiState,
                                                searchQuery = searchQuery,
                                                gridColumns = gridColumns,
                                                gridViewMode = gridViewMode,
                                                showMediaBadges = appSettings.showMediaBadges,
                                                selectedIds = selectedIds,
                                                bottomContentPadding = bottomGridPadding,
                                                onPhotoClick = { photo ->
                                                    if (selectedIds.isNotEmpty()) {
                                                        viewModel.togglePhotoSelection(photo.id)
                                                    } else {
                                                        navController.navigate(
                                                            GalleryRoutes.viewer(ViewerSource.FAVORITES, -1L, photo.id),
                                                        )
                                                    }
                                                },
                                                onPhotoLongClick = { photo ->
                                                    viewModel.togglePhotoSelection(photo.id)
                                                },
                                                onRetry = viewModel::refreshPermissionAndGallery,
                                                onClearSearch = { viewModel.setSearchActive(false) },
                                            )
                                        }

                                        composable(GalleryRoutes.SETTINGS) {
                                            LaunchedEffect(Unit) {
                                                viewModel.refreshCacheSize()
                                            }
                                            SettingsScreen(
                                                settings = appSettings,
                                                cacheSizeBytes = cacheSizeBytes,
                                                bottomContentPadding = bottomGridPadding,
                                                onThemeModeChange = viewModel::setThemeMode,
                                                onGridViewModeChange = viewModel::setGridViewMode,
                                                onGridColumnsChange = viewModel::setGridColumns,
                                                onSortOrderChange = viewModel::setSortOrder,
                                                onAutoPlayVideosChange = viewModel::setAutoPlayVideos,
                                                onLoopVideosChange = viewModel::setLoopVideos,
                                                onShowMediaBadgesChange = viewModel::setShowMediaBadges,
                                                onClearCache = viewModel::clearThumbnailCache,
                                            )
                                        }

                                        composable(
                                            route = GalleryRoutes.ALBUM_DETAIL,
                                            arguments = listOf(
                                                navArgument("bucketId") { type = NavType.LongType },
                                                navArgument("albumName") { type = NavType.StringType },
                                            ),
                                        ) { entry ->
                                            val bucketId = entry.arguments?.getLong("bucketId") ?: -1L
                                            val albumPhotosState by remember(bucketId) {
                                                viewModel.observeAlbumPhotos(bucketId)
                                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                                            AlbumDetailScreen(
                                                uiState = albumPhotosState,
                                                searchQuery = searchQuery,
                                                favoritesOnly = favoritesOnly,
                                                sortOrder = sortOrder,
                                                gridColumns = gridColumns,
                                                gridViewMode = gridViewMode,
                                                showMediaBadges = appSettings.showMediaBadges,
                                                selectedIds = selectedIds,
                                                bottomContentPadding = 28.dp,
                                                onBack = {
                                                    viewModel.clearSelection()
                                                    navController.popBackStack()
                                                },
                                                onPhotoClick = { photo ->
                                                    if (selectedIds.isNotEmpty()) {
                                                        viewModel.togglePhotoSelection(photo.id)
                                                    } else {
                                                        navController.navigate(
                                                            GalleryRoutes.viewer(ViewerSource.ALBUM, bucketId, photo.id),
                                                        )
                                                    }
                                                },
                                                onPhotoLongClick = { photo ->
                                                    viewModel.togglePhotoSelection(photo.id)
                                                },
                                                onRetry = viewModel::refreshPermissionAndGallery,
                                                onClearFilters = {
                                                    viewModel.setSearchActive(false)
                                                    if (favoritesOnly) viewModel.toggleFavoritesFilter()
                                                },
                                            )
                                        }

                                        composable(
                                            route = GalleryRoutes.VIEWER,
                                            arguments = listOf(
                                                navArgument("source") { type = NavType.StringType },
                                                navArgument("bucketId") { type = NavType.LongType },
                                                navArgument("photoId") { type = NavType.LongType },
                                            ),
                                        ) { entry ->
                                            val sourceName = entry.arguments?.getString("source") ?: ViewerSource.ALL_PHOTOS.name
                                            val source = runCatching { ViewerSource.valueOf(sourceName) }
                                                .getOrDefault(ViewerSource.ALL_PHOTOS)
                                            val bucketId = entry.arguments?.getLong("bucketId") ?: -1L
                                            val photoId = entry.arguments?.getLong("photoId") ?: -1L

                                            val viewerPhotos by remember(source, bucketId) {
                                                viewModel.observeViewerPhotos(source, bucketId)
                                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                                            var hasLoadedAny by remember { mutableStateOf(false) }
                                            LaunchedEffect(viewerPhotos) {
                                                if (viewerPhotos.isNotEmpty()) {
                                                    hasLoadedAny = true
                                                } else if (hasLoadedAny) {
                                                    navController.popBackStack()
                                                }
                                            }

                                            if (viewerPhotos.isNotEmpty()) {
                                                PhotoViewerScreen(
                                                    photos = viewerPhotos,
                                                    initialPhotoId = photoId,
                                                    autoPlayVideos = appSettings.autoPlayVideos,
                                                    loopVideos = appSettings.loopVideos,
                                                    onBack = { navController.popBackStack() },
                                                    onToggleFavorite = viewModel::toggleFavorite,
                                                    onSharePhoto = { photo -> sharePhotos(listOf(photo)) },
                                                    onDeletePhoto = viewModel::requestDeletePhoto,
                                                    onEditMedia = { media ->
                                                        val targetRoute = if (media.isVideo) {
                                                            GalleryRoutes.videoEditor(media.id)
                                                        } else {
                                                            GalleryRoutes.photoEditor(media.id)
                                                        }
                                                        navController.navigate(targetRoute)
                                                    },
                                                    onAiStudioPhoto = { media ->
                                                        aiViewModel.launchWithPhoto(
                                                            uri = media.contentUri,
                                                            targetMode = AiStudioMode.IMAGE_STUDIO,
                                                        )
                                                        navController.popBackStack()
                                                        navigateTopLevel(GalleryRoutes.AI_STUDIO)
                                                    },
                                                )
                                            }
                                        }

                                        composable(
                                            route = GalleryRoutes.PHOTO_EDITOR,
                                            arguments = listOf(
                                                navArgument("photoId") { type = NavType.LongType },
                                            ),
                                        ) { entry ->
                                            val photoId = entry.arguments?.getLong("photoId") ?: -1L
                                            val photo = remember(photoId) { viewModel.findMediaById(photoId) }
                                            if (photo == null) {
                                                LaunchedEffect(photoId) { navController.popBackStack() }
                                            } else {
                                                PhotoEditorScreen(
                                                    photo = photo,
                                                    onCancel = { navController.popBackStack() },
                                                    onSaveBitmap = { bitmap ->
                                                        val savedUri = viewModel.saveEditedImage(bitmap, photo.displayName)
                                                        if (savedUri != null) {
                                                            viewModel.notifyMessage(context.getString(R.string.editor_saved_photo))
                                                            true
                                                        } else {
                                                            viewModel.notifyMessage("Could not save edited photo")
                                                            false
                                                        }
                                                    },
                                                )
                                            }
                                        }

                                        composable(
                                            route = GalleryRoutes.VIDEO_EDITOR,
                                            arguments = listOf(
                                                navArgument("photoId") { type = NavType.LongType },
                                            ),
                                        ) { entry ->
                                            val photoId = entry.arguments?.getLong("photoId") ?: -1L
                                            val video = remember(photoId) { viewModel.findMediaById(photoId) }
                                            if (video == null) {
                                                LaunchedEffect(photoId) { navController.popBackStack() }
                                            } else {
                                                VideoEditorScreen(
                                                    video = video,
                                                    onCancel = { navController.popBackStack() },
                                                    onSaveVideo = { file ->
                                                        val savedUri = viewModel.saveEditedVideo(file, video.displayName)
                                                        if (savedUri != null) {
                                                            viewModel.notifyMessage(context.getString(R.string.editor_saved_video))
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                    onError = { message -> viewModel.notifyMessage(message) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Glassmorphic Pill Bottom Dock on Compact Screens
                        AnimatedVisibility(
                            visible = !useNavRail && isTopLevelRoute && selectedIds.isEmpty(),
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            FloatingGalleryNavBar(
                                currentRoute = currentRoute,
                                onSelectRoute = navigateTopLevel,
                            )
                        }
                    }
                }
            }
        }

        if (legacyDeleteTargets.isNotEmpty()) {
            val count = legacyDeleteTargets.size
            AlertDialog(
                onDismissRequest = { legacyDeleteTargets = emptyList() },
                title = {
                    Text(
                        text = if (count == 1) {
                            stringResource(R.string.delete_dialog_title)
                        } else {
                            "Delete $count items?"
                        },
                    )
                },
                text = { Text(text = stringResource(R.string.delete_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targets = legacyDeleteTargets
                            legacyDeleteTargets = emptyList()
                            viewModel.confirmLegacyDelete(targets)
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { legacyDeleteTargets = emptyList() }) {
                        Text(text = stringResource(R.string.delete_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun FloatingGalleryNavBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
) {
    val dockShape = RoundedCornerShape(32.dp)
    Surface(
        shape = dockShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                shape = dockShape,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            topLevelDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                val pillColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_pill_color",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "nav_content_color",
                )

                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .weight(if (selected) 1.75f else 0.85f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(pillColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { onSelectRoute(dest.route) },
                        )
                        .minimumInteractiveComponentSize()
                        .testTag(dest.testTag),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes),
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    AnimatedContent(
                        targetState = selected,
                        label = "nav_label_anim",
                    ) { isSelected ->
                        if (isSelected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(dest.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = contentColor,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

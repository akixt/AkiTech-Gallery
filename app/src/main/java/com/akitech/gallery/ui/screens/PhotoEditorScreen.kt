package com.akitech.gallery.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.akitech.gallery.domain.model.Photo
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PhotoEditTab(val label: String, val icon: ImageVector) {
    FILTERS("Filters", Icons.Rounded.AutoFixHigh),
    ADJUST("Adjust", Icons.Rounded.Tune),
    CROP("Crop & Rotate", Icons.Rounded.Crop),
}

private enum class PhotoFilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    MONO("Mono"),
    CYAN_NEON("Cyan"),
    WARM_FILM("Warm"),
    MATTE("Fade"),
    DRAMATIC("Drama"),
}

private enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Original", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    THREE_FOUR("3:4", 3f / 4f),
}

@Composable
fun PhotoEditorScreen(
    photo: Photo,
    onCancel: () -> Unit,
    onSaveBitmap: suspend (Bitmap) -> Boolean,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCancel)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var activeTab by remember { mutableStateOf(PhotoEditTab.FILTERS) }
    var selectedPreset by remember { mutableStateOf(PhotoFilterPreset.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) }      // -80..80
    var contrast by remember { mutableFloatStateOf(1f) }        // 0.6..1.6
    var saturation by remember { mutableFloatStateOf(1f) }      // 0..2
    var warmth by remember { mutableFloatStateOf(0f) }          // -40..40
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var cropAspect by remember { mutableStateOf(CropAspect.FREE) }

    LaunchedEffect(photo.contentUri) {
        sourceBitmap = loadEditableBitmap(context, photo.contentUri, maxDim = 2048)
    }

    val colorMatrix = remember(selectedPreset, brightness, contrast, saturation, warmth) {
        buildCombinedColorMatrix(
            preset = selectedPreset,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth,
        )
    }

    val composeColorFilter = remember(colorMatrix) {
        ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(colorMatrix.array))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .testTag("photo_editor_screen"),
    ) {
        // Editor Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close editor",
                    tint = Color.White,
                )
            }

            Text(
                text = "Photo Studio",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        selectedPreset = PhotoFilterPreset.ORIGINAL
                        brightness = 0f
                        contrast = 1f
                        saturation = 1f
                        warmth = 0f
                        rotationDegrees = 0
                        flipH = false
                        flipV = false
                        cropAspect = CropAspect.FREE
                    },
                    enabled = !isSaving,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset edits",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }

                Button(
                    onClick = {
                        val base = sourceBitmap ?: return@Button
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            val rendered = renderFinalBitmap(
                                source = base,
                                colorMatrix = colorMatrix,
                                rotationDegrees = rotationDegrees,
                                flipH = flipH,
                                flipV = flipV,
                                cropRatio = cropAspect.ratio,
                            )
                            val ok = onSaveBitmap(rendered)
                            if (rendered !== base) {
                                rendered.recycle()
                            }
                            isSaving = false
                            if (ok) onCancel()
                        }
                    },
                    enabled = sourceBitmap != null && !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("photo_editor_save_button"),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }

        // Live Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = sourceBitmap
            if (bmp == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                val previewContainerModifier = if (cropAspect.ratio != null) {
                    Modifier
                        .aspectRatio(cropAspect.ratio!!)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier.fillMaxSize()
                }

                Box(
                    modifier = previewContainerModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Edited photo preview",
                        colorFilter = composeColorFilter,
                        contentScale = if (cropAspect.ratio != null) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = rotationDegrees.toFloat()
                                scaleX = if (flipH) -1f else 1f
                                scaleY = if (flipV) -1f else 1f
                            },
                    )
                }
            }
        }

        // Editor Control Panel
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFF111319),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 14.dp, bottom = 14.dp),
            ) {
                when (activeTab) {
                    PhotoEditTab.FILTERS -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(PhotoFilterPreset.entries) { preset ->
                                val isSelected = selectedPreset == preset
                                val presetFilter = remember(preset) {
                                    ColorFilter.colorMatrix(
                                        androidx.compose.ui.graphics.ColorMatrix(
                                            buildCombinedColorMatrix(preset, 0f, 1f, 1f, 0f).array,
                                        ),
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedPreset = preset }
                                        .padding(4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.White.copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                            ),
                                    ) {
                                        sourceBitmap?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = preset.label,
                                                colorFilter = presetFilter,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.White.copy(alpha = 0.78f)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    PhotoEditTab.ADJUST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            EditorSlider("Brightness", brightness, -80f..80f) { brightness = it }
                            EditorSlider("Contrast", contrast, 0.6f..1.6f) { contrast = it }
                            EditorSlider("Saturation", saturation, 0f..2f) { saturation = it }
                            EditorSlider("Warmth", warmth, -40f..40f) { warmth = it }
                        }
                    }

                    PhotoEditTab.CROP -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                TransformButton(
                                    icon = Icons.AutoMirrored.Rounded.RotateLeft,
                                    label = "-90°",
                                    onClick = { rotationDegrees = (rotationDegrees - 90) % 360 },
                                )
                                TransformButton(
                                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                                    label = "+90°",
                                    onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                                )
                                TransformButton(
                                    icon = Icons.Rounded.Flip,
                                    label = "Flip H",
                                    onClick = { flipH = !flipH },
                                )
                                TransformButton(
                                    icon = Icons.Rounded.Flip,
                                    label = "Flip V",
                                    onClick = { flipV = !flipV },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CropAspect.entries.forEach { aspect ->
                                    val selected = cropAspect == aspect
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color(0xFF1F232E)
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { cropAspect = aspect },
                                    ) {
                                        Text(
                                            text = aspect.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.85f)
                                            },
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Tool Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PhotoEditTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        TextButton(
                            onClick = { activeTab = tab },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                },
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.width(84.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TransformButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1F232E),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

private fun buildCombinedColorMatrix(
    preset: PhotoFilterPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
): ColorMatrix {
    val result = ColorMatrix()

    when (preset) {
        PhotoFilterPreset.ORIGINAL -> Unit
        PhotoFilterPreset.VIVID -> {
            val sat = ColorMatrix().apply { setSaturation(1.38f) }
            val con = contrastMatrix(1.14f, 6f)
            result.postConcat(sat)
            result.postConcat(con)
        }
        PhotoFilterPreset.MONO -> {
            result.setSaturation(0f)
            result.postConcat(contrastMatrix(1.18f, 4f))
        }
        PhotoFilterPreset.CYAN_NEON -> {
            val neon = ColorMatrix(
                floatArrayOf(
                    0.82f, 0f, 0f, 0f, -8f,
                    0f, 1.08f, 0f, 0f, 6f,
                    0f, 0f, 1.24f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            result.postConcat(neon)
            result.postConcat(contrastMatrix(1.12f, 0f))
        }
        PhotoFilterPreset.WARM_FILM -> {
            val warm = ColorMatrix(
                floatArrayOf(
                    1.14f, 0f, 0f, 0f, 10f,
                    0f, 1.04f, 0f, 0f, 4f,
                    0f, 0f, 0.86f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            result.postConcat(warm)
        }
        PhotoFilterPreset.MATTE -> {
            val sat = ColorMatrix().apply { setSaturation(0.75f) }
            result.postConcat(sat)
            result.postConcat(contrastMatrix(0.88f, 18f))
        }
        PhotoFilterPreset.DRAMATIC -> {
            val sat = ColorMatrix().apply { setSaturation(0.62f) }
            result.postConcat(sat)
            result.postConcat(contrastMatrix(1.32f, -12f))
        }
    }

    if (saturation != 1f) {
        val sat = ColorMatrix().apply { setSaturation(saturation) }
        result.postConcat(sat)
    }

    if (contrast != 1f || brightness != 0f) {
        result.postConcat(contrastMatrix(contrast, brightness))
    }

    if (warmth != 0f) {
        val w = warmth * 0.5f
        result.postConcat(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, w,
                    0f, 1f, 0f, 0f, w * 0.25f,
                    0f, 0f, 1f, 0f, -w,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }

    return result
}

private fun contrastMatrix(contrast: Float, brightness: Float): ColorMatrix {
    val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

private suspend fun loadEditableBitmap(
    context: Context,
    uri: Uri,
    maxDim: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = max(w, h)
                if (largest > maxDim) {
                    val scale = maxDim.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            var sample = 1
            while (max(opts.outWidth, opts.outHeight) / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }
    }.getOrNull()
}

private suspend fun renderFinalBitmap(
    source: Bitmap,
    colorMatrix: ColorMatrix,
    rotationDegrees: Int,
    flipH: Boolean,
    flipV: Boolean,
    cropRatio: Float?,
): Bitmap = withContext(Dispatchers.Default) {
    val transformMatrix = Matrix().apply {
        postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        postRotate(rotationDegrees.toFloat())
    }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, transformMatrix, true)

    val cropped = if (cropRatio != null) {
        val currentRatio = rotated.width.toFloat() / rotated.height.toFloat()
        val (targetW, targetH) = if (currentRatio > cropRatio) {
            (rotated.height * cropRatio).roundToInt().coerceAtLeast(1) to rotated.height
        } else {
            rotated.width to (rotated.width / cropRatio).roundToInt().coerceAtLeast(1)
        }
        val x = ((rotated.width - targetW) / 2).coerceAtLeast(0)
        val y = ((rotated.height - targetH) / 2).coerceAtLeast(0)
        Bitmap.createBitmap(rotated, x, y, targetW, targetH)
    } else {
        rotated
    }

    val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    canvas.drawBitmap(cropped, 0f, 0f, paint)

    if (cropped !== rotated && cropped !== source) cropped.recycle()
    if (rotated !== source) rotated.recycle()
    output
}

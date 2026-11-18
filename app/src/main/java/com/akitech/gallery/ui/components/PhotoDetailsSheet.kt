package com.akitech.gallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akitech.gallery.R
import com.akitech.gallery.domain.model.Photo
import com.akitech.gallery.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailsSheet(
    photo: Photo,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.details_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailRow(
                icon = if (photo.isVideo) Icons.Rounded.VideoFile else Icons.Rounded.Image,
                label = stringResource(R.string.details_name),
                value = photo.displayName,
            )
            DetailRow(
                icon = Icons.Rounded.Folder,
                label = stringResource(R.string.details_album),
                value = photo.bucketDisplayName,
            )
            DetailRow(
                icon = Icons.Rounded.CalendarToday,
                label = stringResource(R.string.details_date),
                value = Formatters.formatPhotoDate(photo.effectiveDateSeconds),
            )
            if (photo.isVideo && photo.durationMs > 0L) {
                DetailRow(
                    icon = Icons.Rounded.Timer,
                    label = stringResource(R.string.details_duration),
                    value = Formatters.formatDuration(photo.durationMs),
                )
            }
            DetailRow(
                icon = Icons.Rounded.SdStorage,
                label = stringResource(R.string.details_size),
                value = Formatters.formatFileSize(photo.sizeBytes),
            )
            DetailRow(
                icon = Icons.Rounded.AspectRatio,
                label = stringResource(R.string.details_resolution),
                value = Formatters.formatResolution(photo.width, photo.height),
            )
            DetailRow(
                icon = Icons.Rounded.InsertDriveFile,
                label = stringResource(R.string.details_type),
                value = photo.mimeType,
            )
            if (!photo.relativePath.isNullOrBlank()) {
                DetailRow(
                    icon = Icons.Rounded.Folder,
                    label = stringResource(R.string.details_path),
                    value = photo.relativePath,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

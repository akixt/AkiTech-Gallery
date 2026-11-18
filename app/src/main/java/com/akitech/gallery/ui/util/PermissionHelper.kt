package com.akitech.gallery.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.akitech.gallery.domain.model.PermissionStatus

object PermissionHelper {

    fun requiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
    }

    fun checkPermissionStatus(context: Context): PermissionStatus {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val hasFullImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED
                val hasFullVideos = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ) == PackageManager.PERMISSION_GRANTED
                val hasPartialMedia = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ) == PackageManager.PERMISSION_GRANTED
                when {
                    hasFullImages || hasFullVideos -> PermissionStatus.GRANTED
                    hasPartialMedia -> PermissionStatus.LIMITED
                    else -> PermissionStatus.DENIED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val hasImages = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED
                val hasVideos = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasImages || hasVideos) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
        }
    }

    fun openAppSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

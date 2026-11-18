package com.akitech.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.akitech.gallery.ai.configureAppCheck
import com.akitech.gallery.ui.navigation.GalleryAppRoot
import com.akitech.gallery.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels { GalleryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            GalleryAppRoot(viewModel = viewModel)
        }
    }
}

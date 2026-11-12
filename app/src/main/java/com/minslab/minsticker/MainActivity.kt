package com.minslab.minsticker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.minslab.minsticker.keyboard.SampleStickerGenerator
import com.minslab.minsticker.ui.EditorScreen
import com.minslab.minsticker.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private var sharedImageUri by mutableStateOf<Uri?>(null)
    private var currentScreen by mutableStateOf<Screen>(Screen.Home)

    sealed interface Screen {
        object Home : Screen
        data class Editor(val uri: Uri? = null) : Screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure sample stickers exist
        SampleStickerGenerator.ensureSampleStickers(this)

        // Handle incoming SEND intent (from Gallery / Google Photos)
        handleIncomingIntent(intent)

        setContent {
            MaterialTheme {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            onCreateStickerClick = {
                                currentScreen = Screen.Editor(null)
                            }
                        )
                    }
                    is Screen.Editor -> {
                        EditorScreen(
                            initialUri = screen.uri,
                            onNavigateBack = {
                                currentScreen = Screen.Home
                            },
                            onSavedSuccess = {
                                currentScreen = Screen.Home
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("create_sticker", false)) {
            currentScreen = Screen.Editor(null)
            return
        }
        if (Intent.ACTION_SEND == intent.action && intent.type?.startsWith("image/") == true) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            }
            if (uri != null) {
                sharedImageUri = uri
                currentScreen = Screen.Editor(uri)
            }
        }
    }
}

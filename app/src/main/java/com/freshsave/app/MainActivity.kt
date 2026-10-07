package com.freshsave.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.freshsave.app.navigation.FreshSaveApp
import com.freshsave.app.ui.theme.FreshSaveTheme

/** The app's only Activity. Every screen is a composable swapped in by Navigation Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; screens add their own padding.
        enableEdgeToEdge()
        setContent {
            FreshSaveTheme {
                FreshSaveApp()
            }
        }
    }
}

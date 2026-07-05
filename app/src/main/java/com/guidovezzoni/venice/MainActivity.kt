package com.guidovezzoni.venice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guidovezzoni.venice.ui.MainScreen
import com.guidovezzoni.venice.ui.theme.HeadingToVeniceTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeadingToVeniceTheme {
                MainScreen()
            }
        }
    }
}

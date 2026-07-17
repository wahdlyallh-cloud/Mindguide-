package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DiaryComposeScreen
import com.example.ui.screens.MainContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DiaryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DiaryViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf("main") }

                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            "main" -> MainContainer(
                                viewModel = viewModel,
                                onNavigateToCompose = { currentScreen = "compose" }
                            )
                            "compose" -> DiaryComposeScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = "main" }
                            )
                        }
                    }
                }
            }
        }
    }
}

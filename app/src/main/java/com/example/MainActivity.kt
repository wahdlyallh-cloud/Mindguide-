package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DiaryComposeScreen
import com.example.ui.screens.MainContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DiaryViewModel

class MainActivity : ComponentActivity() {
    private var viewModelInstance: DiaryViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start the persistent foreground service for system tray and lock screen notification
        com.example.service.PersistentNotificationService.start(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DiaryViewModel = viewModel()
                    viewModelInstance = viewModel

                    // Handle intent when app starts
                    LaunchedEffect(intent) {
                        handleIntent(intent, viewModel)
                    }

                    // Check overlay permission and trigger floating service if enabled
                    LaunchedEffect(viewModel.isFloatingBallEnabled) {
                        if (viewModel.isFloatingBallEnabled) {
                            checkOverlayPermissionAndStartService()
                        } else {
                            stopFloatingWidgetService()
                        }
                    }

                    Crossfade(targetState = viewModel.currentScreenState, label = "ScreenTransition") { screen ->
                        when (screen) {
                            "main" -> MainContainer(
                                viewModel = viewModel,
                                onNavigateToCompose = { viewModel.currentScreenState = "compose" }
                            )
                            "compose" -> DiaryComposeScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.currentScreenState = "main" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModelInstance?.let { handleIntent(intent, it) }
    }

    private fun handleIntent(intent: Intent, viewModel: DiaryViewModel) {
        val openTab = intent.getIntExtra("open_tab", -1)
        if (openTab != -1) {
            viewModel.currentTabState = openTab
            viewModel.currentScreenState = "main"
        }
        val openCompose = intent.getBooleanExtra("open_compose", false)
        if (openCompose) {
            viewModel.currentScreenState = "compose"
        }
        
        val openAction = intent.getStringExtra("open_action")
        if (openAction != null) {
            when (openAction) {
                "photo" -> {
                    viewModel.currentScreenState = "compose"
                    viewModel.shouldSelectPhotoInCompose = true
                }
                "task" -> {
                    viewModel.currentTabState = 0 // Home screen
                    viewModel.currentScreenState = "main"
                    viewModel.shouldOpenDailyTasksDialog = true
                }
                "consultant" -> {
                    viewModel.currentTabState = 2 // Consultant tab
                    viewModel.currentScreenState = "main"
                }
                "mood" -> {
                    viewModel.currentTabState = 0 // Home screen
                    viewModel.currentScreenState = "main"
                    viewModel.shouldOpenMoodDialog = true
                }
                "record" -> {
                    viewModel.currentTabState = 2 // Consultant tab
                    viewModel.currentScreenState = "main"
                    viewModel.shouldOpenVoiceCallDialog = true
                }
                "write" -> {
                    viewModel.currentScreenState = "compose"
                }
            }
        }
    }

    companion object {
        fun showPersistentNotification(context: Context) {
            com.example.service.PersistentNotificationService.start(context)
        }
    }

    private fun checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, 5469)
                    Toast.makeText(this, "يرجى تفعيل خيار الظهور فوق التطبيقات الأخرى لتشغيل الزر العائم خارج التطبيق 🧠", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                startFloatingWidgetService()
            }
        } else {
            startFloatingWidgetService()
        }
    }

    private fun startFloatingWidgetService() {
        try {
            val intent = Intent(this, com.example.service.FloatingWidgetService::class.java)
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFloatingWidgetService() {
        try {
            val intent = Intent(this, com.example.service.FloatingWidgetService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

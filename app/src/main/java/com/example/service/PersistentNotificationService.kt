package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class PersistentNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "persistent_yawmiyati_channel_v2"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "شريط إشعارات يومياتي المستمر",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعار دائم لتسهيل الدخول السريع للتطبيق والمستشار الذكي"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // PendingIntent for main content click (opens main screen)
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            3001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // PendingIntent for Action 1: Smart Consultant (opens tab 2)
        val consultantIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_tab", 2)
        }
        val consultantPendingIntent = PendingIntent.getActivity(
            this,
            3002,
            consultantIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // PendingIntent for Action 2: Write Diary (opens compose screen)
        val composeIntent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_compose", true)
        }
        val composePendingIntent = PendingIntent.getActivity(
            this,
            3003,
            composeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("يومياتي AI والمستشار النفسي 🧠")
            .setContentText("اضغط للدخول السريع وجلسات التدوين والاستشارة اليومية.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // lock screen visible
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_help,
                "المستشار النفسي 🧠",
                consultantPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_edit,
                "تدوين يومية جديدة 📝",
                composePendingIntent
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(2002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(2002, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to startForeground without type if required
            try {
                startForeground(2002, notification)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        return START_STICKY
    }

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, PersistentNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PersistentNotificationService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

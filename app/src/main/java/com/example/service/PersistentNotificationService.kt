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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

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

        // Helper to build action intents
        fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("open_action", action)
            }
            return PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
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

        // Build the RemoteViews
        val remoteViews = RemoteViews(packageName, R.layout.notification_custom)
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_camera, createActionPendingIntent("photo", 4001))
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_check, createActionPendingIntent("task", 4002))
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_consultant, createActionPendingIntent("consultant", 4003))
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_heart, createActionPendingIntent("mood", 4004))
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_mic, createActionPendingIntent("record", 4005))
        remoteViews.setOnClickPendingIntent(R.id.btn_notif_pencil, createActionPendingIntent("write", 4006))

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // lock screen visible
            .setContentIntent(mainPendingIntent)
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

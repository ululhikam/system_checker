package com.example.checker.service

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.checker.MainActivity
import com.example.checker.data.CheckerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ClipboardMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private lateinit var clipboard: ClipboardManager
    private val repository by lazy { CheckerRepository(this) }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "cyber_shield_guard"
        private const val ALERT_CHANNEL_ID = "cyber_shield_alerts"
        private const val FOREGROUND_NOTIF_ID = 20261
        private const val ALERT_NOTIF_ID = 20262
        
        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        processClipboard()
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipListener)
        createNotificationChannels()
        startForegroundNotification()
        Log.d("ClipboardService", "Cyber Shield Background Protection Activated.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        clipboard.removePrimaryClipChangedListener(clipListener)
        job.cancel()
        Log.d("ClipboardService", "Cyber Shield Background Protection Deactivated.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun processClipboard() {
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString() ?: ""
            Log.d("ClipboardService", "Clipboard change captured: $text")

            val url = extractUrl(text)
            if (url != null) {
                Log.d("ClipboardService", "URL Extracted for scanning: $url")
                scope.launch {
                    val result = repository.scanUrl(url)
                    result.onSuccess { scanResult ->
                        if (scanResult.dangerScore > 35) {
                            sendThreatPushNotification(scanResult.target, scanResult.dangerScore, scanResult.safetyAdvice)
                        }
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
        val pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        // Match bits, shortlinks without http prefix
        if (text.contains("bit.ly/") || text.contains("s.id/") || text.contains("tinyurl.com/")) {
            val words = text.split("\\s+".toRegex())
            for (word in words) {
                if (word.startsWith("bit.ly/") || word.startsWith("s.id/") || word.startsWith("tinyurl.com/")) {
                    return "http://$word"
                }
            }
        }
        return null
    }

    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Using standard Android system drawable for active protection shield (ic_secure)
        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Cyber Shield Guard Aktif")
            .setContentText("Mengawasi clipboard dan melindungimu secara real-time...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(FOREGROUND_NOTIF_ID, notification)
    }

    private fun sendThreatPushNotification(url: String, dangerScore: Int, advice: String) {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Tautan Mencurigakan Terdeteksi! 🚨")
            .setContentText("URL '$url' memiliki skor ancaman $dangerScore%.")
            .setSubText("Cyber Threat Shield")
            .setStyle(NotificationCompat.BigTextStyle().bigText("URL '$url' terindikasi berbahaya (Skor Risiko: $dangerScore%).\n\nAnalisis: $advice"))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(ALERT_NOTIF_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Cyber Shield Active Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Saluran untuk persistent guard notification"
            }

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Cyber Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Memberikan peringatan darurat saat link phishing disalin ke clipboard"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.createNotificationChannel(foregroundChannel)
            notifManager.createNotificationChannel(alertChannel)
        }
    }
}

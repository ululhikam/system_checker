package com.example.checker.service

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.checker.MainActivity
import com.example.checker.data.CheckerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class NotificationMonitorService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val repository = CheckerRepository()

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "cyber_shield_guard"
        private const val ALERT_CHANNEL_ID = "cyber_shield_alerts"
        private const val FOREGROUND_NOTIF_ID = 20261
        private const val ALERT_NOTIF_ID = 20262
        
        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            // Untuk NotificationListenerService, kita tidak bisa memulainya seperti service biasa
            // Kita hanya bisa mengecek apakah ijin sudah diberikan
            if (!isNotificationServiceEnabled(context)) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Toast.makeText(context, "Mohon izinkan Cyber Shield untuk membaca notifikasi", Toast.LENGTH_LONG).show()
            } else {
                // Service akan otomatis jalan oleh sistem jika ijin diberikan
                isServiceRunning = true
                Toast.makeText(context, "Pemantau Notifikasi Aktif", Toast.LENGTH_SHORT).show()
            }
        }

        fun stopService(context: Context) {
            // NotificationListenerService biasanya dikontrol oleh sistem
            // Kita bisa menonaktifkan logic internalnya
            isServiceRunning = false
            Toast.makeText(context, "Pemantau Notifikasi Dinonaktifkan", Toast.LENGTH_SHORT).show()
        }

        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (flat != null) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == pkgName) {
                        return true
                    }
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannels()
        startForegroundNotification()
        Log.d("NotifService", "Cyber Shield Notification Protection Activated.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceRunning = true
        Log.d("NotifService", "Notification Listener Connected")
        scope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, "Cyber Shield Terhubung!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceRunning = false
        Log.d("NotifService", "Notification Listener Disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        job.cancel()
        Log.d("NotifService", "Cyber Shield Notification Protection Deactivated.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Jangan scan jika dimatikan via UI atau sbn null
        if (!isServiceRunning || sbn == null) return
        
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return // Jangan scan notif sendiri

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        
        val fullContent = "$title $text $bigText $subText".trim()

        if (fullContent.isEmpty()) return

        Log.d("NotifService", "Captured from $packageName: $fullContent")
        
        // Tampilkan Toast untuk membantu user tahu bahwa app sedang memproses
        scope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, "Shield: Mendeteksi notifikasi dari $packageName", Toast.LENGTH_SHORT).show()
        }
        
        processContent(fullContent)
    }

    private fun processContent(content: String) {
        val url = extractUrl(content)
        if (url != null) {
            Log.d("NotifService", "URL found in notification: $url. Starting API scan...")
            scope.launch {
                // Beri feedback visual kalau proses scan dimulai
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Shield: Memindai Link Mencurigakan...", Toast.LENGTH_SHORT).show()
                }
                
                val result = repository.scanUrl(url)
                result.onSuccess { scanResult ->
                    Log.d("NotifService", "Scan success. Danger Score: ${scanResult.dangerScore}")
                    // Turunkan threshold ke 20 agar lebih sensitif untuk testing
                    if (scanResult.dangerScore >= 20) {
                        sendThreatPushNotification(scanResult.target, scanResult.dangerScore, scanResult.safetyAdvice)
                    } else {
                        Log.d("NotifService", "Score ${scanResult.dangerScore} below threshold 20.")
                    }
                }.onFailure { error ->
                    Log.e("NotifService", "Scan API failed: ${error.message}")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Shield Error: Gagal konek ke backend laptop!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else if (content.length > 15) { // Kurangi batas karakter agar lebih sensitif
            Log.d("NotifService", "No URL, scanning for Hoax content...")
            scope.launch {
                // Beri feedback visual
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Shield: Menganalisis Potensi Hoaks...", Toast.LENGTH_SHORT).show()
                }

                val result = repository.checkHoax(content, null, "gemini")
                result.onSuccess { hoaxResult ->
                    Log.d("NotifService", "Hoax scan success. Trust Score: ${hoaxResult.trustScore}")
                    // Jika skor kepercayaan rendah (hoaks), kirim notif
                    if (hoaxResult.trustScore < 60) {
                        sendHoaxPushNotification(content.take(40) + "...", hoaxResult.trustScore, hoaxResult.verdictSummary)
                    }
                }.onFailure { error ->
                    Log.e("NotifService", "Hoax API failed: ${error.message}")
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

        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Cyber Shield Notification Guard Aktif")
            .setContentText("Mengawasi notifikasi berbahaya secara real-time...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FOREGROUND_NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FOREGROUND_NOTIF_ID, notification)
        }
    }

    private fun sendThreatPushNotification(url: String, dangerScore: Int, advice: String) {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Link Berbahaya di Notifikasi! 🚨")
            .setContentText("URL '$url' memiliki skor ancaman $dangerScore%.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mendeteksi link mencurigakan dari notifikasi: $url\n\nSkor Risiko: $dangerScore%\nAnalisis: $advice"))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(ALERT_NOTIF_ID, notification)
    }

    private fun sendHoaxPushNotification(snippet: String, trustScore: Int, verdict: String) {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Hoaks Terdeteksi di Notifikasi! 🛡️")
            .setContentText("Konten: $snippet")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mendeteksi pesan mencurigakan: $snippet\n\nTrust Score: $trustScore%\nVerdict: $verdict"))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(ALERT_NOTIF_ID + 1, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Cyber Shield Active Protection",
                NotificationManager.IMPORTANCE_LOW
            )

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Cyber Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.createNotificationChannel(foregroundChannel)
            notifManager.createNotificationChannel(alertChannel)
        }
    }
}

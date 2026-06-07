package com.example.checker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.checker.MainActivity
import com.example.checker.data.CheckerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationMonitorService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val repository by lazy { CheckerRepository.getInstance(this) }

    companion object {
        private const val CHANNEL_ID = "cyber_shield_notif_monitor"
        private const val NOTIF_ID = 20263
        
        var isServiceRunning = false
            private set

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, NotificationMonitorService::class.java)
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        Log.d("NotificationService", "Cyber Shield Notification Monitoring Activated.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceRunning = true
        Log.d("NotificationService", "Notification Listener Connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceRunning = false
        Log.d("NotificationService", "Notification Listener Disconnected.")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        job.cancel()
        Log.d("NotificationService", "Cyber Shield Notification Monitoring Deactivated.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Skip our own notifications to avoid infinite loop
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val appName = getAppName(sbn.packageName)
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val content = if (title.isNotEmpty()) "$title: $text" else text

        if (content.isBlank()) return

        Log.d("NotificationService", "Received from $appName: $content")
        
        scope.launch {
            repository.processNotification(appName, content)
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}

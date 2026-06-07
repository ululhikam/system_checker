package com.example.checker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID

@Suppress("UNCHECKED_CAST")
class CheckerRepository private constructor(private val context: Context) {

    private val api = NetworkClient.apiService
    private val gson = Gson()
    private val sharedPrefs = context.getSharedPreferences("checker_history", Context.MODE_PRIVATE)
    
    // In-memory cache for Search History to make UI reactive
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList

    private val _notificationLogs = MutableStateFlow<List<HistoryItem>>(emptyList())
    val notificationLogs: StateFlow<List<HistoryItem>> = _notificationLogs

    companion object {
        @Volatile
        private var INSTANCE: CheckerRepository? = null

        fun getInstance(context: Context): CheckerRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CheckerRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "history_list") loadLocalHistory()
        if (key == "notification_logs") loadNotificationLogs()
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        loadLocalHistory()
        loadNotificationLogs()
    }

    private fun saveLocalHistory(list: List<HistoryItem>) {
        try {
            sharedPrefs?.edit()?.putString("history_list", gson.toJson(list))?.apply()
        } catch (e: Exception) {
            Log.e("CheckerRepository", "Failed to save history: ${e.message}")
        }
    }

    private fun saveNotificationLogs(list: List<HistoryItem>) {
        try {
            sharedPrefs?.edit()?.putString("notification_logs", gson.toJson(list))?.apply()
        } catch (e: Exception) {
            Log.e("CheckerRepository", "Failed to save notification logs: ${e.message}")
        }
    }

    private fun loadLocalHistory() {
        val json = sharedPrefs?.getString("history_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                val list = gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
                _historyList.value = list
            } catch (e: Exception) {
                Log.e("CheckerRepository", "Failed to load history: ${e.message}")
            }
        }
    }

    private fun loadNotificationLogs() {
        val json = sharedPrefs?.getString("notification_logs", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<HistoryItem>>() {}.type
                val list = gson.fromJson<List<HistoryItem>>(json, type) ?: emptyList()
                _notificationLogs.value = list
            } catch (e: Exception) {
                Log.e("CheckerRepository", "Failed to load notification logs: ${e.message}")
            }
        }
    }

    suspend fun processNotification(appName: String, content: String) {
        if (!isAutoScanEnabled()) return

        // Extract URLs first to ensure we only detect links
        val urls = extractUrls(content)
        val isIgnoreKeyword = containsIgnoreKeywords(content)

        // Only process if it contains a link and is not a system/battery notification
        if (urls.isEmpty() || isIgnoreKeyword) return

        val id = UUID.randomUUID().toString().take(8)
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Cek duplikasi via hash
        val contentHash = content.hashCode().toString()
        if (_notificationLogs.value.any { it.originalContent?.hashCode().toString() == contentHash }) {
            return
        }

        val url = urls.first()
        val initialItem = HistoryItem(
            id = id,
            type = "notification",
            title = "Scan Link: $url",
            score = 0,
            status = "analyzing",
            timestamp = timestamp,
            appName = appName,
            originalContent = content
        )

        addNotificationLog(initialItem)

        // Scan URL
        val result = scanUrl(url)
        result.onSuccess { scanResult ->
            updateNotificationStatus(id, "completed", scanResult.dangerScore, scanResult)
            addLocalHistory("scam", "Notif: $appName - $url", scanResult.dangerScore, scanResult.threatLevel, scanResult)
        }.onFailure {
            updateNotificationStatus(id, "failed")
        }
    }

    private fun containsIgnoreKeywords(text: String): Boolean {
        val ignoreKeywords = listOf("charging", "mengisi daya", "battery", "baterai", "daya terhubung", "power connected")
        return ignoreKeywords.any { text.lowercase().contains(it) }
    }

    private fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val urlRegex = "((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
        val pattern = java.util.regex.Pattern.compile(urlRegex, java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val group = matcher.group(1)
            if (group != null) {
                urls.add(group)
            }
        }
        return urls
    }

    private fun containsNewsKeywords(text: String): Boolean {
        val keywords = listOf("berita", "kabaran", "info", "viral", "kejadian", "peristiwa", "waspada", "pengumuman")
        return keywords.any { text.lowercase().contains(it) } || text.split(" ").size > 10
    }

    private fun addNotificationLog(item: HistoryItem) {
        val currentList = _notificationLogs.value.toMutableList()
        currentList.add(0, item)
        if (currentList.size > 100) currentList.removeAt(currentList.lastIndex)
        _notificationLogs.value = currentList
        saveNotificationLogs(currentList)
    }

    private fun updateNotificationStatus(id: String, status: String, score: Int = 0, details: Any? = null) {
        val currentList = _notificationLogs.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldItem = currentList[index]
            currentList[index] = oldItem.copy(status = status, score = score, resultDetails = details)
            _notificationLogs.value = currentList
            saveNotificationLogs(currentList)
        }
    }

    fun isAutoScanEnabled(): Boolean {
        return sharedPrefs.getBoolean("auto_scan_enabled", false)
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("auto_scan_enabled", enabled).apply()
    }

    suspend fun clearNotificationLogs(): Result<Boolean> {
        return try {
            val prefs = context?.getSharedPreferences("checker_history", Context.MODE_PRIVATE)
            prefs?.edit()?.remove("notification_logs")?.apply()
            _notificationLogs.value = emptyList()
            Result.success(true)
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    suspend fun checkHoax(text: String?, imageUrl: String?, engine: String): Result<HoaxResponse> {
        return try {
            val response = api.checkHoax(HoaxRequest(text, imageUrl, engine))
            
            // Add to history list reactive state
            val historyTitle = text?.take(40) ?: "Pemindaian Gambar OCR"
            addLocalHistory("hoax", historyTitle, response.trustScore, response.status, response)
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun scanUrl(url: String, enableWebScraping: Boolean = false): Result<ScamResponse> {
        return try {
            val response = api.scanUrl(ScamUrlRequest(url, enableWebScraping))
            
            addLocalHistory("scam", url, response.dangerScore, response.threatLevel, response)
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF URL Scan Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun scanFile(file: File): Result<ScamResponse> {
        return try {
            val fileRequestBody = file.readBytes().toRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, fileRequestBody)
            
            val fileNameBody = file.name.toRequestBody("text/plain".toMediaTypeOrNull())
            val fileSizeBody = file.length().toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.scanFile(filePart, fileNameBody, fileSizeBody)
            
            addLocalHistory("scam", file.name, response.dangerScore, response.threatLevel, response)
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF File Scan Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchHistory(): Result<List<HistoryItem>> {
        loadLocalHistory()
        return Result.success(_historyList.value)
    }

    suspend fun clearHistory(): Result<Boolean> {
        return try {
            sharedPrefs?.edit()?.clear()?.apply()
            _historyList.value = emptyList()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "Failed to clear local history: ${e.message}")
            Result.success(false)
        }
    }

    private fun addLocalHistory(type: String, title: String, score: Int, status: String, details: Any) {
        val newItem = HistoryItem(
            id = UUID.randomUUID().toString().take(8),
            type = type,
            title = title,
            score = score,
            status = status,
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
            resultDetails = details
        )
        val currentList = _historyList.value.toMutableList()
        currentList.add(0, newItem)
        if (currentList.size > 50) currentList.removeAt(currentList.lastIndex)
        saveLocalHistory(currentList)
        _historyList.value = currentList
    }
}

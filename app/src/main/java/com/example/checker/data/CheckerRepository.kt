package com.example.checker.data

import android.content.Context
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
class CheckerRepository(private val context: Context? = null) {

    private val api = NetworkClient.apiService
    private val gson = Gson()
    private val sharedPrefs = context?.getSharedPreferences("checker_history", Context.MODE_PRIVATE)
    
    // In-memory cache for Search History to make UI reactive
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList

    init {
        loadLocalHistory()
    }

    private fun saveLocalHistory(list: List<HistoryItem>) {
        try {
            sharedPrefs?.edit()?.putString("history_list", gson.toJson(list))?.apply()
        } catch (e: Exception) {
            Log.e("CheckerRepository", "Failed to save history: ${e.message}")
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
        if (currentList.size > 50) currentList.removeLast()
        saveLocalHistory(currentList)
        _historyList.value = currentList
    }
}

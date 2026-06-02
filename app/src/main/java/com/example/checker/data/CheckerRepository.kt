package com.example.checker.data

import android.util.Log
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
class CheckerRepository {

    private val api = NetworkClient.apiService
    
    // In-memory cache for Search History to make UI reactive
    private val _historyList = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyList: StateFlow<List<HistoryItem>> = _historyList

    suspend fun checkHoax(text: String?, imageUrl: String?, engine: String): Result<HoaxResponse> {
        return try {
            val response = api.checkHoax(HoaxRequest(text, imageUrl, engine))
            
            // Add to history list reactive state
            val historyTitle = text?.take(40) ?: "Pemindaian Gambar OCR"
            addLocalHistory("hoax", historyTitle, response.trustScore, response.status, response)
            
            // Sync to backend BFF asynchronously (non-blocking)
            CoroutineScope(Dispatchers.IO).launch {
                syncHistoryToBackend("hoax", historyTitle, response.trustScore, response.status, response)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun scanUrl(url: String): Result<ScamResponse> {
        return try {
            val response = api.scanUrl(ScamUrlRequest(url))
            
            addLocalHistory("scam", url, response.dangerScore, response.threatLevel, response)
            
            // Sync to backend BFF asynchronously (non-blocking)
            CoroutineScope(Dispatchers.IO).launch {
                syncHistoryToBackend("scam", url, response.dangerScore, response.threatLevel, response)
            }
            
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
            
            // Sync to backend BFF asynchronously (non-blocking)
            CoroutineScope(Dispatchers.IO).launch {
                syncHistoryToBackend("scam", file.name, response.dangerScore, response.threatLevel, response)
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF File Scan Error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun syncHistoryToBackend(type: String, title: String, score: Int, status: String, details: Any) {
        try {
            api.addHistory(AddHistoryRequest(type, title, score, status, details))
        } catch (e: Exception) {
            Log.e("CheckerRepository", "Failed to sync history to backend: ${e.message}")
        }
    }

    suspend fun fetchHistory(): Result<List<HistoryItem>> {
        return try {
            val history = api.getHistory()
            _historyList.value = history
            Result.success(history)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF History fetch failed. Serving Local Cache.")
            Result.success(_historyList.value)
        }
    }

    suspend fun clearHistory(): Result<Boolean> {
        return try {
            api.clearHistory()
            _historyList.value = emptyList()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("CheckerRepository", "BFF History clear failed. Purging Local Cache.")
            _historyList.value = emptyList()
            Result.success(true)
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
        _historyList.value = currentList
    }
}

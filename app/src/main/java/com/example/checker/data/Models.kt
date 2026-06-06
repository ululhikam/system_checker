package com.example.checker.data

import com.google.gson.annotations.SerializedName

// Hoax Checker Contracts
data class HoaxRequest(
    @SerializedName("text") val text: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("engine") val engine: String // "gemini" or "deepseek"
)

data class GoogleFactCheck(
    @SerializedName("claim") val claim: String,
    @SerializedName("claimant") val claimant: String,
    @SerializedName("verdict") val verdict: String,
    @SerializedName("reviewDate") val reviewDate: String,
    @SerializedName("publisher") val publisher: String,
    @SerializedName("url") val url: String
)

data class AiInsights(
    @SerializedName("engineUsed") val engineUsed: String,
    @SerializedName("contextNarrative") val contextNarrative: String,
    @SerializedName("credibilityAnalysis") val credibilityAnalysis: String,
    @SerializedName("recommendations") val recommendations: String
)

data class HoaxResponse(
    @SerializedName("trustScore") val trustScore: Int,
    @SerializedName("status") val status: String, // "safe", "neutral", "unsafe"
    @SerializedName("query") val query: String,
    @SerializedName("ocrExtractedText") val ocrExtractedText: String?,
    @SerializedName("verdictSummary") val verdictSummary: String,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("correctedFact") val correctedFact: String = "", // Fakta Sebenarnya
    @SerializedName("fallaciesDetected") val fallaciesDetected: List<String>,
    @SerializedName("googleFactChecks") val googleFactChecks: List<GoogleFactCheck>,
    @SerializedName("aiInsights") val aiInsights: AiInsights,
    @SerializedName("timestamp") val timestamp: String
)

// Scam Scanner Contracts
data class ScamUrlRequest(
    @SerializedName("url") val url: String,
    @SerializedName("enableWebScraping") val enableWebScraping: Boolean = false
)

data class DetectionItem(
    @SerializedName("engine") val engine: String,
    @SerializedName("category") val category: String,
    @SerializedName("result") val result: String // "clean", "phishing", "malware", "suspicious"
)

data class ScamResponse(
    @SerializedName("target") val target: String,
    @SerializedName("type") val type: String, // "url" or "file"
    @SerializedName("dangerScore") val dangerScore: Int, // 0-100
    @SerializedName("threatLevel") val threatLevel: String, // "safe", "warning", "dangerous"
    @SerializedName("totalEngines") val totalEngines: Int,
    @SerializedName("flaggedEngineCount") val flaggedEngineCount: Int,
    @SerializedName("cleanCount") val cleanCount: Int,
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("hostCountry") val hostCountry: String?,
    @SerializedName("reputationPoints") val reputationPoints: Int,
    @SerializedName("detections") val detections: List<DetectionItem>,
    @SerializedName("safetyAdvice") val safetyAdvice: String,
    @SerializedName("timestamp") val timestamp: String
)

// History Contracts
data class HistoryItem(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "hoax" or "scam"
    @SerializedName("title") val title: String,
    @SerializedName("score") val score: Int,
    @SerializedName("status") val status: String, // "safe", "warning", "dangerous", "neutral"
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("resultDetails") val resultDetails: Any? // Can be cast or mapped based on type
)


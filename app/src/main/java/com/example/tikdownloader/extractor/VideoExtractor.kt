package com.example.tikdownloader.extractor

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import java.util.concurrent.TimeUnit

data class VideoData(
    val downloadUrl: String,
    val coverUrl: String,
    val title: String,
    val source: String,
    val isAudioOnly: Boolean = false
)

object VideoExtractor {
    private const val TAG = "EXTRACTOR_PRO"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT_PC = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun extract(url: String, audioOnly: Boolean = false): VideoData? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        Log.d(TAG, "🔍 Iniciando extracción: $cleanUrl")
        return@withContext when {
            cleanUrl.contains("tiktok.com") -> extractTikTok(cleanUrl, audioOnly)
            cleanUrl.contains("instagram.com") -> extractInstagram(cleanUrl)
            else -> {
                Log.e(TAG, "Dominio no soportado o eliminado")
                null
            }
        }
    }

    private fun extractTikTok(url: String, audioOnly: Boolean): VideoData? {
        return try {
            val request = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$url")
                .header("User-Agent", USER_AGENT_PC)
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.getInt("code") == 0) {
                val data = json.getJSONObject("data")
                VideoData(
                    downloadUrl = if (audioOnly) data.optString("music") else data.getString("play"),
                    coverUrl = "https://www.tikwm.com" + data.getString("cover"),
                    title = data.optString("title", "TikTok Video"),
                    source = "TikTok",
                    isAudioOnly = audioOnly
                )
            } else null
        } catch (e: Exception) { null }
    }

    private fun extractInstagram(url: String): VideoData? {
        return try {
            val request = Request.Builder()
                .url("https://indown.io/download?link=$url")
                .header("User-Agent", USER_AGENT_PC)
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val videoUrl = findMatch(html, "href=\"(https://.*?\\.cdninstagram\\.com/.*?)\"")
            if (videoUrl != null) VideoData(videoUrl, "", "Instagram Video", "Instagram") else null
        } catch (e: Exception) { null }
    }

    private fun findMatch(html: String, patternStr: String): String? {
        return try {
            val pattern = Pattern.compile(patternStr)
            val matcher = pattern.matcher(html)
            if (matcher.find()) matcher.group(1) else null
        } catch (e: Exception) { null }
    }
}

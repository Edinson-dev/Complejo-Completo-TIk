package com.example.tikdownloader.extractor

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import java.net.URLDecoder

data class VideoData(
    val downloadUrl: String,
    val coverUrl: String,
    val title: String,
    val source: String,
    val isAudioOnly: Boolean = false
)

object VideoExtractor {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    suspend fun extract(url: String, audioOnly: Boolean = false): VideoData? = withContext(Dispatchers.IO) {
        return@withContext when {
            url.contains("tiktok.com") -> extractTikTok(url, audioOnly)
            url.contains("facebook.com") || url.contains("fb.watch") || url.contains("fb.com") -> extractFacebook(url)
            url.contains("instagram.com") -> extractInstagram(url)
            else -> null
        }
    }

    private fun extractTikTok(url: String, audioOnly: Boolean): VideoData? {
        return try {
            val request = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$url")
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.getInt("code") == 0) {
                val data = json.getJSONObject("data")
                val downloadLink = if (audioOnly) data.optString("music", data.getString("play")) else data.getString("play")
                VideoData(
                    downloadUrl = downloadLink,
                    coverUrl = "https://www.tikwm.com" + data.getString("cover"),
                    title = data.optString("title", "TikTok Content"),
                    source = "TikTok",
                    isAudioOnly = audioOnly
                )
            } else null
        } catch (e: Exception) { null }
    }

    private fun extractFacebook(url: String): VideoData? {
        return try {
            // Método 1: Usando el plugin de video de Facebook (Más estable)
            val request = Request.Builder()
                .url("https://www.facebook.com/plugins/video.php?href=$url")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            
            // Buscar URL de video HD o SD en el código fuente
            var videoUrl = findMatch(html, "hd_src\":\"(.*?)\"") ?: findMatch(html, "sd_src\":\"(.*?)\"")
            
            if (videoUrl != null) {
                return VideoData(
                    downloadUrl = videoUrl.replace("\\/", "/"),
                    coverUrl = "",
                    title = "Facebook Video",
                    source = "Facebook"
                )
            }

            // Método 2: Fallback a FDOWN
            val formBody = FormBody.Builder().add("url", url).build()
            val req2 = Request.Builder()
                .url("https://fdown.net/download.php")
                .post(formBody)
                .build()
            val res2 = client.newCall(req2).execute()
            val html2 = res2.body?.string() ?: ""
            val fdownUrl = findMatch(html2, "id=\"hdlink\" href=\"(.*?)\"") ?: findMatch(html2, "id=\"sdlink\" href=\"(.*?)\"")
            
            if (fdownUrl != null) {
                VideoData(fdownUrl.replace("&amp;", "&"), "", "Facebook Video", "Facebook")
            } else null
        } catch (e: Exception) { null }
    }

    private fun extractInstagram(url: String): VideoData? {
        return try {
            val request = Request.Builder()
                .url("https://indown.io/download?link=$url")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: ""
            val videoUrl = findMatch(html, "href=\"(https://.*?\\.cdninstagram\\.com/.*?)\"")
            if (videoUrl != null) {
                VideoData(videoUrl, "", "Instagram Reel", "Instagram")
            } else VideoData(url, "", "Instagram Content", "Instagram")
        } catch (e: Exception) { null }
    }

    private fun findMatch(html: String, patternStr: String): String? {
        val pattern = Pattern.compile(patternStr)
        val matcher = pattern.matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }
}
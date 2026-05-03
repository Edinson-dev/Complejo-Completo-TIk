package com.example.tikdownloader.extractor

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class VideoData(
    val downloadUrl: String,
    val coverUrl: String,
    val title: String,
    val source: String,
    val isAudioOnly: Boolean = false,
    val authorName: String = "",
    val authorNickname: String = "",
    val authorAvatar: String = ""
)

object VideoExtractor {
    private const val TAG = "TIK_EXTRACTOR"
    
    private val client = getUnsafeOkHttpClient()
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder().build()
        }
    }

    suspend fun extract(url: String, audioOnly: Boolean = false): VideoData? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim().split("?")[0]
        if (cleanUrl.contains("tiktok.com")) {
            return@withContext extractTikTok(cleanUrl, audioOnly)
        }
        return@withContext null
    }

    private fun extractTikTok(url: String, audioOnly: Boolean): VideoData? {
        try {
            val request = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$url")
                .header("User-Agent", USER_AGENT)
                .build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.optInt("code") == 0) {
                val data = json.getJSONObject("data")
                val author = data.optJSONObject("author")
                return VideoData(
                    downloadUrl = if (audioOnly) data.optString("music") else data.getString("play"),
                    coverUrl = "https://www.tikwm.com" + data.optString("cover"),
                    title = data.optString("title", "TikTok Video"),
                    source = "TikTok",
                    isAudioOnly = audioOnly,
                    authorName = author?.optString("unique_id") ?: "",
                    authorNickname = author?.optString("nickname") ?: "",
                    authorAvatar = author?.optString("avatar") ?: ""
                )
            }
        } catch (e: Exception) { 
            Log.e(TAG, "TikTok Error: ${e.message}")
        }
        
        // Fallback simple a Cobalt
        return try {
            val jsonBody = JSONObject().apply { 
                put("url", url)
                put("vCodec", "h264") 
            }
            val request = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()
            
            val response = client.newCall(request).execute()
            val jsonObj = JSONObject(response.body?.string() ?: "")
            if (jsonObj.has("url")) {
                VideoData(
                    downloadUrl = jsonObj.getString("url"),
                    coverUrl = "",
                    title = "TikTok Video",
                    source = "TikTok"
                )
            } else null
        } catch (e: Exception) { null }
    }
}

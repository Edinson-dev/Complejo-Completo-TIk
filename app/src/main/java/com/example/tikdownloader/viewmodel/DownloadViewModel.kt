package com.example.tikdownloader.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikdownloader.extractor.VideoData
import com.example.tikdownloader.extractor.VideoExtractor
import com.example.tikdownloader.utils.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    object Extracting : DownloadState()
    object Downloading : DownloadState()
    data class Success(val videoData: VideoData) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionName: String,
    val updateUrl: String
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadHelper = DownloadHelper(application)
    private val prefs = application.getSharedPreferences("tik_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState

    private val _history = MutableStateFlow<List<VideoData>>(emptyList())
    val history: StateFlow<List<VideoData>> = _history

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly

    private val _isHighQuality = MutableStateFlow(true)
    val isHighQuality: StateFlow<Boolean> = _isHighQuality

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _totalSavedMB = MutableStateFlow(0L)
    val totalSavedMB: StateFlow<Long> = _totalSavedMB

    private val _totalDownloads = MutableStateFlow(0)
    val totalDownloads: StateFlow<Int> = _totalDownloads

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val jsonStr = withContext(Dispatchers.IO) {
                    URL("https://tik-downloader-five.vercel.app/version.json").readText()
                }
                val json = JSONObject(jsonStr)
                val latestCode = json.getInt("latestVersionCode")
                val latestName = json.getString("latestVersionName")
                val url = json.getString("updateUrl")
                
                val currentCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionCode
                }

                if (latestCode > currentCode) {
                    _updateInfo.value = UpdateInfo(true, latestName, url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun performCleanup() {
        viewModelScope.launch {
            val freed = downloadHelper.performSystemCleanup()
            _totalSavedMB.value += freed
        }
    }

    fun downloadUpdate(url: String) {
        viewModelScope.launch {
            downloadHelper.enqueueDownload(url, "Update", "TikDownloader_New.apk")
            _updateInfo.value = null
        }
    }

    fun setAudioOnly(enabled: Boolean) {
        _isAudioOnly.value = enabled
    }

    fun setHighQuality(enabled: Boolean) {
        _isHighQuality.value = enabled
    }

    fun extractAndDownload(url: String) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = DownloadState.Extracting
            try {
                val videoData = VideoExtractor.extract(
                    url = url,
                    audioOnly = _isAudioOnly.value,
                    highQuality = _isHighQuality.value
                )
                if (videoData != null) {
                    _uiState.value = DownloadState.Downloading
                    downloadHelper.enqueueDownload(
                        url = videoData.downloadUrl,
                        source = videoData.source
                    )
                    _totalDownloads.value += 1
                    
                    _uiState.value = DownloadState.Success(videoData)
                    
                    if (!_history.value.any { it.downloadUrl == videoData.downloadUrl }) {
                        _history.value = (listOf(videoData) + _history.value).take(10)
                    }
                } else {
                    _uiState.value = DownloadState.Error("LINK DE TIKTOK NO VÁLIDO")
                }
            } catch (e: Exception) {
                _uiState.value = DownloadState.Error("ERROR DE CONEXIÓN")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = DownloadState.Idle
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    fun removeFromHistory(videoData: VideoData) {
        _history.value = _history.value.filter { it.downloadUrl != videoData.downloadUrl }
    }
}

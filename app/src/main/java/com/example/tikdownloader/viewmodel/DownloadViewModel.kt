package com.example.tikdownloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tikdownloader.extractor.VideoData
import com.example.tikdownloader.extractor.VideoExtractor
import com.example.tikdownloader.utils.DownloadHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DownloadState {
    object Idle : DownloadState()
    object Extracting : DownloadState()
    object Downloading : DownloadState()
    data class Success(val videoData: VideoData) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadHelper = DownloadHelper(application)
    
    private val _uiState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val uiState: StateFlow<DownloadState> = _uiState

    private val _history = MutableStateFlow<List<VideoData>>(emptyList())
    val history: StateFlow<List<VideoData>> = _history

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly

    fun setAudioOnly(enabled: Boolean) {
        _isAudioOnly.value = enabled
    }

    fun extractAndDownload(url: String) {
        if (url.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = DownloadState.Extracting
            try {
                val videoData = VideoExtractor.extract(url, _isAudioOnly.value)
                if (videoData != null) {
                    downloadHelper.enqueueDownload(
                        url = videoData.downloadUrl,
                        source = videoData.source
                    )
                    _uiState.value = DownloadState.Success(videoData)
                    if (!_history.value.any { it.downloadUrl == videoData.downloadUrl }) {
                        _history.value = (listOf(videoData) + _history.value).take(10)
                    }
                } else {
                    _uiState.value = DownloadState.Error("No se pudo extraer el contenido")
                }
            } catch (e: Exception) {
                _uiState.value = DownloadState.Error(e.message ?: "Error desconocido")
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
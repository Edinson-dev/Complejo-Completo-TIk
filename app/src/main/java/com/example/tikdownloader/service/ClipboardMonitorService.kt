package com.example.tikdownloader.service

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tikdownloader.R
import com.example.tikdownloader.extractor.VideoExtractor
import com.example.tikdownloader.utils.DownloadHelper
import kotlinx.coroutines.*

class ClipboardMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var clipboard: ClipboardManager
    private lateinit var downloadHelper: DownloadHelper

    override fun onCreate() {
        super.onCreate()
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        downloadHelper = DownloadHelper(this)
        
        createNotificationChannel()
        startForeground(1, createNotification("Modo Fantasma Activo", "Escuchando enlaces..."))
        
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val item = clipboard.primaryClip?.getItemAt(0)
        val text = item?.text?.toString() ?: ""
        
        if (isValidUrl(text)) {
            processDownload(text)
        }
    }

    private fun isValidUrl(text: String): Boolean {
        return text.contains("tiktok.com") || 
               text.contains("instagram.com") || 
               text.contains("facebook.com") || 
               text.contains("fb.watch") ||
               text.contains("fb.com")
    }

    private fun processDownload(url: String) {
        serviceScope.launch {
            updateNotification("Extrayendo...", url)
            val videoData = VideoExtractor.extract(url)
            if (videoData != null) {
                downloadHelper.enqueueDownload(videoData.downloadUrl)
                updateNotification("✅ Descarga Iniciada", videoData.title)
                
                // Limpieza Automática tras descargar
                val freed = downloadHelper.performSystemCleanup()
                if (freed > 0) {
                    android.util.Log.d("GHOST_MODE", "Auto-Limpieza: ${freed}MB liberados tras descarga.")
                }
            } else {
                updateNotification("❌ Error", "No se pudo extraer el video")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ghost_mode_channel",
                "TikDownloader Ghost Mode",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, "ghost_mode_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        clipboard.removePrimaryClipChangedListener(clipboardListener)
        serviceScope.cancel()
        super.onDestroy()
    }
}
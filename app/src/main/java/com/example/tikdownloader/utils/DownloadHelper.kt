package com.example.tikdownloader.utils

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import java.io.File

class DownloadHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "download_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descargas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de descargas de videos"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun enqueueDownload(url: String, source: String = "General", fileName: String? = null): String {
        val extension = if (url.contains(".mp3") || url.contains("music")) "mp3" else "mp4"
        val finalFileName = fileName ?: "TikDown_${System.currentTimeMillis()}.$extension"
        val subDir = "TikDownloader/$source"
        val relativePath = "$subDir/$finalFileName"
        
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(finalFileName)
            .setDescription("Descargando desde $source")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, relativePath)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)
        
        // Opcional: Podrías monitorear el progreso aquí o simplemente confiar en DownloadManager
        // que ya muestra una notificación nativa. Para hacerlo más "Pro", personalizamos la notificación inicial.
        
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), relativePath).absolutePath
    }
}

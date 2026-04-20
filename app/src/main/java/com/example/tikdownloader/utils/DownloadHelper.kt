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
        
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), relativePath).absolutePath
    }

    /**
     * PURGA DE SISTEMA PRO: Limpieza multifuncional profunda con logs de auditoría
     * @return Cantidad de MB liberados
     */
    fun performSystemCleanup(): Long {
        var totalBytesFreed: Long = 0
        android.util.Log.d("CLEANER", "--- INICIANDO PURGA DE SISTEMA ---")

        // 1. Limpiar Caché Interna y de UI
        val internalCache = deleteDirContent(context.cacheDir)
        totalBytesFreed += internalCache
        android.util.Log.d("CLEANER", "Caché Interna: ${internalCache / 1024} KB liberados")

        // 2. Limpieza de Descargas Basura (APKs, Tmp, Logs)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir?.listFiles()?.forEach { file ->
            val name = file.name.lowercase()
            if (name.endsWith(".apk") || name.endsWith(".tmp") || name.endsWith(".log") || name.contains("temp_")) {
                val size = file.length()
                if (file.delete()) {
                    totalBytesFreed += size
                    android.util.Log.d("CLEANER", "Eliminado archivo basura: ${file.name} (${size / 1024} KB)")
                }
            }
        }

        // 3. Limpieza de Carpetas Vacías
        val appDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "TikDownloader")
        if (appDir.exists()) {
            val emptyDirFreed = cleanEmptyDirectories(appDir)
            totalBytesFreed += emptyDirFreed
        }

        android.util.Log.d("CLEANER", "--- PURGA COMPLETADA: ${totalBytesFreed / (1024 * 1024)} MB TOTALES ---")
        
        System.gc()
        return totalBytesFreed / (1024 * 1024)
    }

    private fun cleanEmptyDirectories(directory: File): Long {
        var freed: Long = 0
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                freed += cleanEmptyDirectories(file)
                if (file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            }
        }
        return freed
    }

    private fun deleteDirContent(dir: File?): Long {
        var bytes: Long = 0
        if (dir != null && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    bytes += deleteDirContent(file)
                } else {
                    val size = file.length()
                    if (file.delete()) {
                        bytes += size
                    }
                }
            }
        }
        return bytes
    }
}

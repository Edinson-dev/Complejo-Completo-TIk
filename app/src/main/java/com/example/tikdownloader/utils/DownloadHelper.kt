package com.example.tikdownloader.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

class DownloadHelper(private val context: Context) {
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
        dm.enqueue(request)
        
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), relativePath).absolutePath
    }
}
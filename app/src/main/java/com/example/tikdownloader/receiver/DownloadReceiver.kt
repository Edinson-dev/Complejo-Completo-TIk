package com.example.tikdownloader.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            Toast.makeText(context, "Descarga Finalizada! Revisa tu galería", Toast.LENGTH_LONG).show()
        }
    }
}
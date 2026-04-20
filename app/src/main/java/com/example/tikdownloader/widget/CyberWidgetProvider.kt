package com.example.tikdownloader.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tikdownloader.R
import com.example.tikdownloader.ui.MainActivity

import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.*
import java.io.File

import com.example.tikdownloader.utils.DownloadHelper
import android.widget.Toast

class CyberWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_CLEANUP") {
            val helper = DownloadHelper(context)
            val freedMB = helper.performSystemCleanup()
            Toast.makeText(context, "SISTEMA PURGADO: ${freedMB}MB LIBERADOS 🧹", Toast.LENGTH_SHORT).show()
            
            // Actualizar el widget después de limpiar
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, CyberWidgetProvider::class.java.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.cyber_widget)

        // 1. Obtener Hora Actual
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.widget_clock, time)

        // 2. Obtener Espacio en Disco Real
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
        val megAvailable = bytesAvailable / (1024 * 1024 * 1024) // GB
        val percentUsed = 100 - ((bytesAvailable.toFloat() / bytesTotal.toFloat()) * 100).toInt()
        
        views.setTextViewText(R.id.disk_progress_text, "DISK_USAGE: ${megAvailable}GB FREE")
        views.setProgressBar(R.id.disk_progress, 100, percentUsed, false)

        // 3. Botón de Limpieza (Nuevo)
        val cleanIntent = Intent(context, CyberWidgetProvider::class.java).apply {
            action = "ACTION_CLEANUP"
        }
        val cleanPendingIntent = PendingIntent.getBroadcast(context, 2, cleanIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_clean_system, cleanPendingIntent)

        // 4. Al hacer clic en el logo, abrir la App
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_logo, mainPendingIntent)

        // 5. Al hacer clic en el botón de descarga rápida
        val downloadIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "QUICK_DOWNLOAD")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val downloadPendingIntent = PendingIntent.getActivity(context, 1, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_quick_download, downloadPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

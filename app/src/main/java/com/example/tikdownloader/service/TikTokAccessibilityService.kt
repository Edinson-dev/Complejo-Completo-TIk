package com.example.tikdownloader.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.example.tikdownloader.R
import com.example.tikdownloader.ui.QuickDownloadActivity
import com.example.tikdownloader.utils.OverlayPermissionHelper

/**
 * TikTokAccessibilityService
 *
 * Responsabilidades:
 *  1. Monitorear qué app está en primer plano.
 *  2. Mostrar/ocultar el botón flotante (overlay) según la app activa.
 *  3. Al pulsar el botón, leer el portapapeles y lanzar la descarga.
 *
 * ⚠️  El usuario debe:
 *   a) Activar el servicio en: Ajustes → Accesibilidad → VideoDownloader
 *   b) Conceder permiso "Mostrar sobre otras apps" (SYSTEM_ALERT_WINDOW)
 */
class TikTokAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TikTokA11yService"

        // Paquetes que activan el botón flotante
        val TARGET_PACKAGES = setOf(
            "com.zhiliaoapp.musically",   // TikTok (global)
            "com.ss.android.ugc.trill"    // TikTok (algunas regiones)
        )

        // Intent action para comunicarse con la UI
        const val ACTION_QUICK_DOWNLOAD = "com.example.tikdownloader.QUICK_DOWNLOAD"
        const val EXTRA_URL = "extra_url"

        // Singleton para saber si el servicio está activo (útil para la UI)
        @Volatile
        var isRunning = false
            private set
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Estado del overlay
    // ──────────────────────────────────────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayVisible = false
    private var currentPackage: String? = null

    // Posición del botón flotante (arrastrable)
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        configureAccessibilityService()
        Log.i(TAG, "Servicio de accesibilidad conectado ✓")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeOverlay()
        Log.i(TAG, "Servicio de accesibilidad destruido")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Configuración dinámica (alternativa al XML, más flexible)
    // ──────────────────────────────────────────────────────────────────────────

    private fun configureAccessibilityService() {
        serviceInfo = serviceInfo?.apply {
            // Escuchar cambios de ventana y de paquete en primer plano
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Monitorear TODOS los paquetes (luego filtramos en onAccessibilityEvent)
            // Si solo quisieras TikTok, añade: packageNames = arrayOf("com.zhiliaoapp.musically")
            packageNames = null // null = monitorear todo

            notificationTimeout = 100L // ms entre eventos
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Procesamiento de eventos de accesibilidad
    // ──────────────────────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Solo nos interesan cambios de ventana
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackage) return // Sin cambio

        currentPackage = packageName
        Log.d(TAG, "App en primer plano: $packageName")

        if (packageName in TARGET_PACKAGES) {
            // Nuestra app objetivo está activa → mostrar overlay
            if (!isOverlayVisible) showOverlay()
        } else {
            // Otra app → ocultar overlay
            if (isOverlayVisible) hideOverlay()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servicio interrumpido")
        hideOverlay()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Overlay: mostrar / ocultar
    // ──────────────────────────────────────────────────────────────────────────

    private fun showOverlay() {
        if (!OverlayPermissionHelper.hasPermission(this)) {
            Log.w(TAG, "Sin permiso SYSTEM_ALERT_WINDOW — no se puede mostrar overlay")
            return
        }
        if (overlayView != null) {
            // Ya existe, solo hacerlo visible
            overlayView?.visibility = View.VISIBLE
            isOverlayVisible = true
            return
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16   // margen derecho (dp → se convierte luego)
            y = 300  // posición vertical inicial
        }

        overlayView = createOverlayButton()
        setupDragging(overlayView!!, params)

        try {
            windowManager?.addView(overlayView, params)
            isOverlayVisible = true
            Log.i(TAG, "Overlay mostrado ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir overlay: ${e.message}")
        }
    }

    private fun hideOverlay() {
        overlayView?.visibility = View.GONE
        isOverlayVisible = false
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar overlay: ${e.message}")
            }
        }
        overlayView = null
        isOverlayVisible = false
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Construcción del botón flotante
    // ──────────────────────────────────────────────────────────────────────────

    private fun createOverlayButton(): View {
        // Contenedor
        val container = FrameLayout(this)

        // Botón circular con icono de descarga
        val button = ImageView(this).apply {
            setImageResource(R.drawable.ic_download_bubble)
            setBackgroundResource(R.drawable.bg_download_bubble)
            val sizePx = (64 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            elevation = 8f * resources.displayMetrics.density
            contentDescription = getString(R.string.overlay_button_description)
        }

        container.addView(button)

        // Click: leer portapapeles y lanzar descarga
        button.setOnClickListener {
            vibrate()
            val url = readClipboard()
            if (url != null && isValidVideoUrl(url)) {
                launchQuickDownload(url)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.overlay_no_url_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return container
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrastre del botón flotante
    // ──────────────────────────────────────────────────────────────────────────

    private fun setupDragging(view: View, params: WindowManager.LayoutParams) {
        var hasMoved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    false // No consumir para que el click también funcione
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    // Solo considerar "arrastre" si se movió más de 10px
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        hasMoved = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(view, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> hasMoved // Si se movió, consumir el evento (no disparar click)

                else -> false
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Portapapeles
    // ──────────────────────────────────────────────────────────────────────────

    private fun readClipboard(): String? {
        return try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0)?.coerceToText(this)?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo portapapeles: ${e.message}")
            null
        }
    }

    /**
     * Valida que el texto del portapapeles sea una URL de TikTok.
     */
    private fun isValidVideoUrl(url: String): Boolean {
        val patterns = listOf(
            "tiktok.com",
            "vm.tiktok.com",
            "vt.tiktok.com"
        )
        return patterns.any { url.contains(it, ignoreCase = true) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lanzar descarga rápida
    // ──────────────────────────────────────────────────────────────────────────

    private fun launchQuickDownload(url: String) {
        Log.i(TAG, "Lanzando descarga rápida para: $url")

        val intent = Intent(this, QuickDownloadActivity::class.java).apply {
            action = ACTION_QUICK_DOWNLOAD
            putExtra(EXTRA_URL, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Vibración (feedback háptico)
    // ──────────────────────────────────────────────────────────────────────────

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo vibrar: ${e.message}")
        }
    }
}
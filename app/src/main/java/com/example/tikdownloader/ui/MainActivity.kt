package com.example.tikdownloader.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.tikdownloader.ui.components.*
import com.example.tikdownloader.ui.theme.*
import com.example.tikdownloader.viewmodel.DownloadState
import com.example.tikdownloader.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Forzar Modo Fantasma siempre encendido
        val ghostIntent = Intent(this, com.example.tikdownloader.service.ClipboardMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(ghostIntent)
        } else {
            startService(ghostIntent)
        }

        setContent {
            var urlText by remember { mutableStateOf("") }
            var showBrowser by remember { mutableStateOf(false) }
            val uiState by viewModel.uiState.collectAsState()
            val updateInfo by viewModel.updateInfo.collectAsState()
            val history by viewModel.history.collectAsState()
            val isAudioOnly by viewModel.isAudioOnly.collectAsState()
            val haptic = LocalHapticFeedback.current
            val context = LocalContext.current
            
            // Obtener versión real del sistema
            val packageInfo = remember { context.packageManager.getPackageInfo(context.packageName, 0) }
            val currentVersionName = packageInfo.versionName ?: "1.0"
            
            val lifecycleOwner = LocalLifecycleOwner.current
            var lastProcessedUrl by remember { mutableStateOf("") }
            
            // Lógica de detección de links y chequeo de actualización al volver a la App
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        // 1. DETECCIÓN DE PORTAPAPELES
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val item = clipboard.primaryClip?.getItemAt(0)
                        val pasteData = item?.text?.toString() ?: ""
                        
                        val isValidUrl = pasteData.contains("tiktok.com") || 
                                        pasteData.contains("instagram.com") || 
                                        pasteData.contains("facebook.com") || 
                                        pasteData.contains("fb.watch")

                        if (isValidUrl && pasteData != lastProcessedUrl) {
                            lastProcessedUrl = pasteData
                            urlText = pasteData
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Link detectado automáticamente 🚀", Toast.LENGTH_SHORT).show()
                            viewModel.extractAndDownload(pasteData)
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            TikDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF050505)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showBrowser) {
                            // ... (resto del SmartBrowser)
                        } else {
                            // Mostrar Dialogo de Actualización
                            updateInfo?.let { info ->
                                UpdateDialog(
                                    updateInfo = info,
                                    onDismiss = { viewModel.checkForUpdates() }, // Ocultar o re-chequear
                                    onUpdate = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                )
                            }

                            BackgroundDecor()

                            AnimatedContent(
                                targetState = uiState,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                                },
                                label = "MainContent"
                            ) { state ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(40.dp))
                                    FuturisticHeader()
                                    Spacer(modifier = Modifier.height(30.dp))

                                    when (state) {
                                        is DownloadState.Success -> {
                                            TransferSuccessView(
                                                videoData = state.videoData,
                                                onReturn = { viewModel.resetState() }
                                            )
                                        }
                                        is DownloadState.Downloading -> {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                RadarScanner()
                                                Spacer(modifier = Modifier.height(24.dp))
                                                LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                                    color = TikTokCyan,
                                                    trackColor = Color.White.copy(alpha = 0.1f)
                                                )
                                                Text("DESCARGANDO ARCHIVO...", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                                            }
                                        }
                                        else -> {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Button(
                                                    onClick = { showBrowser = true },
                                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.3f))
                                                ) {
                                                    Icon(Icons.Default.Public, null, tint = TikTokCyan, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("NAVEGADOR INTELIGENTE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }

                                                Spacer(modifier = Modifier.height(20.dp))

                                                InteractiveMainCard(
                                                    urlText = urlText,
                                                    onUrlChange = { urlText = it },
                                                    uiState = state,
                                                    onDownload = {
                                                        if (urlText.isNotBlank()) {
                                                            viewModel.extractAndDownload(urlText)
                                                            urlText = ""
                                                        }
                                                    }
                                                )

                                                Spacer(modifier = Modifier.height(30.dp))
                                                SocialBubbles(urlText)
                                                Spacer(modifier = Modifier.height(24.dp))

                                                SettingsSection(
                                                    isAudioOnly = isAudioOnly,
                                                    onAudioToggle = { viewModel.setAudioOnly(it) }
                                                )

                                                if (history.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(30.dp))
                                                    HistorySection(
                                                        history = history,
                                                        onItemClick = { viewModel.extractAndDownload(it.downloadUrl) },
                                                        onClearHistory = { viewModel.clearHistory() },
                                                        onRemoveItem = { viewModel.removeFromHistory(it) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(40.dp))
                                }
                            }
                        }

                        // Etiqueta de Versión en la esquina inferior izquierda
                        Text(
                            text = "v$currentVersionName",
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TikDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TikTokCyan,
            secondary = TikTokPink,
            background = Color(0xFF050505),
            surface = Color(0xFF111111)
        ),
        content = content
    )
}

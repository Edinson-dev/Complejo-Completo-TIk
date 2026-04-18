package com.example.tikdownloader.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.FileProvider
import android.net.Uri
import android.os.Environment
import java.io.File
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tikdownloader.extractor.VideoData
import com.example.tikdownloader.ui.theme.*
import com.example.tikdownloader.utils.OverlayPermissionHelper
import com.example.tikdownloader.viewmodel.DownloadState
import com.example.tikdownloader.viewmodel.DownloadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.widget.VideoView
import android.widget.MediaController
import androidx.activity.compose.BackHandler
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val history by viewModel.history.collectAsState()
            val isAudioOnly by viewModel.isAudioOnly.collectAsState()
            val haptic = LocalHapticFeedback.current
            val context = LocalContext.current
            val currentVersion = "1.2" // Versión actual
            var showUpdateDialog by remember { mutableStateOf(false) }
            var newVersionName by remember { mutableStateOf("") }

            val lifecycleOwner = LocalLifecycleOwner.current
            var lastProcessedUrl by remember { mutableStateOf("") }
            
            // Lógica de detección de links y chequeo de actualización al volver a la App
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        // 1. CHEQUEO DE ACTUALIZACIÓN
                        lifecycleOwner.lifecycleScope.launch {
                            val versionEnServidor = withContext(Dispatchers.IO) {
                                try {
                                    val client = OkHttpClient()
                                    val request = Request.Builder()
                                        .url("https://tik-downloader-five.vercel.app/version.json")
                                        .build()
                                    val response = client.newCall(request).execute()
                                    val json = JSONObject(response.body?.string() ?: "")
                                    json.getString("latestVersionName")
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (versionEnServidor != null && versionEnServidor > currentVersion) {
                                newVersionName = versionEnServidor
                                showUpdateDialog = true
                            }
                        }

                        // 2. DETECCIÓN DE PORTAPAPELES
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
                            SmartBrowser(
                                onUrlDetected = { 
                                    urlText = it
                                    viewModel.extractAndDownload(it)
                                    showBrowser = false
                                },
                                onBack = { showBrowser = false }
                            )
                        } else {
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

                        // Update Dialog Overlay
                        if (showUpdateDialog) {
                            UpdateDialog(
                                version = newVersionName,
                                onDismiss = { showUpdateDialog = false },
                                onUpdate = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tik-downloader-five.vercel.app/"))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // Etiqueta de Versión en la esquina inferior izquierda
                        Text(
                            text = "v$currentVersion",
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
fun UpdateDialog(version: String, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f).border(1.dp, TikTokCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SystemUpdate, null, tint = TikTokCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("ACTUALIZACIÓN CYBER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp)
                Text("Versión $version disponible", color = TikTokCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Hemos optimizado los motores de extracción para mayor velocidad.", color = Color.Gray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACTUALIZAR AHORA", fontWeight = FontWeight.Black, color = Color.Black)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("MÁS TARDE", color = Color.DarkGray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(isAudioOnly: Boolean, onAudioToggle: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.03f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = TikTokCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("SOLO AUDIO (MP3)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Switch(checked = isAudioOnly, onCheckedChange = onAudioToggle, colors = SwitchDefaults.colors(checkedThumbColor = TikTokCyan))
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.03f)).border(1.dp, TikTokPink.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = TikTokPink, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("MODO FANTASMA", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("ESCUCHANDO PORTAPAPELES...", color = TikTokPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse))
            Icon(Icons.Default.RadioButtonChecked, null, tint = TikTokPink, modifier = Modifier.size(20.dp).graphicsLayer(scaleX = scale, scaleY = scale))
        }
    }
}

@Composable
fun FuturisticHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp).background(Color.Black, CircleShape).border(2.dp, Color.Red, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DownloadForOffline, null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, tint = Color.Red, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("TIK", fontWeight = FontWeight.Black, fontSize = 32.sp, color = Color.Red, letterSpacing = 2.sp)
            Text("DOWNLOADER", fontWeight = FontWeight.Light, fontSize = 32.sp, color = Color.White, letterSpacing = 1.sp)
        }
        Text("by edinson_dev • todos los derechos reservados", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)
    }
}

@Composable
fun InteractiveMainCard(urlText: String, onUrlChange: (String) -> Unit, uiState: DownloadState, onDownload: () -> Unit) {
    val isExtracting = uiState is DownloadState.Extracting
    val infiniteTransition = rememberInfiniteTransition()
    val borderGlow by infiniteTransition.animateColor(initialValue = TikTokCyan.copy(alpha = 0.4f), targetValue = TikTokPink.copy(alpha = 0.4f), animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse))

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.5.dp, borderGlow)) {
        Column(modifier = Modifier.padding(24.dp).animateContentSize()) {
            if (isExtracting) {
                RadarScanner()
            } else {
                Text("SISTEMA DE EXTRACCIÓN", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(value = urlText, onValueChange = onUrlChange, placeholder = { Text("Pega el enlace aquí...", color = Color.DarkGray) }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = TikTokCyan, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White), singleLine = true)
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (urlText.isBlank()) Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF1A1A1A))) else Brush.linearGradient(listOf(TikTokCyan, TikTokPink))).clickable(enabled = urlText.isNotBlank()) { onDownload() }, contentAlignment = Alignment.Center) {
                    Text("INICIAR DESCARGA ULTRA", color = if (urlText.isBlank()) Color.Gray else Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun BackgroundDecor() {
    val infiniteTransition = rememberInfiniteTransition()
    val particles = remember { List(20) { Offset((0..1000).random().toFloat(), (0..2000).random().toFloat()) } }
    val particleAlpha by infiniteTransition.animateFloat(initialValue = 0.1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse))
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(400.dp).offset(x = (-150).dp, y = (-100).dp).background(TikTokCyan.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.size(300.dp).align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).background(TikTokPink.copy(alpha = 0.05f), CircleShape))
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { offset -> drawCircle(color = TikTokCyan.copy(alpha = particleAlpha), radius = 1.5f, center = offset) }
        }
    }
}

@Composable
fun SmartBrowser(onUrlDetected: (String) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("SMART BROWSER", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("TIK/FB/IG", color = TikTokCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        AndroidView(
            modifier = Modifier.weight(1f), 
            factory = { context -> 
                WebView(context).apply { 
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl("https://www.google.com")
                    webView = this
                } 
            }
        )
        Button(
            onClick = { 
                webView?.url?.let { currentUrl ->
                    onUrlDetected(currentUrl)
                }
            }, 
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = TikTokPink), 
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("CAPTURAR Y DESCARGAR", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun RadarScanner() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(1500, easing = LinearEasing)))
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) { drawCircle(color = TikTokCyan.copy(alpha = 0.1f), radius = size.minDimension / 2) }
            Box(modifier = Modifier.size(100.dp).rotate(rotation).background(Brush.sweepGradient(listOf(Color.Transparent, TikTokCyan, Color.Transparent)), CircleShape))
            Icon(Icons.Default.WifiTethering, null, tint = TikTokCyan, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("BUSCANDO STREAM HD...", color = TikTokCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
fun SocialBubbles(currentUrl: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SocialBubble("TikTok", TikTokCyan, currentUrl.contains("tiktok"))
        SocialBubble("Insta", TikTokPink, currentUrl.contains("instagram"))
        SocialBubble("FB", Color(0xFF1877F2), currentUrl.contains("facebook") || currentUrl.contains("fb"))
    }
}

@Composable
fun SocialBubble(label: String, color: Color, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(46.dp).background(color.copy(alpha = if (isActive) 0.2f else 0.05f), CircleShape).border(1.dp, color.copy(alpha = if (isActive) 1f else 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Text(label.take(1), color = color, fontWeight = FontWeight.Bold)
        }
        Text(label, color = color.copy(alpha = if (isActive) 1f else 0.4f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun HistorySection(history: List<VideoData>, onItemClick: (VideoData) -> Unit, onClearHistory: () -> Unit, onRemoveItem: (VideoData) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORIAL DE DESCARGAS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            TextButton(onClick = onClearHistory) { Text("LIMPIAR TODO", color = TikTokPink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(history) { video ->
                Box(modifier = Modifier.size(height = 110.dp, width = 85.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F0F0F)).border(1.dp, when(video.source) { "TikTok" -> TikTokCyan.copy(alpha = 0.3f); "Facebook" -> Color(0xFF1877F2).copy(alpha = 0.3f); "Instagram" -> TikTokPink.copy(alpha = 0.3f); else -> Color.White.copy(alpha = 0.1f) }, RoundedCornerShape(16.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onItemClick(video) }) {
                    AsyncImage(model = video.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.7f)
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(vertical = 4.dp), contentAlignment = Alignment.Center) { Text(video.source.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { onRemoveItem(video) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(videoPath: String, modifier: Modifier = Modifier) {
    AndroidView(factory = { ctx -> VideoView(ctx).apply { setVideoPath(videoPath); val controller = MediaController(ctx); controller.setAnchorView(this); setMediaController(controller); setOnPreparedListener { it.isLooping = true } } }, modifier = modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
}

@Composable
fun TransferSuccessView(videoData: VideoData, onReturn: () -> Unit) {
    val context = LocalContext.current
    val videoFile = remember(videoData) { val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES); moviesDir.listFiles()?.filter { it.extension == "mp4" }?.maxByOrNull { it.lastModified() } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(260.dp).padding(10.dp)) {
            Box(modifier = Modifier.fillMaxSize().border(2.dp, Brush.linearGradient(listOf(TikTokCyan, TikTokPink)), RoundedCornerShape(20.dp)))
            if (videoFile != null && videoFile.exists()) { VideoPlayer(videoPath = videoFile.absolutePath, modifier = Modifier.padding(6.dp)) } else { AsyncImage(model = videoData.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("¡DESCARGA EXITOSA!", color = TikTokCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(videoData.title, color = Color.Gray, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReturn, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) { Text("NUEVO") }
            Button(onClick = { if (videoFile != null) { val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile); val intent = Intent(Intent.ACTION_SEND).apply { type = "video/mp4"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(Intent.createChooser(intent, "Compartir video")) } }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = TikTokPink), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("ENVIAR") }
        }
    }
}

@Composable
fun TikDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = TikTokCyan, secondary = TikTokPink, background = Color(0xFF050505), surface = Color(0xFF111111)), content = content)
}

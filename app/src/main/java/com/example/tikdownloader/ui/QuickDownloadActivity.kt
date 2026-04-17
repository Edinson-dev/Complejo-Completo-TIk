package com.example.tikdownloader.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tikdownloader.ui.theme.TikTokCyan
import com.example.tikdownloader.ui.theme.TikTokPink
import com.example.tikdownloader.viewmodel.DownloadState
import com.example.tikdownloader.viewmodel.DownloadViewModel
import kotlinx.coroutines.delay

class QuickDownloadActivity : ComponentActivity() {
    private val viewModel: DownloadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("EXTRA_URL") ?: ""

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val haptic = LocalHapticFeedback.current
            
            LaunchedEffect(uiState) {
                when(uiState) {
                    is DownloadState.Success -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(2000)
                        finish()
                    }
                    is DownloadState.Error -> {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    else -> {}
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                InteractiveDownloadPopup(
                    url = url,
                    uiState = uiState,
                    onDownload = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.extractAndDownload(url) 
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
fun InteractiveDownloadPopup(
    url: String, 
    uiState: DownloadState, 
    onDownload: () -> Unit, 
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .border(1.0.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con indicador de fuente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sourceIcon = when {
                        url.contains("tiktok") -> Icons.Default.Bolt
                        url.contains("fb") || url.contains("facebook") -> Icons.Default.Public
                        else -> Icons.Default.Download
                    }
                    Icon(sourceIcon, null, tint = TikTokCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TikDownloader", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(400)) + slideInVertically { it / 2 } togetherWith fadeOut(tween(400))
                },
                label = "PopupState"
            ) { state ->
                when (state) {
                    is DownloadState.Idle -> {
                        Column {
                            Text("Enlace detectado", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                url, color = TikTokCyan, fontSize = 12.sp, maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onDownload,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text("DESCARGAR AHORA", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        }
                    }

                    is DownloadState.Extracting, is DownloadState.Downloading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TikTokCyan, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (state is DownloadState.Extracting) "EXTRAYENDO..." else "DESCARGANDO...",
                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                            )
                        }
                    }

                    is DownloadState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00FFA3), modifier = Modifier.size(50.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("LISTO EN GALERÍA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }

                    is DownloadState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, null, tint = TikTokPink, modifier = Modifier.size(40.dp))
                            Text(state.message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            TextButton(onClick = onDismiss) { Text("REINTENTAR", color = TikTokCyan) }
                        }
                    }
                }
            }
        }
    }
}

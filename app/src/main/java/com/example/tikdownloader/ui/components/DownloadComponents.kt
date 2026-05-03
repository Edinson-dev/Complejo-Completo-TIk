package com.example.tikdownloader.ui.components

import android.content.Intent
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.tikdownloader.extractor.VideoData
import com.example.tikdownloader.ui.theme.TikTokCyan
import com.example.tikdownloader.ui.theme.TikTokPink
import com.example.tikdownloader.viewmodel.DownloadState
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.animateContentSize

@Composable
fun InteractiveMainCard(urlText: String, onUrlChange: (String) -> Unit, uiState: DownloadState, onDownload: () -> Unit) {
    val isExtracting = uiState is DownloadState.Extracting
    val infiniteTransition = rememberInfiniteTransition(label = "cardGlow")
    val borderGlow by infiniteTransition.animateColor(
        initialValue = TikTokCyan.copy(alpha = 0.4f),
        targetValue = TikTokPink.copy(alpha = 0.4f),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "borderGlow"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.5.dp, borderGlow)
    ) {
        Column(modifier = Modifier.padding(24.dp).animateContentSize()) {
            if (isExtracting) {
                RadarScanner()
            } else {
                Text("TIKTOK DOWNLOADER PRO", color = TikTokCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = urlText,
                    onValueChange = onUrlChange,
                    placeholder = { Text("Pega el enlace de TikTok...", color = Color.DarkGray) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TikTokCyan,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (urlText.isBlank()) Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF1A1A1A))) else Brush.linearGradient(listOf(TikTokCyan, TikTokPink)))
                        .clickable(enabled = urlText.isNotBlank()) { onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DESCARGAR VIDEO", color = if (urlText.isBlank()) Color.Gray else Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun HistorySection(history: List<VideoData>, onItemClick: (VideoData) -> Unit, onClearHistory: () -> Unit, onRemoveItem: (VideoData) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORIAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            TextButton(onClick = onClearHistory) { Text("BORRAR", color = TikTokPink, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        LazyRow(contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(history) { video ->
                Box(modifier = Modifier.size(height = 110.dp, width = 85.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F0F0F)).border(1.dp, TikTokCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onItemClick(video) }) {
                    AsyncImage(model = video.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.7f)
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))).padding(vertical = 4.dp), contentAlignment = Alignment.Center) { Text("TIKTOK", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { onRemoveItem(video) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
                }
            }
        }
    }
}

@Composable
fun TransferSuccessView(videoData: VideoData, onReturn: () -> Unit) {
    val context = LocalContext.current
    val videoFile = remember(videoData) { 
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        moviesDir.listFiles()?.filter { it.extension == "mp4" }?.maxByOrNull { it.lastModified() } 
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(260.dp).padding(10.dp)) {
            Box(modifier = Modifier.fillMaxSize().border(2.dp, TikTokCyan, RoundedCornerShape(20.dp)))
            if (videoFile != null && videoFile.exists()) { 
                VideoPlayer(videoPath = videoFile.absolutePath, modifier = Modifier.padding(6.dp)) 
            } else { 
                AsyncImage(model = videoData.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) 
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("¡VIDEO DESCARGADO!", color = TikTokCyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(videoData.title, color = Color.Gray, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 20.dp))
        
        if (videoData.authorName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            AuthorCard(videoData)
        }

        Spacer(modifier = Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReturn, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) { Text("VOLVER") }
            Button(
                onClick = { 
                    if (videoFile != null) { 
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
                        val intent = Intent(Intent.ACTION_SEND).apply { 
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) 
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartir video")) 
                    } 
                }, 
                modifier = Modifier.weight(1f).height(50.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink), 
                shape = RoundedCornerShape(12.dp)
            ) { 
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("COMPARTIR") 
            }
        }
    }
}

@Composable
fun AuthorCard(videoData: VideoData) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val profileUrl = "https://www.tiktok.com/@${videoData.authorName}"
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(profileUrl))
                context.startActivity(intent)
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).border(2.dp, TikTokCyan, CircleShape).padding(2.dp).clip(CircleShape)) {
                AsyncImage(model = videoData.authorAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = videoData.authorNickname.ifEmpty { "Creador" }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = "@${videoData.authorName}", color = TikTokCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(TikTokCyan.copy(alpha = 0.1f)).border(1.dp, TikTokCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("PERFIL", color = TikTokCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun VideoPlayer(videoPath: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx -> 
            VideoView(ctx).apply { 
                setVideoPath(videoPath)
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setOnPreparedListener { it.isLooping = true } 
            } 
        }, 
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
    )
}

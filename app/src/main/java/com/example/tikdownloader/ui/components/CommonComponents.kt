package com.example.tikdownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tikdownloader.ui.theme.TikTokCyan
import com.example.tikdownloader.ui.theme.TikTokPink
import com.example.tikdownloader.viewmodel.UpdateInfo

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
fun BackgroundDecor() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val particles = remember { List(20) { Offset((0..1000).random().toFloat(), (0..2000).random().toFloat()) } }
    val particleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse),
        label = "particleAlpha"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.size(400.dp).offset(x = (-150).dp, y = (-100).dp).background(TikTokCyan.copy(alpha = 0.05f), CircleShape))
        Box(modifier = Modifier.size(300.dp).align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).background(TikTokPink.copy(alpha = 0.05f), CircleShape))
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { offset -> drawCircle(color = TikTokCyan.copy(alpha = particleAlpha), radius = 1.5f, center = offset) }
        }
    }
}

@Composable
fun RadarScanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "radarRotation"
    )
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
fun UpdateDialog(updateInfo: UpdateInfo, onDismiss: () -> Unit, onUpdate: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111111),
            border = BorderStroke(1.dp, TikTokCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Bolt, null, tint = TikTokCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "¡NUEVA VERSIÓN DISPONIBLE!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Versión ${updateInfo.versionName} lista para descargar. Incluye mejoras en la extracción y velocidad.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onUpdate(updateInfo.updateUrl) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACTUALIZAR AHORA", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("MÁS TARDE", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

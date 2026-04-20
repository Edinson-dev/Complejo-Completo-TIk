package com.example.tikdownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tikdownloader.ui.theme.TikTokCyan
import com.example.tikdownloader.ui.theme.TikTokPink

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
            val infiniteTransition = rememberInfiniteTransition(label = "ghostMode")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                label = "ghostScale"
            )
            Icon(Icons.Default.RadioButtonChecked, null, tint = TikTokPink, modifier = Modifier.size(20.dp).graphicsLayer(scaleX = scale, scaleY = scale))
        }
    }
}

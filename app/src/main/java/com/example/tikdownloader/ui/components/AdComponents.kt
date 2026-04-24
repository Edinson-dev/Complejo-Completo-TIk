package com.example.tikdownloader.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tikdownloader.ui.theme.TikTokCyan
import com.example.tikdownloader.ui.theme.TikTokPink
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

@Composable
fun CyberBannerAd(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black)
            .padding(top = 1.dp) // Espacio para el borde neón
    ) {
        // Línea Neón Superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TikTokCyan.copy(alpha = 0.5f))
        )

        AndroidView(
            modifier = Modifier.align(Alignment.Center),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    // ID de prueba de Google
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

@Composable
fun PremiumOfferButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(60.dp) // Un poco más alto
            .clip(RoundedCornerShape(16.dp))
            .background(TikTokPink.copy(alpha = 0.15f)) // Más visible
            .border(2.dp, TikTokPink, RoundedCornerShape(16.dp)) // Borde más grueso
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TikTokPink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = Color.Black, // Icono negro sobre fondo rosa
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SISTEMA ÉLITE ACTIVADO",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "ELIMINAR ANUNCIOS POR $4.000 COP",
                    color = TikTokPink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TikTokPink,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CyberPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val nequiNumber = "300 000 0000" // 👈 CAMBIA ESTO POR TU NÚMERO REAL

    AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(2.dp, TikTokPink, RoundedCornerShape(20.dp)),
        containerColor = Color(0xFF0D0D0D),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TikTokPink,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "PASARELA DE PAGO ÉLITE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Para eliminar los anuncios permanentemente, transfiere $4.000 COP a través de Nequi:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Caja de número Nequi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { 
                            clipboardManager.setText(AnnotatedString(nequiNumber))
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NÚMERO NEQUI", color = TikTokPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(nequiNumber, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(15.dp))
                
                Text(
                    "Una vez realizada la transferencia, pulsa el botón de abajo para activar tu licencia.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("VERIFICAR TRANSACCIÓN", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Text(
                "CANCELAR",
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp),
                fontSize = 12.sp
            )
        }
    )
}

// Lógica para cargar y mostrar el anuncio Intersticial
fun showInterstitial(activity: Activity) {
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
        activity,
        "ca-app-pub-3940256099942544/1033173712", // ID de prueba
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAd.show(activity)
            }
        }
    )
}

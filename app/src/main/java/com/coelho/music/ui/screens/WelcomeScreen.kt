package com.coelho.music.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coelho.music.R

@Composable
fun WelcomeScreen(denied: Boolean, onStart: () -> Unit) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(800), label = "entrada")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .25f * alpha), MaterialTheme.colorScheme.background)))
        .safeDrawingPadding().padding(28.dp).graphicsLayer { this.alpha = alpha; translationY = (1f - alpha) * 24f }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_coelho), "Coelho com fones", Modifier.size(148.dp))
        Spacer(Modifier.height(30.dp))
        Text("Coelho Music", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text("Sua música, do seu jeito.", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(52.dp))
        Text("Toda a sua biblioteca.\nSempre com você, mesmo offline.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, Modifier.fillMaxWidth().height(56.dp)) { Text("Começar") }
        Text("Acesso apenas aos seus áudios. Nenhum dado é enviado.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        if (denied) {
            Text("Precisamos da permissão de áudio para encontrar suas músicas.", textAlign = TextAlign.Center)
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Abrir permissões do aplicativo") }
        }
    }
}

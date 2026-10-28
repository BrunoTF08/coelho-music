package com.coelho.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelho.music.domain.model.formatTime
import com.coelho.music.player.controller.PlaybackController
import com.coelho.music.player.controller.PlaybackProgress
import kotlinx.coroutines.flow.StateFlow

@Composable
fun FavoriteButton(selected: Boolean, toggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (initialized && selected) {
            scale.animateTo(1.24f, spring(stiffness = 650f))
        }
        scale.animateTo(1f, spring(stiffness = 450f))
        initialized = true
    }
    val tint by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, label = "favorito")
    IconButton(onClick = toggle) {
        Icon(if (selected) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            if (selected) "Remover dos favoritos" else "Adicionar aos favoritos",
            Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }, tint = tint)
    }
}

@Composable
fun PlayerProgress(controller: PlaybackController, mediaId: String) {
    val progress by controller.progress.collectAsStateWithLifecycle()
    var seeking by remember(mediaId) { mutableStateOf<Float?>(null) }
    Slider(value = seeking ?: progress.position.toFloat().coerceIn(0f, progress.duration.coerceAtLeast(1).toFloat()),
        onValueChange = { seeking = it }, onValueChangeFinished = { seeking?.let { controller.seek(it.toLong()) }; seeking = null },
        valueRange = 0f..progress.duration.coerceAtLeast(1).toFloat(), enabled = progress.duration > 0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(seeking?.toLong() ?: progress.position), style = MaterialTheme.typography.labelSmall)
        Text(formatTime(progress.duration), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun MiniProgress(flow: StateFlow<PlaybackProgress>, modifier: Modifier = Modifier) {
    val progress by flow.collectAsStateWithLifecycle()
    LinearProgressIndicator(progress = { if (progress.duration > 0) (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f) else 0f }, modifier = modifier)
}

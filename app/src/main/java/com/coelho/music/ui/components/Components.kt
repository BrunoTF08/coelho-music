package com.coelho.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.coelho.music.player.controller.PlaybackProgress
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.coelho.music.domain.model.Song
import com.coelho.music.domain.model.formatTime
import com.coelho.music.player.controller.PlaybackState

@Composable
fun Action(icon: ImageVector, label: String, onClick: () -> Unit, selected: Boolean = false, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, label,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
}
@Composable
fun Artwork(model: Any?, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp))) {
        ArtPlaceholder()
        AsyncImage(model = model, contentDescription = "Capa do álbum", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
@Composable
private fun ArtPlaceholder() {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .5f), MaterialTheme.colorScheme.surface))), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.MusicNote, null, Modifier.fillMaxSize(.4f), tint = MaterialTheme.colorScheme.secondary)
    }
}
@Composable
fun SongRow(song: Song, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit, onAdd: () -> Unit, onQueue: () -> Unit,
    extra: (@Composable () -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Artwork(song.artwork, Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = favorite) { Icon(Icons.Rounded.Favorite, "Favorita", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
        Text(formatTime(song.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Action(Icons.Rounded.MoreVert, "Opções de ${song.title}", { menu = true })
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text(if (favorite) "Remover dos favoritos" else "Favoritar") }, onClick = { onFavorite(); menu = false })
                DropdownMenuItem(text = { Text("Adicionar à playlist") }, onClick = { onAdd(); menu = false })
                DropdownMenuItem(text = { Text("Adicionar à fila") }, onClick = { onQueue(); menu = false })
            }
        }
        extra?.invoke()
    }
}
@Composable
fun MiniPlayer(state: PlaybackState, progress: StateFlow<PlaybackProgress>, open: () -> Unit, toggle: () -> Unit, next: () -> Unit) {
    val item = state.current ?: return
    var drag by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var reset by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val threshold = with(LocalDensity.current) { 32.dp.toPx() }
    val draggableState = rememberDraggableState { delta -> drag = (drag + delta).coerceIn(-threshold * 4, threshold) }
    Surface(Modifier.padding(horizontal = 12.dp).fillMaxWidth()
        .graphicsLayer { translationY = drag * .18f }
        .draggable(draggableState, Orientation.Vertical, onDragStarted = { reset?.cancel(); drag = 0f }, onDragStopped = { velocity ->
            if (drag <= -threshold) { drag = 0f; open() }
            else reset = scope.launch { Animatable(drag).animateTo(0f, spring(stiffness = 450f), initialVelocity = velocity) { drag = value } }
        }).clickable(onClick = open), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.animateContentSize()) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(item.mediaMetadata.artworkUri, Modifier.size(46.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.mediaMetadata.title?.toString() ?: "Sem título", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(item.mediaMetadata.artist?.toString() ?: "", maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Action(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproduzir ou pausar", toggle)
                Action(Icons.Rounded.SkipNext, "Próxima", next)
            }
            MiniProgress(progress, Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

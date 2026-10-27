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
import coil.compose.SubcomposeAsyncImage
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
    SubcomposeAsyncImage(model = model, contentDescription = "Capa do álbum", contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(18.dp)), loading = { ArtPlaceholder() }, error = { ArtPlaceholder() })
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
fun MiniPlayer(state: PlaybackState, open: () -> Unit, toggle: () -> Unit, next: () -> Unit) {
    val item = state.current ?: return
    Surface(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clickable(onClick = open), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(item.mediaMetadata.artworkUri, Modifier.size(46.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(item.mediaMetadata.title?.toString() ?: "Sem título", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                    Text(item.mediaMetadata.artist?.toString() ?: "", maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Action(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproduzir ou pausar", toggle)
                Action(Icons.Rounded.SkipNext, "Próxima", next)
            }
            LinearProgressIndicator(progress = { if (state.duration > 0) (state.position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

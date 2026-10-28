package com.coelho.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelho.music.domain.model.Song
import com.coelho.music.domain.model.formatTime
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.*

@Composable
fun RecentlyAddedStrip(vm: MusicViewModel, viewAll: () -> Unit, openPlayer: () -> Unit) {
    val songs by vm.recentSongs.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.libraryError.collectAsStateWithLifecycle()
    val playback by vm.playback.state.collectAsStateWithLifecycle()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Adicionadas recentemente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = viewAll) { Text("Ver todas") }
    }
    when {
        loading && songs.isEmpty() -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp))
        error != null -> LibraryReadError(error!!, vm::refresh)
        songs.isEmpty() -> Text("Suas novas músicas aparecerão aqui.", Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> LazyRow(Modifier.padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(songs.take(10), key = { _, song -> song.id }, contentType = { _, _ -> "recent" }) { index, song ->
                Column(Modifier.width(144.dp).animateItem().clickable(enabled = playback.connected) {
                    vm.playback.play(songs, index); openPlayer()
                }) {
                    Artwork(song.artwork, Modifier.size(144.dp))
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(song.duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun LibraryReadError(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = retry) { Text("Tentar novamente") }
    }
}

@Composable
fun RecentSongsScreen(vm: MusicViewModel, back: () -> Unit, openPlayer: () -> Unit) {
    val songs by vm.recentSongs.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playback by vm.playback.state.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.libraryError.collectAsStateWithLifecycle()
    var add by remember { mutableStateOf<Song?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "heading") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Action(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", back)
                Text("Adicionadas recentemente", style = MaterialTheme.typography.titleLarge)
            }
            Text("Da mais nova para a mais antiga na biblioteca.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) LibraryReadError(error!!, vm::refresh)
            else if (!loading && songs.isEmpty()) Text("Suas novas músicas aparecerão aqui.", Modifier.padding(vertical = 32.dp))
        }
        itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
            SongRow(song, song.id in favorites,
                { if (playback.connected) { vm.playback.play(songs, index); openPlayer() } },
                { vm.favorite(song.id) }, { add = song }, { vm.playback.enqueue(song) })
        }
    }
    add?.let { song -> PlaylistPicker(vm, song) { add = null } }
}

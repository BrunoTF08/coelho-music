package com.coelho.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelho.music.R
import com.coelho.music.domain.model.*
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(vm: MusicViewModel, home: Boolean, openPlayer: () -> Unit, openPlaylists: () -> Unit, openRecent: () -> Unit) {
    val all by vm.songs.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val playback by vm.playback.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf("Todas") }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var add by remember { mutableStateOf<Song?>(null) }
    val songs = all
    val libraryError by vm.libraryError.collectAsStateWithLifecycle()
    val minimum = (settings?.minDuration ?: 0) * 1000L
    val filtered by produceState<List<Song>>(emptyList(), songs, query, tab, favorites, group, minimum) {
        if (query.isNotBlank()) delay(120)
        val key = query.searchKey()
        value = withContext(Dispatchers.Default) {
            songs.filter { it.duration >= minimum && (tab != "Favoritas" || it.id in favorites) &&
                (group == null || (if (tab == "Artistas") it.artist else it.album) == group) && it.searchText.contains(key) }
        }
    }
    val grouped = remember(filtered, tab) { filtered.groupBy { if (tab == "Artistas") it.artist else it.album } }
    val byId = remember(songs) { songs.associateBy { it.id } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_coelho), null, Modifier.size(38.dp))
                Text(if (home) "Coelho Music" else "Sua biblioteca", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Action(Icons.Rounded.Refresh, "Atualizar biblioteca", vm::refresh)
            }
            if (home) {
                Text("SEU UNIVERSO SONORO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text("Dê play no seu mundo.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
            }
            if (home && query.isBlank() && tab == "Todas") RecentlyAddedStrip(vm, openRecent, openPlayer)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Músicas, artistas, álbuns…") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                items(listOf("Todas", "Favoritas", "Artistas", "Álbuns")) { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label; group = null }, label = { Text(label) })
                }
                item { FilterChip(false, openPlaylists, { Text("Playlists") }) }
            }
        }
        if (home && query.isBlank() && tab == "Todas" && history.isNotEmpty()) {
            item {
                Text("Ouvidas recentemente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                    items(history.mapNotNull { h -> byId[h.songId] }.take(12), key = { it.id }) { song ->
                        Column(Modifier.width(132.dp).clickable(enabled = playback.connected) { vm.playback.play(listOf(song), 0); openPlayer() }) {
                            Artwork(song.artwork, Modifier.size(132.dp))
                            Text(song.title, maxLines = 1, modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall)
                            Text(song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (query.isNotBlank()) items(playlists.filter { it.name.searchKey().contains(query.searchKey()) }, key = { "playlist-${it.id}" }) { p ->
            ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("Playlist • abrir playlists") }, leadingContent = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) }, modifier = Modifier.clickable(onClick = openPlaylists))
        }
        if ((tab == "Artistas" || tab == "Álbuns") && group == null) {
            val groups = grouped
            items(groups.keys.toList(), key = { it }) { key ->
                ListItem(headlineContent = { Text(key) }, supportingContent = { Text("${groups[key]?.size} músicas") }, leadingContent = { Artwork(groups[key]?.firstOrNull()?.artwork, Modifier.size(52.dp)) }, modifier = Modifier.clickable { group = key })
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (group != null) Action(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar aos grupos", { group = null })
                    Text(group ?: "${filtered.size} músicas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Action(Icons.Rounded.Shuffle, "Reproduzir aleatoriamente", {
                        vm.playback.play(filtered, filtered.indices.random()); if (!playback.shuffle) vm.playback.shuffle(); openPlayer()
                    }, enabled = filtered.isNotEmpty() && playback.connected)
                }
            }
            itemsIndexed(filtered, key = { _, song -> song.id }) { index, song ->
                SongRow(song, song.id in favorites, { if (playback.connected) { vm.playback.play(filtered, index); openPlayer() } },
                    { vm.favorite(song.id) }, { add = song }, { vm.playback.enqueue(song) })
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 24.dp)) }
        else if (libraryError != null) item { LibraryReadError(libraryError!!, vm::refresh) }
        else if (filtered.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                Text(if (query.isBlank()) "Nenhuma música por aqui" else "Nenhuma música encontrada", modifier = Modifier.padding(12.dp))
                Text(if (query.isBlank()) "Adicione arquivos de áudio ao dispositivo e atualize a biblioteca." else "Tente outro título, artista ou álbum.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    add?.let { song -> PlaylistPicker(vm, song) { add = null } }
}

@Composable
fun PlaylistPicker(vm: MusicViewModel, song: Song, dismiss: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Adicionar à playlist") }, text = {
        LazyColumn {
            items(playlists) { p -> TextButton(onClick = { vm.add(p.id, song.id); dismiss() }) { Text(p.name) } }
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Nova playlist") }, singleLine = true)
                TextButton(onClick = { vm.create(name); name = "" }, enabled = name.isNotBlank()) { Text("Criar playlist") }
            }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Fechar") } })
}

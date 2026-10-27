package com.coelho.music.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelho.music.data.local.Playlist
import com.coelho.music.domain.model.Song
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.*

@Composable
fun PlaylistsScreen(vm: MusicViewModel, openPlayer: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playback by vm.playback.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var toPlaylist by remember { mutableStateOf<Song?>(null) }
    val playlist = playlists.find { it.id == selected }
    val context = LocalContext.current
    val cover = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && playlist != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); vm.cover(playlist, uri.toString()) }
            catch (_: SecurityException) { vm.error.value = "Não foi possível manter o acesso à capa selecionada." }
        }
    }
    val list = remember(entries, songs, selected) {
        val byId = songs.associateBy { it.id }
        entries.filter { it.playlistId == selected }.sortedBy { it.position }.mapNotNull { byId[it.songId] }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(vertical = 18.dp)) {
                if (selected != null) Action(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar às playlists", { selected = null })
                Text(playlist?.name ?: "Suas playlists", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Action(if (playlist == null) Icons.Rounded.Add else Icons.Rounded.Edit, if (playlist == null) "Criar playlist" else "Renomear", { name = playlist?.name ?: ""; editing = true })
            }
        }
        if (playlist == null) {
            if (playlists.isEmpty()) item { Text("Crie uma playlist e reúna suas músicas favoritas.", Modifier.padding(vertical = 32.dp)) }
            items(playlists, key = { it.id }) { p ->
                ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("${entries.count { it.playlistId == p.id }} músicas") },
                    leadingContent = { Artwork(p.cover, Modifier.size(62.dp)) }, trailingContent = { Icon(Icons.Rounded.ChevronRight, null) }, modifier = Modifier.clickable { selected = p.id })
            }
        } else {
            item {
                Artwork(playlist.cover ?: list.firstOrNull()?.artwork, Modifier.fillMaxWidth().height(200.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Action(Icons.Rounded.PlayArrow, "Reproduzir playlist", { vm.playback.play(list, 0); openPlayer() }, enabled = list.isNotEmpty() && playback.connected)
                    Action(Icons.Rounded.Add, "Adicionar músicas", { adding = true })
                    Action(Icons.Rounded.Image, "Escolher capa", { cover.launch(arrayOf("image/*")) })
                    Action(Icons.Rounded.Delete, "Excluir playlist", { deleting = true })
                }
                if (list.isEmpty()) Text("Esta playlist ainda não tem músicas disponíveis.", Modifier.padding(vertical = 24.dp))
            }
            itemsIndexed(list, key = { _, s -> s.id }) { i, song ->
                Column {
                    SongRow(song, song.id in favorites, { if (playback.connected) { vm.playback.play(list, i); openPlayer() } }, { vm.favorite(song.id) }, { toPlaylist = song }, { vm.playback.enqueue(song) })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Action(Icons.Rounded.ArrowUpward, "Mover para cima", { vm.move(playlist.id, song.id, -1) }, enabled = i > 0)
                        Action(Icons.Rounded.ArrowDownward, "Mover para baixo", { vm.move(playlist.id, song.id, 1) }, enabled = i < list.lastIndex)
                        Action(Icons.Rounded.RemoveCircleOutline, "Remover da playlist", { vm.remove(playlist.id, song.id) })
                    }
                }
            }
        }
    }
    toPlaylist?.let { song -> PlaylistPicker(vm, song) { toPlaylist = null } }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text(if (playlist == null) "Nova playlist" else "Renomear playlist") }, text = {
        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nome") })
    }, confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { if (playlist == null) vm.create(name) else vm.rename(playlist, name); editing = false }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } })
    if (deleting && playlist != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("Excluir ${playlist.name}?") }, text = { Text("Os arquivos de música permanecem no dispositivo.") }, confirmButton = {
        TextButton(onClick = { vm.delete(playlist); selected = null; deleting = false }) { Text("Excluir") }
    }, dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } })
    if (adding && playlist != null) AlertDialog(onDismissRequest = { adding = false }, title = { Text("Adicionar músicas") }, text = {
        LazyColumn { items(songs, key = { it.id }) { song ->
            val included = entries.any { it.playlistId == playlist.id && it.songId == song.id }
            ListItem(headlineContent = { Text(song.title) }, supportingContent = { Text(song.artist) }, trailingContent = {
                Checkbox(included, { checked -> if (checked) vm.add(playlist.id, song.id) else vm.remove(playlist.id, song.id) })
            })
        } }
    }, confirmButton = { TextButton(onClick = { adding = false }) { Text("Concluir") } })
}

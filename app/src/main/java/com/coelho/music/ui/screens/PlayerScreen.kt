package com.coelho.music.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.toBitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.coelho.music.domain.model.formatTime
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(vm: MusicViewModel, back: () -> Unit, equalizer: () -> Unit) {
    val state by vm.playback.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    var queue by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<String?>(null) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    var dominant by remember { mutableStateOf(Color(0xFF392256)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lyricPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            lyrics = withContext(Dispatchers.IO) { runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val buffer = CharArray(100_000)
                    var count = 0
                    while (count < buffer.size) {
                        val read = reader.read(buffer, count, buffer.size - count)
                        if (read < 0) break
                        count += read
                    }
                    String(buffer, 0, count)
                }
            }.getOrNull() } ?: "Não foi possível ler a letra."
        }
    }
    LaunchedEffect(state.current?.mediaId) {
        lyrics = null
        seeking = null
        val art = state.current?.mediaMetadata?.artworkUri
        val image = runCatching { SingletonImageLoader.get(context).execute(ImageRequest.Builder(context).data(art).allowHardware(false).size(96).build()).image }.getOrNull()
        dominant = withContext(Dispatchers.Default) {
            image?.toBitmap()?.let { Color(Palette.from(it).generate().getDominantColor(0xFF392256.toInt())) } ?: Color(0xFF392256)
        }
    }
    val item = state.current
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(dominant.copy(alpha = .6f), MaterialTheme.colorScheme.background)))
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Action(Icons.Rounded.KeyboardArrowDown, "Fechar player", back)
            Text("TOCANDO AGORA", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Action(Icons.Rounded.Equalizer, "Equalizador", equalizer)
        }
        if (item == null) { Text("Escolha uma música na biblioteca.", Modifier.padding(48.dp)); return@Column }
        Spacer(Modifier.height(24.dp))
        Artwork(item.mediaMetadata.artworkUri, Modifier.widthIn(max = 400.dp).fillMaxWidth().aspectRatio(1f))
        Row(Modifier.fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.mediaMetadata.title?.toString() ?: "Sem título", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(item.mediaMetadata.artist?.toString() ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            val id = item.mediaId.toLongOrNull()
            Action(if (id in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favoritar", { id?.let(vm::favorite) }, selected = id in favorites)
        }
        Slider(value = seeking ?: state.position.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
            onValueChange = { seeking = it }, onValueChangeFinished = { seeking?.let { vm.playback.seek(it.toLong()) }; seeking = null },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(), enabled = state.duration > 0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(seeking?.toLong() ?: state.position), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(state.duration), style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Action(Icons.Rounded.Shuffle, "Aleatório", vm.playback::shuffle, state.shuffle)
            Action(Icons.Rounded.SkipPrevious, "Anterior", vm.playback::previous)
            FilledIconButton(onClick = vm.playback::toggle, modifier = Modifier.size(76.dp), enabled = state.connected) {
                Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Reproduzir ou pausar", Modifier.size(40.dp))
            }
            Action(Icons.Rounded.SkipNext, "Próxima", vm.playback::next)
            Action(if (state.repeat == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repetição: ${listOf("desligada", "música", "fila")[state.repeat.coerceIn(0, 2)]}", vm.playback::repeat, state.repeat != 0)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.VolumeDown, "Volume")
            Slider(state.volume, vm.playback::volume, Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { queue = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null); Text("Fila") }
            TextButton(onClick = { lyricPicker.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Abrir letra") }
        }
        lyrics?.let { Text(it, Modifier.fillMaxWidth().padding(vertical = 24.dp)) }
    }
    if (queue) AlertDialog(onDismissRequest = { queue = false }, title = { Text("Fila de reprodução") }, text = {
        LazyColumn { itemsIndexed(state.queue) { index, media ->
            Column {
                TextButton(onClick = { vm.playback.jump(index); queue = false }) { Text("${if (index == state.index) "▶ " else ""}${media.mediaMetadata.title ?: "Sem título"}") }
                Row {
                    Action(Icons.Rounded.ArrowUpward, "Mover para cima", { vm.playback.move(index, index - 1) }, enabled = index > 0)
                    Action(Icons.Rounded.ArrowDownward, "Mover para baixo", { vm.playback.move(index, index + 1) }, enabled = index < state.queue.lastIndex)
                    Action(Icons.Rounded.Close, "Remover da fila", { vm.playback.remove(index) })
                }
            }
        } }
    }, confirmButton = { TextButton(onClick = { queue = false }) { Text("Concluir") } })
}

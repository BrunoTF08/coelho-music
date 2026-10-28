package com.coelho.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coelho.music.player.controller.PlaybackController
import com.coelho.music.player.controller.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(state: PlaybackState, controller: PlaybackController, dismiss: () -> Unit) {
    var all by remember { mutableStateOf(false) }
    val order = remember(state.queue, state.index, state.upNext, all) {
        if (all) state.queue.indices.toList() else (listOf(state.index) + state.upNext).filter { it in state.queue.indices }
    }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.8f).padding(horizontal = 20.dp)) {
            Text("Fila de reprodução", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!all, { all = false }, { Text("A seguir") })
                FilterChip(all, { all = true }, { Text("Fila completa") })
            }
            Text(if (state.shuffle) "Ordem aleatória do Media3" else "Selecione ou reorganize as músicas", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(order, key = { _, index -> index }, contentType = { _, _ -> "queue" }) { position, index ->
                    val media = state.queue[index]
                    Column(Modifier.animateItem()) {
                        ListItem(headlineContent = { Text(media.mediaMetadata.title?.toString() ?: "Sem título") },
                            supportingContent = { Text(if (index == state.index) "Tocando agora" else media.mediaMetadata.artist?.toString() ?: "") },
                            leadingContent = { Artwork(media.mediaMetadata.artworkUri, Modifier.size(44.dp)) },
                            modifier = Modifier.clickable { controller.jump(index); dismiss() })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Action(Icons.Rounded.ArrowUpward, "Mover para cima", { controller.move(index, order[position - 1]) }, enabled = position > 0)
                            Action(Icons.Rounded.ArrowDownward, "Mover para baixo", { controller.move(index, order[position + 1]) }, enabled = position < order.lastIndex)
                            Action(Icons.Rounded.Close, "Remover da fila", { controller.remove(index) })
                        }
                    }
                }
            }
            TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Concluir") }
        }
    }
}

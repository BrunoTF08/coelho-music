package com.coelho.music.player.controller

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.coelho.music.domain.model.Song
import com.coelho.music.player.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PlaybackState(val connected: Boolean = false, val current: MediaItem? = null, val playing: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val shuffle: Boolean = false, val repeat: Int = 0,
    val queue: List<MediaItem> = emptyList(), val index: Int = -1, val volume: Float = 1f, val error: String? = null)

class PlaybackController @Inject constructor(@ApplicationContext context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state = _state.asStateFlow()
    private var controller: MediaController? = null
    private val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    init {
        future.addListener({
            try {
                controller = future.get().also { player -> player.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) { update() }
                    override fun onPlayerError(error: PlaybackException) {
                        _state.value = _state.value.copy(error = "Não foi possível reproduzir este arquivo. Ele pode ter sido removido ou estar corrompido.")
                    }
                }) }
                update()
            } catch (_: Exception) { _state.value = _state.value.copy(error = "Falha ao conectar ao serviço de áudio. Reabra o aplicativo.") }
        }, ContextCompat.getMainExecutor(context))
        scope.launch { while (isActive) { delay(500); update() } }
    }
    private fun update() { controller?.let { p ->
        _state.value = _state.value.copy(connected = true, current = p.currentMediaItem, playing = p.isPlaying,
            position = p.currentPosition.coerceAtLeast(0), duration = p.duration.coerceAtLeast(0), shuffle = p.shuffleModeEnabled,
            repeat = p.repeatMode, queue = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }, index = p.currentMediaItemIndex, volume = p.volume)
    } }
    fun play(songs: List<Song>, index: Int) { if (songs.isEmpty()) return; controller?.apply {
        setMediaItems(songs.map { it.mediaItem() }, index.coerceIn(songs.indices), 0); prepare(); play()
    }; clearError() }
    fun toggle() { controller?.apply { if (isPlaying) pause() else { prepare(); play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun seek(position: Long) { controller?.seekTo(position) }
    fun shuffle() { controller?.apply { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.apply { repeatMode = (repeatMode + 1) % 3 } }
    fun volume(value: Float) { controller?.volume = value }
    fun jump(index: Int) { controller?.apply { seekToDefaultPosition(index); prepare(); play() } }
    fun enqueue(song: Song) { controller?.addMediaItem(song.mediaItem()) }
    fun remove(index: Int) { controller?.removeMediaItem(index) }
    fun move(index: Int, target: Int) { controller?.let { if (target in 0 until it.mediaItemCount) it.moveMediaItem(index, target) } }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun release() { scope.cancel(); MediaController.releaseFuture(future) }
}

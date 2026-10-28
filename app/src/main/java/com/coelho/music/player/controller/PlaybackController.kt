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

data class PlaybackProgress(val position: Long = 0, val duration: Long = 0)
data class PlaybackState(val connected: Boolean = false, val current: MediaItem? = null, val playing: Boolean = false,
    val shuffle: Boolean = false, val repeat: Int = 0, val queue: List<MediaItem> = emptyList(),
    val index: Int = -1, val previousIndex: Int = -1, val nextIndex: Int = -1,
    val upNext: List<Int> = emptyList(), val direction: Int = 1, val volume: Float = 1f, val error: String? = null) {
    val previous: MediaItem? get() = queue.getOrNull(previousIndex)
    val next: MediaItem? get() = queue.getOrNull(nextIndex)
}

class PlaybackController @Inject constructor(@ApplicationContext context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state = _state.asStateFlow()
    private val _progress = MutableStateFlow(PlaybackProgress())
    val progress = _progress.asStateFlow()
    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
    init {
        future.addListener({
            try {
                controller = future.get().also { player -> player.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        update(events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_MEDIA_METADATA_CHANGED))
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        _state.value = _state.value.copy(error = "Não foi possível reproduzir este arquivo. Ele pode ter sido removido ou estar corrompido.")
                    }
                }) }
                update(true)
            } catch (_: Exception) { _state.value = _state.value.copy(error = "Falha ao conectar ao serviço de áudio. Reabra o aplicativo.") }
        }, ContextCompat.getMainExecutor(context))
    }
    fun setUiActive(active: Boolean) {
        if (active && progressJob?.isActive != true) progressJob = scope.launch {
            updateProgress()
            while (isActive) { delay(500); if (controller?.isPlaying == true) updateProgress() }
        } else if (!active) { progressJob?.cancel(); progressJob = null }
    }
    private fun updateProgress() { controller?.let { _progress.value = PlaybackProgress(it.currentPosition.coerceAtLeast(0), it.duration.coerceAtLeast(0)) } }
    private fun update(rebuildQueue: Boolean = false) { controller?.let { p ->
        val old = _state.value
        val current = p.currentMediaItem
        val direction = if (old.current?.mediaId == current?.mediaId) old.direction else when (current?.mediaId) {
            old.next?.mediaId -> 1
            old.previous?.mediaId -> -1
            else -> if (p.currentMediaItemIndex < old.index) -1 else 1
        }
        val orderChanged = rebuildQueue || old.index != p.currentMediaItemIndex || old.shuffle != p.shuffleModeEnabled || old.repeat != p.repeatMode
        val upcoming = if (!orderChanged) old.upNext else buildList {
            val visited = mutableSetOf(p.currentMediaItemIndex)
            var cursor = p.currentMediaItemIndex
            val timeline = p.currentTimeline
            if (!timeline.isEmpty && cursor >= 0) {
                while (size < p.mediaItemCount) {
                    cursor = timeline.getNextWindowIndex(cursor, if (p.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF, p.shuffleModeEnabled)
                    if (cursor == C.INDEX_UNSET || !visited.add(cursor)) break
                    add(cursor)
                }
            }
        }
        _state.value = old.copy(connected = true, current = current, playing = p.isPlaying,
            shuffle = p.shuffleModeEnabled, repeat = p.repeatMode,
            queue = if (rebuildQueue) (0 until p.mediaItemCount).map { p.getMediaItemAt(it) } else old.queue,
            index = p.currentMediaItemIndex, previousIndex = p.previousMediaItemIndex, nextIndex = p.nextMediaItemIndex,
            upNext = upcoming, direction = direction, volume = p.volume)
        updateProgress()
    } }
    fun play(songs: List<Song>, index: Int) { if (songs.isEmpty()) return; controller?.apply {
        setMediaItems(songs.map { it.mediaItem() }, index.coerceIn(songs.indices), 0); prepare(); play()
    }; clearError() }
    fun toggle() { controller?.apply { if (isPlaying) pause() else { prepare(); play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun swipeNext(expectedId: String, targetId: String): Boolean = controller?.let {
        if (it.currentMediaItem?.mediaId != expectedId || !it.hasNextMediaItem() || it.getMediaItemAt(it.nextMediaItemIndex).mediaId != targetId) false
        else { it.seekToNextMediaItem(); true }
    } ?: false
    fun swipePrevious(expectedId: String, targetId: String): Boolean = controller?.let {
        if (it.currentMediaItem?.mediaId != expectedId || !it.hasPreviousMediaItem() || it.getMediaItemAt(it.previousMediaItemIndex).mediaId != targetId) false
        else { it.seekToPreviousMediaItem(); true }
    } ?: false
    fun seek(position: Long) { controller?.seekTo(position) }
    fun shuffle() { controller?.apply { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.apply { repeatMode = (repeatMode + 1) % 3 } }
    fun volume(value: Float) { controller?.volume = value }
    fun jump(index: Int) { controller?.apply { if (index !in 0 until mediaItemCount) return; seekToDefaultPosition(index); prepare(); play() } }
    fun enqueue(song: Song) { controller?.addMediaItem(song.mediaItem()) }
    fun remove(index: Int) { controller?.let { if (index in 0 until it.mediaItemCount) it.removeMediaItem(index) } }
    fun move(index: Int, target: Int) { controller?.let { if (index in 0 until it.mediaItemCount && target in 0 until it.mediaItemCount) it.moveMediaItem(index, target) } }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun release() { scope.cancel(); MediaController.releaseFuture(future) }
}

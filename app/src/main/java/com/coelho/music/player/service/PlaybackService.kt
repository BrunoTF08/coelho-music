package com.coelho.music.player.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.ContentDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.coelho.music.MainActivity
import com.coelho.music.domain.model.isLibraryAudioUri
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.coelho.music.data.local.History
import com.coelho.music.data.local.MusicDao
import com.coelho.music.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var dao: MusicDao
    @Inject lateinit var effects: AudioEffects
    @Inject lateinit var preferences: PreferencesRepository
    private var session: MediaSession? = null
    private var queueSaved = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate() {
        super.onCreate()
        // Playback only opens ContentResolver resources. No HTTP or file-path datasource.
        val player = ExoPlayer.Builder(this).setMediaSourceFactory(
            DefaultMediaSourceFactory(DataSource.Factory { ContentDataSource(this) })
        ).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        restore(player)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { effects.attach(audioSessionId) }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { id -> scope.launch { runCatching { dao.played(History(id, System.currentTimeMillis())) } } }
                save(player)
            }
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) || events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                    events.contains(Player.EVENT_TIMELINE_CHANGED) || events.contains(Player.EVENT_REPEAT_MODE_CHANGED) || events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) save(player, events.contains(Player.EVENT_TIMELINE_CHANGED))
            }
        })
        session = MediaSession.Builder(this, player).setCallback(object : MediaSession.Callback {
            override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>): ListenableFuture<List<MediaItem>> {
                if (controller.uid != android.os.Process.myUid() || mediaItems.any {
                        !isLibraryAudioUri(it.localConfiguration?.uri?.toString() ?: "")
                    }) return Futures.immediateFailedFuture(SecurityException("Only local MediaStore audio is allowed"))
                return Futures.immediateFuture(mediaItems)
            }
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                if (!controller.isTrusted) return MediaSession.ConnectionResult.reject()
                val defaults = super.onConnect(session, controller)
                if (controller.uid == android.os.Process.myUid()) return defaults
                // System/media controls may control playback, but cannot inject a new URI/queue.
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(defaults.availablePlayerCommands.buildUpon()
                        .remove(Player.COMMAND_CHANGE_MEDIA_ITEMS).remove(Player.COMMAND_SET_MEDIA_ITEM).build())
                    .build()
            }
        }).setSessionActivity(PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
        scope.launch { preferences.settings.collect { s -> effects.configure(s.equalizer, s.bands.split(',').mapNotNull { it.toIntOrNull() }) } }
        scope.launch { while (isActive) { delay(5000); if (player.isPlaying) save(player) } }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        if (controllerInfo.isTrusted) session else null
    private fun save(player: Player, rebuildQueue: Boolean = false) {
        // Queue serialization happens on timeline changes, never on every position checkpoint.
        if (!queueSaved || rebuildQueue) {
            val items = JSONArray()
            repeat(player.mediaItemCount) { i ->
                val item = player.getMediaItemAt(i)
                items.put(JSONObject().put("id", item.mediaId).put("uri", item.localConfiguration?.uri.toString())
                    .put("title", item.mediaMetadata.title?.toString()).put("artist", item.mediaMetadata.artist?.toString())
                    .put("art", item.mediaMetadata.artworkUri?.toString()))
            }
            getSharedPreferences("playback", MODE_PRIVATE).edit().putString("queue", items.toString()).apply()
            queueSaved = true
        }
        getSharedPreferences("playback_position", MODE_PRIVATE).edit()
            .putInt("index", player.currentMediaItemIndex.coerceAtLeast(0)).putLong("position", player.currentPosition)
            .putBoolean("shuffle", player.shuffleModeEnabled).putInt("repeat", player.repeatMode).apply()
    }
    private fun restore(player: ExoPlayer) {
        try {
            val saved = getSharedPreferences("playback", MODE_PRIVATE)
            val position = getSharedPreferences("playback_position", MODE_PRIVATE)
            val items = JSONArray(saved.getString("queue", "[]"))
            val queue = (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                require(isLibraryAudioUri(item.getString("uri"))) { "Invalid restored media URI" }
                MediaItem.Builder().setMediaId(item.getString("id")).setUri(item.getString("uri"))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(item.optString("title")).setArtist(item.optString("artist"))
                        .setArtworkUri(item.optString("art").takeUnless { it == "null" }?.let { android.net.Uri.parse(it) }).build()).build()
            }
            if (queue.isNotEmpty()) {
                player.setMediaItems(queue, position.getInt("index", 0).coerceIn(queue.indices), position.getLong("position", 0).coerceAtLeast(0))
                player.shuffleModeEnabled = position.getBoolean("shuffle", false)
                player.repeatMode = position.getInt("repeat", 0)
                queueSaved = true
            }
        } catch (_: Exception) { /* A stale queue never prevents service startup. */ }
    }
    override fun onDestroy() {
        session?.let { save(it.player); it.player.release(); it.release() }
        session = null
        effects.release()
        scope.cancel()
        super.onDestroy()
    }
}

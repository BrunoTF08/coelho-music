package com.coelho.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.coelho.music.domain.model.Song
import com.coelho.music.domain.model.reuseUnchanged
import com.coelho.music.domain.model.applyLibraryDelta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private val pendingLock = Any()
    private val pendingIds = mutableSetOf<Long>()
    private var needsFullRead = true
    private var cached = emptyMap<Long, Song>()
    private var snapshot = emptyList<Song>()
    val changes = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = changed(null)
            override fun onChange(selfChange: Boolean, uri: Uri?) = changed(uri)
            private fun changed(uri: Uri?) {
                synchronized(pendingLock) {
                    val id = uri?.takeIf { it.authority == "media" && it.pathSegments.takeLast(3).take(2) == listOf("audio", "media") }
                        ?.lastPathSegment?.toLongOrNull()
                    if (id == null) needsFullRead = true else pendingIds.add(id)
                }
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        // Close the race if a foreground scan started just before observer registration.
        synchronized(pendingLock) { needsFullRead = true }
        // Conflation is safe: the pending ID set retains all changes.
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.buffer(Channel.CONFLATED)

    suspend fun load(forceFull: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (full, ids) = synchronized(pendingLock) {
                val full = forceFull || needsFullRead || pendingIds.size > 200
                val ids = pendingIds.toSet()
                needsFullRead = false
                pendingIds.clear()
                full to ids
            }
            if (!full && ids.isEmpty()) return@withLock snapshot
            try {
                val incoming = query(if (full) null else ids)
                currentCoroutineContext().ensureActive()
                val reused = reuseUnchanged(cached, incoming) { it.id }
                val next = if (full) reused.associateBy { it.id } else applyLibraryDelta(cached, reused, ids) { it.id }
                val sorted = next.values.sortedWith(compareBy<Song> { it.sortTitle }.thenBy { it.id })
                if (sorted != snapshot) snapshot = sorted
                cached = next
                snapshot
            } catch (error: Exception) {
                synchronized(pendingLock) { needsFullRead = true }
                throw error
            }
        }
    }

    private suspend fun query(ids: Set<Long>?): List<Song> {
        val columns = arrayOf("_id", "title", "artist", "album", "album_id", "duration", "date_added")
        val selection = buildString {
            // Some downloaders leave IS_MUSIC unset. Include their indexed audio, but not alert sounds.
            append("(is_music != 0 OR (duration > 0 AND is_alarm = 0 AND is_notification = 0 AND is_ringtone = 0))")
            if (Build.VERSION.SDK_INT >= 29) append(" AND is_pending = 0")
            if (Build.VERSION.SDK_INT >= 30) append(" AND is_trashed = 0")
            if (ids != null) append(" AND _id IN (${ids.joinToString(",") { "?" }})")
        }
        val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns,
            selection, ids?.map { it.toString() }?.toTypedArray(), null) ?: throw IOException("MediaStore unavailable")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val id = it.getLong(0)
                    add(Song(id, it.getString(1) ?: "Sem título", it.getString(2)?.takeUnless { value -> value == "<unknown>" } ?: "Artista desconhecido",
                        it.getString(3) ?: "Sem álbum", it.getLong(4), it.getLong(5), it.getLong(6),
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)))
                }
            }
        }
    }
}

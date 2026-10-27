package com.coelho.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.coelho.music.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(@ApplicationContext private val context: Context) {
    val changes = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
    suspend fun load(): List<Song> = withContext(Dispatchers.IO) {
        val columns = arrayOf("_id", "title", "artist", "album", "album_id", "duration", "date_added")
        buildList {
            context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns,
                "is_music != 0", null, "title COLLATE NOCASE ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    add(Song(id, cursor.getString(1) ?: "Sem título", cursor.getString(2)?.takeUnless { it == "<unknown>" } ?: "Artista desconhecido",
                        cursor.getString(3) ?: "Sem álbum", cursor.getLong(4), cursor.getLong(5), cursor.getLong(6),
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)))
                }
            }
        }
    }
}

package com.coelho.music.domain.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.text.Normalizer
import androidx.compose.runtime.Immutable

@Immutable
data class Song(val id: Long, val title: String, val artist: String, val album: String,
    val albumId: Long, val duration: Long, val added: Long, val uri: Uri) {
    val artwork: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
    val searchText: String = "$title $artist $album".searchKey()
    val sortTitle: String = title.lowercase(java.util.Locale.ROOT)
    fun mediaItem(): MediaItem = MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
            .setAlbumTitle(album).setArtworkUri(artwork).build()).build()
}

private val searchMarks = "\\p{M}+".toRegex()
fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(searchMarks, "").lowercase(java.util.Locale.ROOT)

fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

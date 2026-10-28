package com.coelho.music.domain.model

import java.net.URI

/** Only MediaStore audio IDs are playable; no arbitrary provider, path, URL or executable. */
fun isLibraryAudioUri(value: String): Boolean {
    if (value.length > 512) return false
    return runCatching {
        val uri = URI(value)
        uri.scheme == "content" && uri.rawAuthority == "media" && uri.rawQuery == null &&
            uri.rawFragment == null && Regex("/[A-Za-z0-9_-]+/audio/media/[0-9]+").matches(uri.rawPath ?: "")
    }.getOrDefault(false)
}

package com.coelho.music.domain.model

import org.junit.Assert.*
import org.junit.Test

class LocalMediaPolicyTest {
    @Test fun acceptsOnlyMediaStoreAudioIds() {
        assertTrue(isLibraryAudioUri("content://media/external/audio/media/42"))
        assertTrue(isLibraryAudioUri("content://media/external_primary/audio/media/1"))
    }
    @Test fun rejectsRemoteFilesOtherProvidersAndTraversal() {
        listOf("https://example.com/song.mp3", "http://127.0.0.1/a", "file:///sdcard/a.mp3",
            "content://other.provider/private/key", "content://media/external/video/media/1",
            "content://media/external/audio/media/../1", "content://media/external/audio/media/%31",
            "content://media/external/audio/media/1?redirect=https://example.com",
            "content://media/external/audio/media/1#fragment", "content://media@evil/external/audio/media/1",
            "not a uri", "").forEach { assertFalse(it, isLibraryAudioUri(it)) }
    }
}

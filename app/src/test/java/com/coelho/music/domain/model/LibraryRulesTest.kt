package com.coelho.music.domain.model

import org.junit.Assert.*
import org.junit.Test

class LibraryRulesTest {
    @Test fun searchIgnoresAccentsAndCase() {
        assertTrue("Canção de João".searchKey().contains("CANCAO".searchKey()))
        assertTrue("Álbum".searchKey().contains("album"))
    }
    @Test fun timeHandlesUnknownAndLongTracks() {
        assertEquals("0:00", formatTime(-1))
        assertEquals("3:05", formatTime(185_000))
        assertEquals("120:01", formatTime(7_201_000))
    }
}

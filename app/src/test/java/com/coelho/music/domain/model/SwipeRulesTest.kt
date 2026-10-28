package com.coelho.music.domain.model

import org.junit.Assert.*
import org.junit.Test

class SwipeRulesTest {
    @Test fun leftAdvancesAndRightGoesBackOnlyAtThreshold() {
        assertEquals(SwipeAction.Next, horizontalSwipe(-100f, 400f, true, true))
        assertEquals(SwipeAction.Previous, horizontalSwipe(100f, 400f, true, true))
        assertEquals(SwipeAction.None, horizontalSwipe(-99f, 400f, true, true))
        assertEquals(SwipeAction.None, horizontalSwipe(99f, 400f, true, true))
    }
    @Test fun boundariesAndUnavailableNeighborsNeverSkip() {
        assertEquals(SwipeAction.None, horizontalSwipe(-400f, 400f, true, false))
        assertEquals(SwipeAction.None, horizontalSwipe(400f, 400f, false, true))
        assertEquals(SwipeAction.None, horizontalSwipe(100f, 0f, true, true))
        assertEquals(SwipeAction.None, horizontalSwipe(Float.NaN, 400f, true, true))
        assertEquals(SwipeAction.None, horizontalSwipe(0f, 400f, true, true))
    }
    @Test fun verticalSwipesOpenQueueOrMinimizeAndIncompleteGesturesDoNothing() {
        assertEquals(SwipeAction.Queue, verticalSwipe(-64f, 64f))
        assertEquals(SwipeAction.Minimize, verticalSwipe(64f, 64f))
        assertEquals(SwipeAction.None, verticalSwipe(63f, 64f))
        assertEquals(SwipeAction.None, verticalSwipe(-63f, 64f))
        assertEquals(SwipeAction.None, verticalSwipe(20f, -1f))
    }
}

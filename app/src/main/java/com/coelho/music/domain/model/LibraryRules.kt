package com.coelho.music.domain.model

/** DATE_ADDED is the date indexed by MediaStore, in seconds, not the file's original date. */
fun <T> recentlyAdded(items: List<T>, added: (T) -> Long, id: (T) -> Long): List<T> =
    items.sortedWith(compareByDescending(added).thenByDescending(id))

/** Reuse unchanged objects so an incremental library refresh does not invalidate every row. */
fun <T, K> reuseUnchanged(previous: Map<K, T>, incoming: List<T>, key: (T) -> K): List<T> =
    incoming.map { item -> previous[key(item)]?.takeIf { it == item } ?: item }

/** A missing changed ID is a deletion; unrelated rows retain their identities. */
fun <T, K> applyLibraryDelta(previous: Map<K, T>, incoming: List<T>, changedIds: Set<K>, key: (T) -> K): Map<K, T> =
    previous.toMutableMap().apply {
        changedIds.forEach { remove(it) }
        reuseUnchanged(previous, incoming, key).forEach { put(key(it), it) }
    }

enum class SwipeAction { None, Next, Previous, Queue, Minimize }

/** Velocity affects the spring, never authorizes an incomplete swipe. */
fun horizontalSwipe(distance: Float, width: Float, hasPrevious: Boolean, hasNext: Boolean): SwipeAction = when {
    width <= 0 || !distance.isFinite() -> SwipeAction.None
    distance <= -width * .25f && hasNext -> SwipeAction.Next
    distance >= width * .25f && hasPrevious -> SwipeAction.Previous
    else -> SwipeAction.None
}

fun verticalSwipe(distance: Float, threshold: Float): SwipeAction = when {
    threshold <= 0 || !distance.isFinite() -> SwipeAction.None
    distance <= -threshold -> SwipeAction.Queue
    distance >= threshold -> SwipeAction.Minimize
    else -> SwipeAction.None
}

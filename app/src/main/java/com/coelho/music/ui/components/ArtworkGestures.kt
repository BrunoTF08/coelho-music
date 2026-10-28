package com.coelho.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.coelho.music.domain.model.*
import com.coelho.music.player.controller.PlaybackState
import kotlinx.coroutines.*

@Composable
fun ArtworkGestures(state: PlaybackState, next: (String, String) -> Boolean, previous: (String, String) -> Boolean,
    openQueue: () -> Unit, minimize: () -> Unit, modifier: Modifier = Modifier) {
    val current = state.current ?: return
    val latest by rememberUpdatedState(state)
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var settling by remember { mutableStateOf<Job?>(null) }
    var pendingTarget by remember { mutableStateOf<String?>(null) }
    var committing by remember { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    LaunchedEffect(current.mediaId, width) {
        settling?.cancel()
        committing = false
        x = 0f; y = 0f
        if (current.mediaId != pendingTarget) pendingTarget = null
    }
    fun settle(target: Float, velocity: Float, action: SwipeAction, origin: String, targetId: String?) {
        settling?.cancel()
        committing = action == SwipeAction.Next || action == SwipeAction.Previous
        settling = scope.launch {
            Animatable(x).animateTo(target, spring(dampingRatio = .86f, stiffness = 360f), initialVelocity = velocity.coerceIn(-8000f, 8000f)) { x = value }
            if (latest.current?.mediaId == origin && targetId != null && committing) {
                pendingTarget = targetId
                val accepted = if (action == SwipeAction.Next) next(origin, targetId) else previous(origin, targetId)
                if (accepted && targetId != origin) {
                    // Media3 is authoritative. Wait for acknowledgement; never leave a predicted cover stuck.
                    delay(1200)
                    if (latest.current?.mediaId != origin) return@launch
                }
            }
            pendingTarget = null
            committing = false
            Animatable(x).animateTo(0f, spring(stiffness = 420f)) { x = value }
        }
    }
    Box(modifier.clipToBounds().onSizeChanged { width = it.width }
        .semantics {
            contentDescription = "Capa: deslize para os lados para trocar; para cima abre a fila; para baixo minimiza"
            customActions = listOf(
                CustomAccessibilityAction("Próxima música") { state.next?.let { next(current.mediaId, it.mediaId) } ?: false },
                CustomAccessibilityAction("Música anterior") { state.previous?.let { previous(current.mediaId, it.mediaId) } ?: false },
                CustomAccessibilityAction("Abrir fila") { openQueue(); true }, CustomAccessibilityAction("Minimizar") { minimize(); true })
        }
        .pointerInput(current.mediaId, width) {
            val velocity = VelocityTracker()
            var axis = 0
            detectDragGestures(
                onDragStart = {
                    if (!committing) {
                        settling?.cancel(); axis = 0; y = 0f
                        velocity.resetTracking()
                    }
                },
                onDrag = { change, delta ->
                    change.consume()
                    if (!committing) {
                        velocity.addPosition(change.uptimeMillis, change.position)
                        if (axis == 0) axis = if (kotlin.math.abs(delta.x) > kotlin.math.abs(delta.y)) 1 else 2
                        if (axis == 1) {
                            val candidate = x + delta.x
                            val blocked = (candidate < 0 && latest.next == null) || (candidate > 0 && latest.previous == null)
                            x = (x + delta.x * if (blocked) .18f else 1f).coerceIn(-width.toFloat(), width.toFloat())
                        } else { x = 0f; y += delta.y }
                    }
                },
                onDragCancel = { if (!committing) { y = 0f; settle(0f, 0f, SwipeAction.None, current.mediaId, null) } },
                onDragEnd = {
                    if (!committing) {
                        if (axis == 1) {
                            val action = horizontalSwipe(x, width.toFloat(), latest.previous != null, latest.next != null)
                            val target = when (action) { SwipeAction.Next -> -width.toFloat(); SwipeAction.Previous -> width.toFloat(); else -> 0f }
                            val targetId = when (action) { SwipeAction.Next -> latest.next?.mediaId; SwipeAction.Previous -> latest.previous?.mediaId; else -> null }
                            settle(target, velocity.calculateVelocity().x, action, current.mediaId, targetId)
                        } else {
                            val action = verticalSwipe(y, threshold)
                            y = 0f
                            when (action) { SwipeAction.Queue -> openQueue(); SwipeAction.Minimize -> minimize(); else -> Unit }
                        }
                    }
                })
        }) {
        AnimatedContent(targetState = current, contentKey = { it.mediaId }, label = "capas", transitionSpec = {
            if (targetState.mediaId == pendingTarget) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (slideInHorizontally(spring(stiffness = 400f)) { it * state.direction } + fadeIn()) togetherWith
                (slideOutHorizontally(spring(stiffness = 400f)) { -it * state.direction } + fadeOut())
        }) { media ->
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = x; translationY = y * .12f }) {
                Artwork(media.mediaMetadata.artworkUri, Modifier.fillMaxSize())
                state.next?.let { Artwork(it.mediaMetadata.artworkUri, Modifier.fillMaxSize().graphicsLayer { translationX = width.toFloat() }) }
                state.previous?.let { Artwork(it.mediaMetadata.artworkUri, Modifier.fillMaxSize().graphicsLayer { translationX = -width.toFloat() }) }
            }
        }
    }
}

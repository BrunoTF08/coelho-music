package com.coelho.music.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0B0B12)
val Surface = Color(0xFF171721)
val Purple = Color(0xFF9747FF)
val Muted = Color(0xFFA0A0AD)
@Composable
fun CoelhoTheme(dark: Boolean = true, accent: Int = 0, content: @Composable () -> Unit) {
    val primary = listOf(Purple, Color(0xFF00C9AE), Color(0xFFFF8A65))[accent.coerceIn(0, 2)]
    val colors = if (dark) darkColorScheme(primary = primary, secondary = Color(0xFFB388FF),
        background = Background, surface = Surface, onSurfaceVariant = Muted, onPrimary = Color.White)
    else lightColorScheme(primary = primary, secondary = Purple, background = Color(0xFFF5F3FA), surface = Color.White)
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

package com.coelho.music.player.service

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerState(val available: Boolean = false, val frequencies: List<Int> = emptyList(),
    val levels: List<Int> = emptyList(), val minimum: Int = -1500, val maximum: Int = 1500,
    val message: String = "Inicie uma música para acessar os efeitos.")

@Singleton
class AudioEffects @Inject constructor() {
    private var equalizer: Equalizer? = null
    private var session = 0
    private var desiredEnabled = false
    private var desiredLevels = emptyList<Int>()
    private val _equalizer = MutableStateFlow(EqualizerState())
    val state = _equalizer.asStateFlow()
    fun attach(id: Int) {
        if (id == session && equalizer != null) return
        release()
        session = id
        if (id == 0) return
        try {
            val effect = Equalizer(0, id)
            equalizer = effect
            _equalizer.value = EqualizerState(true, (0 until effect.numberOfBands).map { effect.getCenterFreq(it.toShort()) / 1000 },
                (0 until effect.numberOfBands).map { effect.getBandLevel(it.toShort()).toInt() },
                effect.bandLevelRange[0].toInt(), effect.bandLevelRange[1].toInt(), "Equalizador da sessão de reprodução")
            configure(desiredEnabled, desiredLevels)
        } catch (_: RuntimeException) { _equalizer.value = EqualizerState(message = "Equalizador indisponível neste dispositivo.") }
    }
    fun configure(enabled: Boolean, levels: List<Int>) {
        desiredEnabled = enabled
        desiredLevels = levels
        try {
            equalizer?.let { effect ->
                effect.enabled = enabled
                levels.take(effect.numberOfBands.toInt()).forEachIndexed { index, value ->
                    effect.setBandLevel(index.toShort(), value.coerceIn(_equalizer.value.minimum, _equalizer.value.maximum).toShort())
                }
                _equalizer.value = _equalizer.value.copy(levels = (0 until effect.numberOfBands).map { effect.getBandLevel(it.toShort()).toInt() })
            }
        } catch (_: RuntimeException) { _equalizer.value = _equalizer.value.copy(message = "Não foi possível aplicar o efeito.") }
    }
    fun release() { equalizer?.release(); equalizer = null; session = 0; _equalizer.value = EqualizerState() }
}

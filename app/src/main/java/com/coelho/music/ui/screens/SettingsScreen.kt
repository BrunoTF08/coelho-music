package com.coelho.music.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coelho.music.ui.MusicViewModel
import com.coelho.music.ui.components.Action

@Composable
fun SettingsScreen(vm: MusicViewModel, equalizer: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Do seu jeito.", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 18.dp))
        Text("APARÊNCIA", color = MaterialTheme.colorScheme.secondary)
        ListItem(headlineContent = { Text("Tema escuro") }, supportingContent = { Text("Uma nova atmosfera para sua música") }, trailingContent = {
            Switch(settings?.dark ?: true, { vm.boolean("dark", it) })
        })
        Text("Cor de destaque", Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Roxo", "Menta", "Coral").forEachIndexed { index, name ->
                FilterChip(settings?.accent == index, { vm.integer("accent", index) }, { Text(name) })
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 20.dp))
        Text("ÁUDIO", color = MaterialTheme.colorScheme.secondary)
        ListItem(headlineContent = { Text("Qualidade original") }, supportingContent = { Text("Decodificação local, sem conversão ou redução de qualidade. Formatos disponíveis dependem dos codecs do dispositivo.") })
        FilledTonalButton(onClick = equalizer, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Equalizer, null); Text("  Equalizador") }
        HorizontalDivider(Modifier.padding(vertical = 20.dp))
        Text("BIBLIOTECA", color = MaterialTheme.colorScheme.secondary)
        Text("Duração mínima dos arquivos", Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 30, 60).forEach { seconds -> FilterChip(settings?.minDuration == seconds, { vm.integer("minDuration", seconds) }, { Text(if (seconds == 0) "Todas" else "${seconds}s") }) }
        }
        OutlinedButton(onClick = vm::refresh, Modifier.fillMaxWidth()) { Text("Atualizar biblioteca") }
        Text("A biblioteca também acompanha alterações do MediaStore automaticamente.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(Modifier.padding(vertical = 20.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, Modifier.fillMaxWidth()) { Text("Configurações de notificações") }
        Text("Coelho Music 1.0.0", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
        Text("Sua música, do seu jeito.\n100% offline. Sem anúncios, conta ou coleta de dados.\nOs arquivos de áudio permanecem no dispositivo.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
fun EqualizerScreen(vm: MusicViewModel, back: () -> Unit) {
    val state by vm.effects.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var preset by remember { mutableStateOf("Personalizado") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row { Action(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", back); Text("Equalizador", style = MaterialTheme.typography.headlineSmall) }
        Text(state.message, Modifier.padding(vertical = 16.dp))
        ListItem(headlineContent = { Text("Ativar equalização") }, trailingContent = {
            Switch(settings?.equalizer ?: false, { vm.boolean("equalizer", it) }, enabled = state.available)
        })
        if (state.available) {
            Text("Perfis de áudio", style = MaterialTheme.typography.titleMedium)
            val presets = listOf("Rock", "Pop", "Eletrônica", "Graves", "Voz", "Personalizado")
            presets.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { name ->
                    FilterChip(preset == name, onClick = {
                        preset = name
                        if (name != "Personalizado") {
                            val levels = state.frequencies.map { hz ->
                                val db = when (name) {
                                    "Rock" -> if (hz < 250 || hz > 4000) 5 else -2
                                    "Pop" -> if (hz in 500..4000) 3 else 0
                                    "Eletrônica" -> if (hz < 150) 6 else if (hz > 4000) 4 else -1
                                    "Graves" -> if (hz < 250) 6 else 0
                                    "Voz" -> if (hz in 300..4000) 5 else -2
                                    else -> 0
                                }
                                (db * 100).coerceIn(state.minimum, state.maximum)
                            }
                            vm.equalize(levels); vm.boolean("equalizer", true)
                        }
                    }, label = { Text(name) })
                } }
            }
            state.frequencies.forEachIndexed { index, frequency ->
                var value by remember(index, state.levels) { mutableFloatStateOf(state.levels.getOrElse(index) { 0 }.toFloat()) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (frequency >= 1000) "${frequency / 1000f} kHz" else "$frequency Hz")
                    Text("${"%.1f".format(value / 100)} dB")
                }
                Slider(value, { value = it }, valueRange = state.minimum.toFloat()..state.maximum.toFloat(),
                    onValueChangeFinished = {
                        preset = "Personalizado"
                        vm.equalize(state.levels.toMutableList().also { it[index] = value.toInt() })
                    }, enabled = settings?.equalizer == true)
            }
            TextButton(onClick = { vm.equalize(state.frequencies.map { 0 }); preset = "Personalizado" }) { Text("Restaurar frequências") }
        } else Text("Os controles ficam disponíveis quando o dispositivo oferece um equalizador para a sessão ativa.")
    }
}

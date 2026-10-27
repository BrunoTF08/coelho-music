package com.coelho.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coelho.music.data.local.*
import com.coelho.music.data.repository.*
import com.coelho.music.domain.model.Song
import com.coelho.music.player.controller.PlaybackController
import com.coelho.music.player.service.AudioEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(val playback: PlaybackController, val effects: AudioEffects,
    private val library: LibraryRepository, private val dao: MusicDao, val preferences: PreferencesRepository) : ViewModel() {
    val songs = MutableStateFlow<List<Song>>(emptyList())
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val settings = preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val favorites = dao.favorites().map { list -> list.map { it.songId }.toSet() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val entries = dao.entries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = dao.history().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private var loadJob: Job? = null
    private var observerJob: Job? = null
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loading.value = true
            try {
                songs.value = library.load(); error.value = null
                if (observerJob == null) observerJob = viewModelScope.launch {
                    library.changes.catch { error.value = "Atualização automática indisponível. Use atualizar biblioteca." }
                        .collectLatest { delay(500); refresh() }
                }
            }
            catch (e: CancellationException) { throw e }
            catch (_: SecurityException) { songs.value = emptyList(); error.value = "Autorize o acesso às músicas para carregar a biblioteca." }
            catch (_: Exception) { error.value = "Não foi possível carregar a biblioteca. Tente atualizar." }
            finally { loading.value = false }
        }
    }
    private fun write(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { error.value = "Não foi possível salvar a alteração." }
    } }
    fun favorite(id: Long) = write { if (id in favorites.value) dao.unfavorite(id) else dao.favorite(Favorite(id)) }
    fun create(name: String) = write { name.trim().takeIf { it.isNotBlank() }?.let { dao.create(Playlist(name = it)) } }
    fun rename(playlist: Playlist, name: String) = write { if (name.isNotBlank()) dao.update(playlist.copy(name = name.trim())) }
    fun cover(playlist: Playlist, uri: String) = write { dao.update(playlist.copy(cover = uri)) }
    fun delete(playlist: Playlist) = write { dao.delete(playlist) }
    fun add(playlist: Long, song: Long) = write { dao.add(playlist, song) }
    fun remove(playlist: Long, song: Long) = write { dao.remove(playlist, song) }
    fun move(playlist: Long, song: Long, delta: Int) = write { dao.move(playlist, song, delta) }
    fun boolean(key: String, value: Boolean) = write { preferences.boolean(key, value) }
    fun integer(key: String, value: Int) = write { preferences.integer(key, value) }
    fun equalize(levels: List<Int>) = write { preferences.text("bands", levels.joinToString(",")) }
    override fun onCleared() { playback.release(); super.onCleared() }
}

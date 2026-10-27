package com.coelho.music.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settings by preferencesDataStore("settings")
data class Settings(val welcomed: Boolean = false, val dark: Boolean = true, val accent: Int = 0,
    val minDuration: Int = 0, val equalizer: Boolean = false, val bands: String = "")
@Singleton
class PreferencesRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.settings
    val settings = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Settings(it[booleanPreferencesKey("welcomed")] ?: false, it[booleanPreferencesKey("dark")] ?: true,
            it[intPreferencesKey("accent")] ?: 0, it[intPreferencesKey("minDuration")] ?: 0,
            it[booleanPreferencesKey("equalizer")] ?: false, it[stringPreferencesKey("bands")] ?: "")
    }
    suspend fun boolean(key: String, value: Boolean) { store.edit { it[booleanPreferencesKey(key)] = value } }
    suspend fun integer(key: String, value: Int) { store.edit { it[intPreferencesKey(key)] = value } }
    suspend fun text(key: String, value: String) { store.edit { it[stringPreferencesKey(key)] = value } }
}

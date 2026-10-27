package com.coelho.music.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class Favorite(@PrimaryKey val songId: Long)
@Entity(tableName = "playlists")
data class Playlist(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val cover: String? = null)
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"],
    foreignKeys = [ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)])
data class PlaylistSong(val playlistId: Long, val songId: Long, val position: Int)
@Entity(tableName = "history")
data class History(@PrimaryKey val songId: Long, val playedAt: Long)

@Dao
abstract class MusicDao {
    @Query("SELECT * FROM favorites") abstract fun favorites(): Flow<List<Favorite>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun favorite(value: Favorite)
    @Query("DELETE FROM favorites WHERE songId = :id") abstract suspend fun unfavorite(id: Long)
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE") abstract fun playlists(): Flow<List<Playlist>>
    @Insert abstract suspend fun create(playlist: Playlist): Long
    @Update abstract suspend fun update(playlist: Playlist)
    @Delete abstract suspend fun delete(playlist: Playlist)
    @Query("SELECT * FROM playlist_songs ORDER BY position") abstract fun entries(): Flow<List<PlaylistSong>>
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position") abstract suspend fun entriesFor(id: Long): List<PlaylistSong>
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertEntry(entry: PlaylistSong)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlist AND songId = :song") abstract suspend fun remove(playlist: Long, song: Long)
    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlist AND songId = :song") abstract suspend fun position(playlist: Long, song: Long, position: Int)
    @Transaction open suspend fun add(playlist: Long, song: Long) {
        insertEntry(PlaylistSong(playlist, song, (entriesFor(playlist).maxOfOrNull { it.position } ?: -1) + 1))
    }
    @Transaction open suspend fun move(playlist: Long, song: Long, delta: Int) {
        val entries = entriesFor(playlist).toMutableList()
        val index = entries.indexOfFirst { it.songId == song }
        val next = index + delta
        if (index < 0 || next !in entries.indices) return
        java.util.Collections.swap(entries, index, next)
        entries.forEachIndexed { i, entry -> position(playlist, entry.songId, i) }
    }
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50") abstract fun history(): Flow<List<History>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun played(history: History)
}

@Database(entities = [Favorite::class, Playlist::class, PlaylistSong::class, History::class], version = 1, exportSchema = true)
abstract class MusicDatabase : RoomDatabase() { abstract fun musicDao(): MusicDao }

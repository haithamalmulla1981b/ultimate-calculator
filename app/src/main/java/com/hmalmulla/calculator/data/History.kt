package com.hmalmulla.calculator.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long,
    val favorite: Boolean = false,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun all(): Flow<List<HistoryEntry>>

    @Query(
        "SELECT * FROM history WHERE (expression LIKE '%' || :q || '%' OR result LIKE '%' || :q || '%') " +
            "AND (:favoritesOnly = 0 OR favorite = 1) ORDER BY timestamp DESC"
    )
    fun search(q: String, favoritesOnly: Boolean): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :n")
    fun recent(n: Int): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(e: HistoryEntry): Long

    @Query("UPDATE history SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE favorite = 0")
    suspend fun deleteAllExceptFavorites()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun history(): HistoryDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "calculator.db").build()
    }
}

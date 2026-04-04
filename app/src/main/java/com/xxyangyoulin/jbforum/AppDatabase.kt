package com.xxyangyoulin.jbforum

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "link_favorites")
data class LinkFavoriteEntity(
    @PrimaryKey val id: String,
    val value: String,
    val type: String,
    val savedAt: Long,
    val sourceThreadTitle: String,
    val sourceThreadUrl: String
)

@Entity(tableName = "thread_history")
data class ThreadHistoryEntity(
    @PrimaryKey val url: String,
    val id: String,
    val title: String,
    val viewedAt: Long
)

@Dao
interface LinkFavoriteDao {
    @Query("SELECT * FROM link_favorites ORDER BY savedAt DESC")
    fun loadAll(): List<LinkFavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: LinkFavoriteEntity)

    @Query("DELETE FROM link_favorites WHERE id IN (:ids)")
    fun deleteByIds(ids: List<String>)
}

@Dao
interface ThreadHistoryDao {
    @Query("SELECT * FROM thread_history ORDER BY viewedAt DESC LIMIT :limit")
    fun loadTop(limit: Int): List<ThreadHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: ThreadHistoryEntity)

    @Query(
        "DELETE FROM thread_history WHERE url NOT IN (" +
            "SELECT url FROM thread_history ORDER BY viewedAt DESC LIMIT :limit" +
            ")"
    )
    fun trimTo(limit: Int)

    @Query("DELETE FROM thread_history")
    fun clearAll()
}

@Database(
    entities = [LinkFavoriteEntity::class, ThreadHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkFavoriteDao(): LinkFavoriteDao
    abstract fun threadHistoryDao(): ThreadHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jb_forum.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

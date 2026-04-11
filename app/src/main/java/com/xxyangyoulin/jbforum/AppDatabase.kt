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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Entity(tableName = "code_metadata")
data class CodeMetadataEntity(
    @PrimaryKey val code: String,
    val provider: String,
    val providerId: String,
    val title: String,
    val coverUrl: String,
    val backdropUrl: String,
    val thumbUrl: String,
    val releaseDate: String,
    val actorsCsv: String,
    val updatedAt: Long
)

@Dao
interface LinkFavoriteDao {
    @Query("SELECT * FROM link_favorites ORDER BY savedAt DESC")
    fun loadAll(): List<LinkFavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: LinkFavoriteEntity)

    @Query("DELETE FROM link_favorites WHERE id IN (:ids)")
    fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM link_favorites")
    fun clearAll()
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

@Dao
interface CodeMetadataDao {
    @Query("SELECT * FROM code_metadata WHERE code IN (:codes)")
    fun loadByCodes(codes: List<String>): List<CodeMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: CodeMetadataEntity)

    @Query("DELETE FROM code_metadata WHERE code IN (:codes)")
    fun deleteByCodes(codes: List<String>)

    @Query("DELETE FROM code_metadata")
    fun clearAll()
}

@Database(
    entities = [LinkFavoriteEntity::class, ThreadHistoryEntity::class, CodeMetadataEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkFavoriteDao(): LinkFavoriteDao
    abstract fun threadHistoryDao(): ThreadHistoryDao
    abstract fun codeMetadataDao(): CodeMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `code_metadata` (
                        `code` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `providerId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `coverUrl` TEXT NOT NULL,
                        `backdropUrl` TEXT NOT NULL,
                        `thumbUrl` TEXT NOT NULL,
                        `releaseDate` TEXT NOT NULL,
                        `actorsCsv` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `code_metadata` ADD COLUMN `backdropUrl` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jb_forum.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}

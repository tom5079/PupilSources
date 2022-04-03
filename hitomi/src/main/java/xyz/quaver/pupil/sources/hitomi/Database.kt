package xyz.quaver.pupil.sources.hitomi

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class Favorite(
    @PrimaryKey val item: String,
    val type: Type,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Type {
        GALLERY, TAG
    }
}

@Dao
interface FavoritesDao {
    @Query("SELECT item FROM favorite")
    fun getAll(): Flow<List<String>>

    @Query("SELECT item FROM favorite WHERE type == 'GALLERY'")
    fun getGalleries(): Flow<List<String>>

    @Query("SELECT item FROM favorite WHERE type == 'TAG'")
    fun getTags(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT * FROM favorite WHERE item = :item)")
    fun contains(item: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)
    suspend fun insertGallery(id: String) = insert(Favorite(id, Favorite.Type.GALLERY))
    suspend fun insertTag(tag: String) = insert(Favorite(tag, Favorite.Type.TAG))

    @Query("DELETE FROM favorite WHERE item = :item")
    suspend fun delete(item: String)
}

val MIGRATION_1_2 = object: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        with (database) {
            execSQL("""
                CREATE TABLE new_Favorite (
                    item        TEXT    PRIMARY KEY NOT NULL,
                    type        TEXT                NOT NULL,
                    timestamp   INTEGER             NOT NULL
                )
            """.trimIndent())
            query("SELECT item, timestamp FROM Favorite").use { cursor ->
                while (cursor.moveToNext()) {
                    val item = cursor.getString(0)
                    val timestamp = cursor.getInt(1)

                    insert(
                        "new_Favorite",
                        SQLiteDatabase.CONFLICT_FAIL,
                        ContentValues().apply {
                            put("item", item)
                            put("timestamp", timestamp)
                            put("type", if (item.all { it.isDigit() }) "GALLERY" else "TAG")
                        }
                    )
                }
            }

            execSQL("DROP TABLE Favorite")
            execSQL("ALTER TABLE new_Favorite RENAME TO Favorite")
        }
    }
}

@Database(entities = [Favorite::class], version = 2)
abstract class HitomiDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}
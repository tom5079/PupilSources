package xyz.quaver.pupil.sources.hitomi

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class Favorite(
    @PrimaryKey val item: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite")
    fun getAll(): Flow<List<Favorite>>

    @Query("SELECT EXISTS(SELECT * FROM favorite WHERE item = :item)")
    fun contains(item: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)
    suspend fun insert(item: String) = insert(Favorite(item))

    @Delete
    suspend fun delete(favorite: Favorite)
    suspend fun delete(item: String) = delete(Favorite(item))
}

@Database(entities = [Favorite::class], version = 1, exportSchema = false)
abstract class HitomiDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
}
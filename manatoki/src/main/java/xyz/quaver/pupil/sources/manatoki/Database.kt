/*
 *     Pupil, Hitomi.la viewer for Android
 *     Copyright (C) 2021 tom5079
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.quaver.pupil.sources.manatoki

import androidx.room.*
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.coroutines.flow.Flow

@Entity
data class Favorite(
    @PrimaryKey val itemID: String
)

@Entity
data class Bookmark(
    @PrimaryKey val itemID: String,
    val page: Int
)

@Entity
data class History(
    @PrimaryKey val itemID: String,
    val parent: String,
    val page: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExtensionMap(
    val extensions: Map<String, String?> = emptyMap()
)

@Entity(tableName = "cookie_entry", primaryKeys = ["url", "name"])
data class CookieEntry(
    val url: String,
    val name: String,
    val value: String,
    val encoding: CookieEncoding = CookieEncoding.URI_ENCODING,
    val maxAge: Int = 0,
    val expires: GMTDate? = null,
    val domain: String? = null,
    val path: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val extensions: ExtensionMap = ExtensionMap()
)

class Converters {
    @TypeConverter
    fun dateFromTimestamp(timestamp: Long?): GMTDate? {
        return timestamp?.let { GMTDate(timestamp) }
    }

    @TypeConverter
    fun dateToTimestamp(date: GMTDate?): Long? {
        return date?.timestamp
    }

    @TypeConverter
    fun mapFromString(str: String?): ExtensionMap? {
        return str?.let { ExtensionMap(buildMap {
            if (str.isEmpty()) return@buildMap

            str.split(';').forEach { extension ->
                val key = extension.takeWhile { it != '=' }

                var value: String? = extension.takeLastWhile { it != '=' }
                if (value?.isEmpty() == true) value = null

                put(key, value)
            }
        }) }
    }

    @TypeConverter
    fun stringFromMap(map: ExtensionMap?): String? {
        return map?.extensions?.entries?.joinToString(";") {
            "${it.key}=${it.value.orEmpty()}"
        }
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT EXISTS(SELECT * FROM favorite WHERE itemID = :itemID)")
    fun contains(itemID: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite)
    suspend fun insert(itemID: String) = insert(Favorite(itemID))

    @Delete
    suspend fun delete(favorite: Favorite)
    suspend fun delete(itemID: String) = delete(Favorite(itemID))
}

@Dao
interface BookmarkDao {

}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History)
    suspend fun insert(itemID: String, parent: String, page: Int) = insert(History(itemID, parent, page))

    @Query("DELETE FROM history WHERE itemID = :itemID")
    suspend fun delete(itemID: String)

    @Query("SELECT parent FROM (SELECT parent, max(timestamp) as t FROM history GROUP BY parent) ORDER BY t DESC")
    fun getRecentManga(): Flow<List<String>>

    @Query("SELECT itemID FROM history WHERE parent = :parent ORDER BY timestamp DESC")
    suspend fun getAll(parent: String): List<String>
}

@Dao
interface CookieDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addCookie(cookieEntry: CookieEntry)

    @Query("""
        SELECT * FROM cookie_entry
        WHERE (domain = :host OR (NOT :hostIsIp AND :host LIKE '%.' || domain))
        AND (path = '/' OR path = :requestPath OR path || '%' LIKE :requestPath)
        AND NOT (secure AND NOT :isSecure)
    """)
    suspend fun get(
        host: String,
        hostIsIp: Boolean,
        requestPath: String,
        isSecure: Boolean
    ): List<CookieEntry>

    @Query("SELECT min(expires) FROM cookie_entry")
    suspend fun oldestCookie(): Long?

    @Query("DELETE FROM cookie_entry WHERE expires < :timestamp")
    suspend fun cleanup(timestamp: Long)

    @Query("""
        DELETE FROM cookie_entry
        WHERE name = :name
        AND (domain = :host OR (NOT :hostIsIp AND :host LIKE '%.' || domain))
        AND (path = '/' OR path = :requestPath OR path || '%' LIKE :requestPath)
        AND NOT (secure AND NOT :isSecure)
    """)
    suspend fun removeDuplicates(
        name: String,
        host: String,
        hostIsIp: Boolean,
        requestPath: String,
        isSecure: Boolean
    )
}

@Database(entities = [Favorite::class, Bookmark::class, History::class, CookieEntry::class], version = 1)
@TypeConverters(Converters::class)
abstract class ManatokiDatabase: RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun cookieDao(): CookieDao
}
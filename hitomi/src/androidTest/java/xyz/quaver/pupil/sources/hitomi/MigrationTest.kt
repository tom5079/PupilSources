package xyz.quaver.pupil.sources.hitomi

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = Hitomi.packageName

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HitomiDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        val items = mapOf(
            "343242" to Favorite.Type.GALLERY,
            "1343" to Favorite.Type.GALLERY,
            "35463" to Favorite.Type.GALLERY,
            "female:ekjr" to Favorite.Type.TAG,
            "eti" to Favorite.Type.TAG,
            "ak34" to Favorite.Type.TAG
        )

        helper.createDatabase(TEST_DB, 1).apply {
            items.keys.forEach { item ->
                insert("favorite", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("item", item)
                    put("timestamp", 0)
                })
            }
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT item, type FROM favorite").use {
            assertEquals(items.size, it.count)
            while (it.moveToNext()) {
                val item = it.getString(0)
                val type = it.getString(1)

                assertEquals(items[item]?.name, type)
            }
        }
    }
}
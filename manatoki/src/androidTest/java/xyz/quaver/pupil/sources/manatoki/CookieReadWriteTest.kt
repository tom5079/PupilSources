package xyz.quaver.pupil.sources.manatoki

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiCookiesStorage

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CookieReadWriteTest {
    private lateinit var database: ManatokiDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, ManatokiDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun write_and_read_cookie() = runTest {
        val url = Url("https://a/")

        val expectedCookies = listOf(
            Cookie("test", "test")
        )

        val cookiesStorage = ManatokiCookiesStorage(database)

        expectedCookies.forEach { cookie ->
            cookiesStorage.addCookie(url, cookie)
        }

        val cookies = cookiesStorage.get(url)

        assertEquals(expectedCookies, cookies)
    }
}
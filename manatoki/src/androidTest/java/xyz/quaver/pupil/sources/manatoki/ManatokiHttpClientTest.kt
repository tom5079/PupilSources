package xyz.quaver.pupil.sources.manatoki

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import xyz.quaver.pupil.sources.manatoki.networking.*

@OptIn(ExperimentalCoroutinesApi::class)
class ManatokiHttpClientTest {

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
    fun main() = runTest {
        val expectedMainData: MainData = Json.decodeFromString(
            javaClass.getResource("/expected_main.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/index.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine, database)

        val result = client.main()!!

        assertEquals(expectedMainData, result)
    }

    @Test
    fun mangaListing() = runTest {
        val expectedMangaListing: MangaListing = Json.decodeFromString(
            javaClass.getResource("/expected_manga_listing.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/manga_listing.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine, database)

        val result = client.getItem(expectedMangaListing.itemID)

        assertEquals(expectedMangaListing, result)
    }

    @Test
    fun readerInfo() = runTest {
        val expectedReaderInfo: ReaderInfo = Json.decodeFromString(
            javaClass.getResource("/expected_reader_info.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/reader_info.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine, database)

        val result = client.getItem(expectedReaderInfo.itemID)

        assertEquals(expectedReaderInfo, result)
    }

    @Test
    fun recent() = runTest {
        val expectedRecent: List<MangaThumbnail> = Json.decodeFromString(
            javaClass.getResource("/expected_recent.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/recent.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine, database)

        val recent = client.recent(0)

        assertEquals(expectedRecent, recent)
    }

    @Test
    fun search() = runTest {
        val expectedSearch: SearchResult = Json.decodeFromString(
            javaClass.getResource("/expected_search.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/search.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine, database)

        val result = client.search(SearchParameters())

        assertEquals(expectedSearch, result)
    }

}
package xyz.quaver.pupil.sources.manatoki.networking

import io.ktor.client.engine.mock.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManatokiHttpClientTest {

    @Test
    fun main() = runTest {
        val expectedMainData: MainData = Json.decodeFromString(
            javaClass.getResource("/expected_main.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/index.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine)

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

        val client = ManatokiHttpClient(mockEngine)

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

        val client = ManatokiHttpClient(mockEngine)

        val result = client.getItem(expectedReaderInfo.itemID)

        assertEquals(expectedReaderInfo, result)
    }

    @Test
    fun recent() = runTest {
        val expectedRecent: List<Thumbnail> = Json.decodeFromString(
            javaClass.getResource("/expected_recent.json")!!.readText()
        )

        val mockEngine = MockEngine { _ ->
            respond(javaClass.getResource("/recent.html")!!.readText())
        }

        val client = ManatokiHttpClient(mockEngine)

        val recent = client.recent(0)

        assertEquals(expectedRecent, recent)
    }

}
package xyz.quaver.pupil.sources.hitomi

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.get
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.quaver.pupil.sources.hitomi.lib.*
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private val client = HttpClient(OkHttp) {
//        install(JsonFeature) {
//            serializer = KotlinxSerializer()
//        }
//        install(HttpTimeout) {
//            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//            connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
//        }
//
//        install(HitomiPlugin)
//        BrowserUserAgent()
    }

    @Test
    fun testGetGalleryForQuery() {
        val testCases = listOf(
            "language:japanese",
            "language:english",
            "female:crotch_tattoo"
        )

        val expectedSize = listOf(
            314174,
            93452,
            5334
        )

        runBlocking {
            testCases.zip(expectedSize).forEach { (query, size) ->
                val result = client.getGalleryIDsForQuery(query)

                println("${result.capacity()} results for $query")

                assertTrue(result.capacity() >= size)
            }
        }
    }

    @Test
    fun searchTest() {
        runBlocking {
            val testCases = listOf(
                "language:japanese",
                "language:english",
                "female:crotch_tattoo",
                "language:japanese -male:yaoi",
                "type:artistcg female:mind_control"
            )

            val expectedSize = listOf(
                314174,
                93452,
                5334,
                284307,
                4486
            )

            testCases.zip(expectedSize).forEach { (query, size) ->
                val result = client.doSearch(query)

                println("${result.limit()} results for $query")

                assertTrue(result.limit() >= size)
            }
        }
    }

    @Test
    fun imageTest() {
        val galleryID = 2175966

        runBlocking {
            val galleryInfo = client.getGalleryInfo(galleryID)

            val images = galleryInfo.files.map { client.imageUrlFromImage(it) }

            println(images)
        }
    }

    @Test
    fun test_getGalleryIDsFromNozomi() {
        runBlocking {
            val result = client.getGalleryIDsFromNozomi(null, "index", "all")

            println("${result.capacity()} results")
        }
    }
}
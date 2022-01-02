package xyz.quaver.pupil.sources.hitomi

import android.util.Log
import android.webkit.WebView
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import xyz.quaver.pupil.sources.hitomi.lib.doSearch
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryIDsForQuery

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest: DIAware {

    override val di by DI.lazy {
        bindSingleton {
            HttpClient(OkHttp) {
                engine {
                    config {
                        protocols(listOf(Protocol.HTTP_1_1))
                    }
                }
                install(JsonFeature) {
                    serializer = KotlinxSerializer()
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                    socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }

                BrowserUserAgent()
            }
        }
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
                val result = getGalleryIDsForQuery(query)

                println("${result.size} results for $query")

                assertTrue(result.size >= size)
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
                val result = doSearch(query)

                println("${result.size} results for $query")

                assertTrue(result.size >= size)
            }
        }
    }
}
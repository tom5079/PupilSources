package xyz.quaver.pupil.sources.hitomi

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.RateLimiter
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.imageUrlFromImage

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    lateinit var client: HttpClient

    @Before
    fun init() {
        client = HttpClient() {
            BrowserUserAgent()
        }
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("xyz.quaver.pupil.sources.hitomi", appContext.packageName)
    }

    @Test
    fun testImages() {
        val ratelimiter = RateLimiter.create(2.0)

        val testCases = listOf(
            2099617,
            2100172
        )

        runBlocking {
            testCases.forEach { galleryID ->
                val galleryInfo = getGalleryInfo(client, galleryID)

                val images = galleryInfo.files.flatMap {
                    listOf(
                        imageUrlFromImage(client, galleryID, it, true),
                        imageUrlFromImage(client, galleryID, it, false)
                    )
                }

                images.forEachIndexed { i, it ->
                    ratelimiter.acquire()
                    println("Requesting $i/${images.size}")
                    val response: HttpResponse = client.get(it) {
                        header("Referer", "https://hitomi.la/")
                    }

                    assertEquals(200, response.status.value)
                    println("Passed $i/${images.size}")
                }
            }
        }
    }
}
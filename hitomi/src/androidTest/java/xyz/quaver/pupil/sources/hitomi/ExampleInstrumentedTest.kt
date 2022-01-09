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
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.imageUrlFromImage

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest: DIAware {

    override val di by DI.lazy {
        bindSingleton {
            HttpClient() {
                BrowserUserAgent()
            }
        }
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("xyz.quaver.pupil.sources.hitomi", appContext.packageName)
    }
}
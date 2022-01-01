package xyz.quaver.pupil.sources.hitomi.lib

import app.cash.zipline.QuickJs
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface gg {
    fun m(g: Int): Int
    val b: String
    fun s(h: String): String

    companion object {
        private val mutex = Mutex()
        private var instance: gg? = null

        private suspend fun getGG(client: HttpClient): gg = withContext(Dispatchers.IO) {
            val engine = QuickJs.create()

            engine.evaluate(client.get("https://ltn.hitomi.la/gg.js"))

            object: gg {
                override fun m(g: Int): Int =
                    engine.evaluate("gg.m($g)") as Int

                override val b: String
                    get() = engine.evaluate("gg.b") as String

                override fun s(h: String): String =
                    engine.evaluate("gg.s('$h')") as String
            }
        }

        suspend fun getInstance(client: HttpClient) =
            instance ?: mutex.withLock {
                instance ?: getGG(client).also { instance = it }
            }
    }
}
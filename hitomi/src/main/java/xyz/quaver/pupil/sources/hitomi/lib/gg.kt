package xyz.quaver.pupil.sources.hitomi.lib

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
            val ggjs: String = client.get("https://ltn.hitomi.la/gg.js")

            object: gg {
                private val oMap = Regex("""case (\d+): o = (\d+); break;""").findAll(ggjs).map { m ->
                    m.groupValues.let { it[1].toInt() to it[2].toInt() }
                }.toMap()

                override fun m(g: Int): Int {
                    return oMap[g] ?: 0
                }

                override val b = Regex("b: '(.+)'").find(ggjs)?.groupValues?.get(1) ?: ""

                override fun s(h: String): String {
                    val m = Regex("(..)(.)$").find(h)!!
                    return m.groupValues.let { (it[2]+it[1]).toInt(16).toString(10) }
                }
            }
        }

        suspend fun getInstance(client: HttpClient) =
            instance ?: mutex.withLock {
                instance ?: getGG(client).also { instance = it }
            }
    }
}
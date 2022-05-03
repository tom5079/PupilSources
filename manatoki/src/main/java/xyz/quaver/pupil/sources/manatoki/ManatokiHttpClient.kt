package xyz.quaver.pupil.sources.manatoki

import android.os.Parcelable
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class Thumbnail(
    val itemID: String,
    val title: String,
    val thumbnail: String
)

@Serializable
data class TopWeekly(
    val itemID: String,
    val title: String,
    val count: String
)

data class MainData(
    val recentUpload: List<Thumbnail>,
    val mangaList: List<Thumbnail>,
    val topWeekly: List<TopWeekly>
)

class ManatokiHttpClient(engine: HttpClientEngine) {
    private val httpClient = HttpClient(engine)

    private val baseUrl = "https://manatoki.net"

    suspend fun main(): MainData? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = Jsoup.parse(httpClient.get("https://manatoki.net/").bodyAsText())

            val misoPostGallery = doc.select(".miso-post-gallery")

            val recentUpload = misoPostGallery[0]
                .select(".post-image > a")
                .map { entry ->
                    val itemID = entry.attr("href").takeLastWhile { it != '/' }
                    val title = entry.selectFirst("div.in-subject > b")!!.ownText()
                    val thumbnail = entry.selectFirst("img")!!.attr("src")

                    Thumbnail(itemID, title, thumbnail)
                }

            val mangaList = misoPostGallery[1]
                .select(".post-image > a")
                .map { entry ->
                    val itemID = entry.attr("href").takeLastWhile { it != '/' }
                    val title = entry.selectFirst("div.in-subject")!!.ownText()
                    val thumbnail = entry.selectFirst("img")!!.attr("src")

                    Thumbnail(itemID, title, thumbnail)
                }

            val misoPostList = doc.select(".miso-post-list")

            val topWeekly = misoPostList[4]
                .select(".post-row > a")
                .map { entry ->
                    val itemID = entry.attr("href").takeLastWhile { it != '/' }
                    val title = entry.ownText()
                    val count = entry.selectFirst("span.count")!!.text()

                    TopWeekly(itemID, title, count)
                }

            MainData(recentUpload, mangaList, topWeekly)
        }.getOrNull()
    }
}
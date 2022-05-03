package xyz.quaver.pupil.sources.manatoki

import android.os.Parcelable
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
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

@Parcelize
@Serializable
data class MangaListingEntry(
    val itemID: String,
    val episode: Int,
    val title: String,
    val starRating: Float,
    val date: String,
    val viewCount: Int,
    val thumbsUpCount: Int
): Parcelable

@Parcelize
@Serializable
data class MangaListing(
    val itemID: String,
    val title: String,
    val thumbnail: String,
    val author: String,
    val tags: List<String>,
    val type: String,
    val thumbsUpCount: Int,
    val entries: List<MangaListingEntry>
): Parcelable

@Parcelize
@Serializable
data class ReaderInfo(
    val itemID: String,
    val title: String,
    val urls: List<String>,
    val listingItemID: String,
    val prevItemID: String,
    val nextItemID: String
): Parcelable

class ManatokiHttpClient(engine: HttpClientEngine) {
    private val httpClient = HttpClient(engine)

    private val baseUrl = "https://manatoki.net"

    /**
     * Fetch main menu.
     * Returns null when exception occurs.
     */
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
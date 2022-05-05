package xyz.quaver.pupil.sources.manatoki.networking

import android.os.Parcelable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Evaluator
import java.nio.ByteBuffer

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
    val prevItemID: String?,
    val nextItemID: String?
): Parcelable

class ManatokiHttpClient(engine: HttpClientEngine) {

    val httpClient = HttpClient(engine) {
        install(ManatokiCaptcha)
        install(HttpCookies)
        install(HttpCache)
    }

    private val baseUrl = "https://manatoki130.net"
    private val captchaUrl = "$baseUrl/plugin/kcaptcha"

    suspend fun getCookie() = httpClient.cookies(baseUrl)

    /**
     * Fetch main menu.
     * Returns null when exception occurs.
     */
    suspend fun main(): MainData? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = Jsoup.parse(httpClient.get("https://manatoki.net/").bodyAsText())

            val misoPostGallery = doc.getElementsByClass("miso-post-gallery")

            val recentUpload = misoPostGallery[0]
                .select(".post-image > a")
                .map { entry ->
                    val itemID = entry
                        .attr("href")
                        .takeLastWhile { it != '/' }
                    val title = entry
                        .selectFirst("div.in-subject > b")!!
                        .ownText()
                    val thumbnail = entry
                        .selectFirst(Evaluator.Tag("img"))!!
                        .attr("src")

                    Thumbnail(itemID, title, thumbnail)
                }

            val mangaList = misoPostGallery[1]
                .select(".post-image > a")
                .map { entry ->
                    val itemID = entry.attr("href").takeLastWhile { it != '/' }
                    val title = entry.selectFirst("div.in-subject")!!.ownText()
                    val thumbnail = entry.selectFirst(Evaluator.Tag("img"))!!.attr("src")

                    Thumbnail(itemID, title, thumbnail)
                }

            val misoPostList = doc.getElementsByClass("miso-post-list")

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

    private fun parseReaderInfo(itemID: String, doc: Document): ReaderInfo {
        val htmlData = buildString {
            doc.selectFirst(".view-padding > script")!!
                .data()
                .lineSequence()
                .forEach { line ->
                    if (!line.startsWith("html_data")) return@forEach

                    line.drop(12).dropLast(2).split('.').forEach {
                        if (it.isNotBlank()) appendCodePoint(it.toInt(16))
                    }
                }
        }
        val urls = Jsoup.parse(htmlData)
            .select("img[^data-]:not([style])")
            .map { url ->
                url.attributes()
                    .first { attr -> attr.key.startsWith("data-") }
                    .value
            }

        val title = doc
            .selectFirst(Evaluator.Class("toon-title"))!!
            .ownText()

        val listingItemID = doc
            .selectFirst("a:contains(전체목록)")!!
            .attr("href")
            .takeLastWhile { it != '/' }

        val prevItemID = doc
            .getElementById("goPrevBtn")!!
            .attr("href")
            .let { href ->
                when {
                    href == "#prev" -> null
                    href.contains('?') -> href.dropLastWhile { it != '?' }.drop(1)
                    else -> href
                }?.takeLastWhile { it != '/' }
            }

        val nextItemID = doc
            .getElementById("goNextBtn")!!
            .attr("href")
            .let { href ->
                when {
                    href == "#next" -> null
                    href.contains('?') -> href.dropLastWhile { it != '?' }.drop(1)
                    else -> href
                }?.takeLastWhile { it != '/' }
            }

        return ReaderInfo(
            itemID,
            title,
            urls,
            listingItemID,
            prevItemID,
            nextItemID
        )
    }

    private fun parseMangaListing(itemID: String, doc: Document): MangaListing {
        val titleBlock = doc.selectFirst("div.view-title")!!

        val title = titleBlock
            .selectFirst("div.view-content:not([itemprop])")!!
            .text()

        val author = titleBlock
            .selectFirst("div.view-content:not([itemprop]):contains(작가) a")!!
            .text()

        val tags = titleBlock
            .select("div.view-content:not([itemprop]):contains(분류) a")
            .map { it.text() }

        val type = titleBlock
            .selectFirst("div.view-content:not([itemprop]):contains(발행구분) a")!!
            .text()

        val thumbnail = titleBlock
            .selectFirst(Evaluator.Tag("img"))!!
            .attr("src")

        val thumbsUpCount = titleBlock
            .select("i.fa-thumbs-up + b")
            .text()
            .toInt()

        val entries = doc
            .select("div.serial-list .list-item")
            .map { entry ->
                val episode = entry
                    .selectFirst(Evaluator.Class("wr-num"))!!
                    .text()
                    .toInt()
                val subject = entry.selectFirst(Evaluator.Class("item-subject"))!!
                val entryItemID = subject
                    .attr("href")
                    .dropLastWhile { it != '?' }
                    .dropLast(1)
                    .takeLastWhile { it != '/' }
                val entryTitle = subject.ownText()
                val starRating = entry.selectFirst(Evaluator.Class("wr-star"))!!
                    .text()
                    .drop(1)
                    .takeWhile { it != ')' }
                    .toFloat()
                val date = entry.selectFirst(Evaluator.Class("wr-date"))!!.text()
                val viewCount = entry
                    .selectFirst(Evaluator.Class("wr-hit"))!!
                    .text()
                    .replace(",", "")
                    .toInt()
                val entryThumbsUpCount = entry
                    .selectFirst(Evaluator.Class("wr-good"))!!
                    .text()
                    .replace(",", "")
                    .toInt()

                MangaListingEntry(
                    entryItemID,
                    episode,
                    entryTitle,
                    starRating,
                    date,
                    viewCount,
                    entryThumbsUpCount
                )
            }

        return MangaListing(
            itemID,
            title,
            thumbnail,
            author,
            tags,
            type,
            thumbsUpCount,
            entries
        )
    }

    /**
     * Fetch MangaListing or ReaderInfo.
     * Returns null when exception occurs.
     */
    suspend fun getItem(itemID: String): Any? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = Jsoup.parse(
                httpClient
                    .get("https://manatoki.net/comic/$itemID")
                    .bodyAsText()
            )

            if (doc.getElementsByClass("serial-list").isEmpty())
                parseReaderInfo(itemID, doc)
            else
                parseMangaListing(itemID, doc)

        }.getOrNull()
    }

    suspend fun reloadCaptcha(): ByteBuffer? = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.post("$captchaUrl/kcaptcha_session.php")

            ByteBuffer.wrap(
                httpClient
                    .get("$captchaUrl/kcaptcha_image.php?t=${System.currentTimeMillis()}")
                    .body<ByteArray>()
            )
        }.getOrNull()
    }

    suspend fun checkCaptcha(key: String): Boolean? = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.submitForm(
                url = "$baseUrl/bbs/captcha_check.php",
                formParameters = Parameters.build {
                    append("captcha_key", key)
                }
            ).status == HttpStatusCode.Found
        }.getOrNull()
    }
}
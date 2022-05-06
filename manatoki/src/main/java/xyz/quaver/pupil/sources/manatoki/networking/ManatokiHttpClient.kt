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
import xyz.quaver.pupil.sources.manatoki.ManatokiDatabase
import java.nio.ByteBuffer

@Serializable
data class MangaThumbnail(
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

@Serializable
data class MainData(
    val recentUpload: List<MangaThumbnail>,
    val mangaList: List<MangaThumbnail>,
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

@Suppress("NonAsciiCharacters", "EnumEntryName")
@Parcelize
@Serializable
data class SearchParameters(
    // 발행
    val publish: Publish? = null,
    // 초성
    val jaum: Jaum? = null,
    // 장르
    val tag: Set<Tag> = emptySet(),
    // 정렬
    val sst: Sort? = null,
    // 오름/내림
    val sod: Order = Order.내림차순,
    // 제목
    val stx: String? = null,
    // 작가
    val artist: String? = null,
    val page: Int = 1
): Parcelable {

    init {
        require(page > 0) { "Page must be a positive number" }
    }

    enum class Publish {
        주간,
        격주,
        월간,
        단편,
        단행본,
        완결
    }

    enum class Jaum {
        ㄱ,
        ㄴ,
        ㄷ,
        ㄹ,
        ㅁ,
        ㅂ,
        ㅅ,
        ㅇ,
        ㅈ,
        ㅊ,
        ㅋ,
        ㅌ,
        ㅍ,
        ㅎ,
        `0-9`,
        `a-z`
    }

    enum class Tag {
        `17`,
        BL,
        SF,
        TS,
        개그,
        게임,
        도박,
        드라마,
        라노벨,
        러브코미디,
        먹방,
        백합,
        붕탁,
        순정,
        스릴러,
        스포츠,
        시대,
        애니화,
        액션,
        음악,
        이세계,
        일상,
        전생,
        추리,
        판타지,
        학원,
        호러
    }

    enum class Sort(val value: String) {
        인기순("as_view"),
        추천순("as_good"),
        댓글순("as_comment"),
        북마크순("as_bookmark")
    }

    enum class Order(val value: String) {
        내림차순("desc"),
        오름차순("asc")
    }
}

@Parcelize
@Serializable
data class SearchResultEntry(
    val itemID: String,
    val title: String,
    val thumbnail: String,
    val artist: String,
    val type: String,
    val lastUpdate: String
): Parcelable

@Parcelize
@Serializable
data class SearchResult(
    val result: List<SearchResultEntry>,
    val availableArtists: List<String>,
    val maxPage: Int
): Parcelable


class ManatokiHttpClient(
    engine: HttpClientEngine,
    database: ManatokiDatabase
) {

    val httpClient = HttpClient(engine) {
        install(ManatokiCaptcha)
        install(HttpCookies) {
            storage = ManatokiCookiesStorage(database)
        }
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

                    MangaThumbnail(itemID, title, thumbnail)
                }

            val mangaList = misoPostGallery[1]
                .select(".post-image > a")
                .map { entry ->
                    val itemID = entry.attr("href").takeLastWhile { it != '/' }
                    val title = entry.selectFirst("div.in-subject")!!.ownText()
                    val thumbnail = entry.selectFirst(Evaluator.Tag("img"))!!.attr("src")

                    MangaThumbnail(itemID, title, thumbnail)
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

    suspend fun recent(page: Int): List<MangaThumbnail>? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = Jsoup.parse(
                httpClient
                    .get("$baseUrl/bbs/page.php?hid=update&page=$page")
                    .bodyAsText()
            )

            doc.select("div.post-list").map { elem ->
                val itemID = elem.attr("rel")
                val title = elem.selectFirst(".post-subject > a")!!.ownText()

                val thumbnail = elem.getElementsByTag("img").attr("src")

                MangaThumbnail(itemID, title, thumbnail)
            }
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    /**
     * Search result
     * Returns a triple of search results, available artists, and max page.
     */
    suspend fun search(parameters: SearchParameters): SearchResult? = withContext(Dispatchers.IO) {
        runCatching {
            with(parameters) {
                val doc = Jsoup.parse(
                    httpClient.get("$baseUrl/comic/p$page") {
                        publish?.let { parameter("publish", publish.name) }
                        jaum?.let { parameter("jaum", jaum.name) }
                        if (tag.isNotEmpty()) parameter("tag", tag.joinToString(",") { it.name })
                        sst?.let { parameter("sst", sst.name) }
                        stx?.let { parameter("stx", stx) }
                        artist?.let { parameter("artist", artist) }
                    }.bodyAsText()
                )

                val maxPage = doc
                    .select(".pagination a")
                    .maxOf { it.text().toIntOrNull() ?: 1 }

                val availableArtists = doc.select("select > option").mapNotNull { elem ->
                    val value = elem.ownText()
                    value.ifEmpty { null }
                }

                val result = doc.getElementsByClass("list-item").map { elem ->
                    val itemID = elem
                        .selectFirst(".img-item > a")!!
                        .attr("href")
                        .takeLastWhile { it != '/' }

                    val title = elem
                        .selectFirst(Evaluator.Class("title"))!!
                        .text()

                    val thumbnail = elem
                        .selectFirst(Evaluator.Tag("img"))!!
                        .attr("src")

                    val artist = elem
                        .selectFirst(Evaluator.Class("list-artist"))!!
                        .text()

                    val type = elem
                        .selectFirst(Evaluator.Class("list-publish"))!!
                        .text()

                    val lastUpdate = elem
                        .selectFirst(Evaluator.Class("list-date"))!!
                        .text()

                    SearchResultEntry(
                        itemID,
                        title,
                        thumbnail,
                        artist,
                        type,
                        lastUpdate
                    )
                }

                SearchResult(result, availableArtists, maxPage)
            }
        }
    }.getOrNull()
}
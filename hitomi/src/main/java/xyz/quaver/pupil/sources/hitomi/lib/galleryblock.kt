/*
 *     Pupil, Hitomi.la viewer for Android
 *     Copyright (C) 2021 tom5079
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.quaver.pupil.sources.hitomi.lib

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.net.URLDecoder

//galleryblock.js
@Serializable
data class GalleryBlock(
    val id: Int,
    val galleryUrl: String,
    val thumbnails: List<String>,
    val title: String,
    val artists: List<String>,
    val series: List<String>,
    val type: String,
    val language: String,
    val relatedTags: List<String>
)

suspend fun DIAware.getGalleryBlock(galleryID: Int) : GalleryBlock = withContext(Dispatchers.IO) {
    val client: HttpClient by instance()

    val url = "$protocol//$domain/$galleryblockdir/$galleryID$extension"

    val doc = Jsoup.parse(rewriteTnPaths(client.get(url)))

    val galleryUrl = doc.selectFirst("h1 > a")!!.attr("href")

    val thumbnails = doc.select(".dj-img-cont img").map { protocol + it.attr("src") }

    val title = doc.selectFirst("h1 > a")!!.text()
    val artists = doc.select(".artist-list a").map{ it.text() }
    val series = doc.select(".dj-content a[href~=^/series/]").map { it.text() }
    val type = doc.selectFirst("a[href~=^/type/]")!!.text()

    val language = run {
        val href = doc.select("a[href~=^/index.+\\.html\$]").attr("href")
        Regex("""index-([^-]+)(-.+)?\.html""").find(href)?.groupValues?.getOrNull(1) ?: ""
    }

    val relatedTags = doc.select(".relatedtags a").map {
        val href = URLDecoder.decode(it.attr("href"), "UTF-8")
        href.slice(5 until href.indexOf("-all"))
    }

    GalleryBlock(galleryID, galleryUrl, thumbnails, title, artists, series, type, language, relatedTags)
}

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.kodein.di.DIAware
import org.kodein.di.instance

const val protocol = "https:"

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = true
}

suspend fun DIAware.getGalleryInfo(galleryID: Int): GalleryInfo = withContext(Dispatchers.IO) {
    val client: HttpClient by instance()

    json.decodeFromString(
        client.get<String>("$protocol//$domain/galleries/$galleryID.js")
            .replace("var galleryinfo = ", "")
    )
}

//common.js
const val domain = "ltn.hitomi.la"
const val galleryblockextension = ".html"
const val galleryblockdir = "galleryblock"
const val nozomiextension = ".nozomi"

suspend fun DIAware.subdomainFromURL(url: String, base: String? = null) : String {
    var retval = base ?: "b"

    val b = 16

    val r = Regex("""/[0-9a-f]{61}([0-9a-f]{2})([0-9a-f])""")
    val m = r.find(url) ?: return "a"

    val g = m.groupValues.let { it[2]+it[1] }.toIntOrNull(b)

    if (g != null) {
        retval = (97+ gg_m(g)).toChar().toString() + retval
    }

    return retval
}
suspend fun DIAware.urlFromUrl(url: String, base: String? = null) : String {
    return url.replace(Regex("""//..?\.hitomi\.la/"""), "//${subdomainFromURL(url, base)}.hitomi.la/")
}


suspend fun DIAware.fullPathFromHash(hash: String) : String =
    "${gg_b()}${gg_s(hash)}/$hash"

fun realFullPathFromHash(hash: String): String =
    hash.replace(Regex("""^.*(..)(.)$"""), "$2/$1/$hash")

suspend fun DIAware.urlFromHash(galleryID: Int, image: GalleryFiles, dir: String? = null, ext: String? = null) : String {
    val ext = ext ?: dir ?: image.name.takeLastWhile { it != '.' }
    val dir = dir ?: "images"
    return "https://a.hitomi.la/$dir/${fullPathFromHash(image.hash)}.$ext"
}

suspend fun DIAware.urlFromUrlFromHash(galleryID: Int, image: GalleryFiles, dir: String? = null, ext: String? = null, base: String? = null) =
    if (base == "tn")
        urlFromUrl("https://a.hitomi.la/$dir/${realFullPathFromHash(image.hash)}.$ext", base)
    else
        urlFromUrl(urlFromHash(galleryID, image, dir, ext), base)

suspend fun DIAware.rewriteTnPaths(html: String) =
    html.replace(Regex("""//tn\.hitomi\.la/[^/]+/[0-9a-f]/[0-9a-f]{2}/[0-9a-f]{64}""")) { url ->
        runBlocking { urlFromUrl(url.value, "tn") }
    }

suspend fun DIAware.imageUrlFromImage(galleryID: Int, image: GalleryFiles, noWebp: Boolean) : String {
    return when {
        noWebp ->
            urlFromUrlFromHash(galleryID, image)
        //image.hasavif != 0 ->
        //    urlFromUrlFromHash(galleryID, image, "avif", null, "a")
        image.haswebp != 0 ->
            urlFromUrlFromHash(galleryID, image, "webp", null, "a")
        else ->
            urlFromUrlFromHash(galleryID, image)
    }
}
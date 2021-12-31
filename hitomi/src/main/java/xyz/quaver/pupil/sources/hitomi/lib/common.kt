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
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.suspendCoroutine

const val protocol = "https:"

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = true
}

suspend fun getGalleryInfo(client: HttpClient, galleryID: Int): GalleryInfo = withContext(Dispatchers.IO) {
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

suspend fun subdomainFromURL(client: HttpClient, url: String, base: String? = null) : String {
    var retval = base ?: "b"

    val b = 16

    val r = Regex("""/[0-9a-f]{61}([0-9a-f]{2})([0-9a-f])""")
    val m = r.find(url) ?: return "a"

    val g = m.groupValues.let { it[2]+it[1] }.toIntOrNull(b)

    if (g != null) {
        gg.getInstance(client).let { gg ->
            retval = (97+ gg.m(g)).toChar().toString() + retval
        }
    }

    return retval
}
suspend fun urlFromUrl(client: HttpClient, url: String, base: String? = null) : String {
    return url.replace(Regex("""//..?\.hitomi\.la/"""), "//${subdomainFromURL(client, url, base)}.hitomi.la/")
}


suspend fun fullPathFromHash(client: HttpClient, hash: String) : String = gg.getInstance(client).let { gg ->
    "${gg.b}${gg.s(hash)}/$hash"
}

fun realFullPathFromHash(hash: String): String =
    hash.replace(Regex("""^.*(..)(.)$"""), "$2/$1/$hash")

suspend fun urlFromHash(client: HttpClient, galleryID: Int, image: GalleryFiles, dir: String? = null, ext: String? = null) : String {
    val ext = ext ?: dir ?: image.name.takeLastWhile { it != '.' }
    val dir = dir ?: "images"
    return "https://a.hitomi.la/$dir/${fullPathFromHash(client, image.hash)}.$ext"
}

suspend fun urlFromUrlFromHash(client: HttpClient, galleryID: Int, image: GalleryFiles, dir: String? = null, ext: String? = null, base: String? = null) =
    if (base == "tn")
        urlFromUrl(client, "https://a.hitomi.la/$dir/${realFullPathFromHash(image.hash)}.$ext", base)
    else
        urlFromUrl(client, urlFromHash(client, galleryID, image, dir, ext), base)

suspend fun rewriteTnPaths(client: HttpClient, html: String) =
    html.replace(Regex("""//tn\.hitomi\.la/[^/]+/[0-9a-f]/[0-9a-f]{2}/[0-9a-f]{64}""")) { url ->
        runBlocking { urlFromUrl(client, url.value, "tn") }
    }

suspend fun imageUrlFromImage(client: HttpClient, galleryID: Int, image: GalleryFiles, noWebp: Boolean) : String {
    return when {
        noWebp ->
            urlFromUrlFromHash(client, galleryID, image)
        //image.hasavif != 0 ->
        //    urlFromUrlFromHash(galleryID, image, "avif", null, "a")
        image.haswebp != 0 ->
            urlFromUrlFromHash(client, galleryID, image, "webp", null, "a")
        else ->
            urlFromUrlFromHash(client, galleryID, image)
    }
}
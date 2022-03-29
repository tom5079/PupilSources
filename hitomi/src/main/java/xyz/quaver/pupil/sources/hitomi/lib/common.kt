/*
 *    Copyright 2019 tom5079
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package xyz.quaver.pupil.sources.hitomi.lib

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.kodein.log.now
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal inline fun <R> logTime(message: String, block: () -> R): R {
    val retval: R

    Log.d("PUPILD", "start $message")

    val time = measureTimeMillis {
        retval = block()
    }

    Log.d("PUPILD", "$message ($time ms)")

    return retval
}

const val protocol = "https:"

@Serializable
data class Artist(
    val artist: String,
    val url: String
)

@Serializable
data class Group(
    val group: String,
    val url: String
)

@Serializable
data class Parody(
    val parody: String,
    val url: String
)

@Serializable
data class Character(
    val character: String,
    val url: String
)

@Serializable
data class Tag(
    val tag: String,
    val url: String,
    val female: String? = null,
    val male: String? = null
) {
    override fun toString() = buildString {
        if (!female.isNullOrEmpty()) append("female:")
        if (!male.isNullOrEmpty()) append("male:")
        append(tag)
    }
}

@Serializable
data class Language(
    val galleryid: String,
    val url: String,
    val language_localname: String,
    val name: String
)

val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
    useArrayPolymorphism = true
}

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun HttpClient.getGalleryInfo(galleryID: Int) = withContext(Dispatchers.IO) {
    json.decodeFromString<GalleryInfo>(
        this@getGalleryInfo.get<String>("$protocol//$domain/galleries/$galleryID.js")
            .replace("var galleryinfo = ", "")
    )
}

//common.js
const val domain = "ltn.hitomi.la"
const val galleryblockextension = ".html"
const val galleryblockdir = "galleryblock"
const val nozomiextension = ".nozomi"

object gg {
    private var lastRetrieval: Instant? = null

    private val mutex = Mutex()

    private var mDefault = 0
    private val mMap = mutableMapOf<Int, Int>()

    private var b = ""

    @OptIn(ExperimentalTime::class)
    private suspend fun refresh(client: HttpClient) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (lastRetrieval == null || (lastRetrieval!! + 1.minutes) < now()) {
                val ggjs: String = client.get("https://ltn.hitomi.la/gg.js")

                mDefault = Regex("var o = (\\d)").find(ggjs)!!.groupValues[1].toInt()
                val o = Regex("o = (\\d); break;").find(ggjs)!!.groupValues[1].toInt()

                mMap.clear()
                Regex("case (\\d+):").findAll(ggjs).forEach {
                    val case = it.groupValues[1].toInt()
                    mMap[case] = o
                }

                b = Regex("b: '(.+)'").find(ggjs)!!.groupValues[1]

                lastRetrieval = now()
            }
        }
    }

    suspend fun m(client: HttpClient, g: Int): Int {
        refresh(client)

        return mMap[g] ?: mDefault
    }

    suspend fun b(client: HttpClient): String {
        refresh(client)
        return b
    }
    fun s(h: String): String {
        val m = Regex("(..)(.)$").find(h)
        return m!!.groupValues.let { it[2]+it[1] }.toInt(16).toString(10)
    }
}

suspend fun HttpClient.subdomainFromURL(url: String, base: String? = null) : String {
    var retval = "b"

    if (!base.isNullOrBlank())
        retval = base

    val b = 16

    val r = Regex("""/[0-9a-f]{61}([0-9a-f]{2})([0-9a-f])""")
    val m = r.find(url) ?: return "a"

    val g = m.groupValues.let { it[2]+it[1] }.toIntOrNull(b)

    if (g != null) {
        retval = (97+ gg.m(this, g)).toChar().toString() + retval
    }

    return retval
}

suspend fun HttpClient.urlFromUrl(url: String, base: String? = null) : String {
    return url.replace(Regex("""//..?\.hitomi\.la/"""), "//${subdomainFromURL(url, base)}.hitomi.la/")
}

suspend fun HttpClient.fullPathFromHash(hash: String) : String =
    "${gg.b(this)}${gg.s(hash)}/$hash"

fun realFullPathFromHash(hash: String): String =
    hash.replace(Regex("""^.*(..)(.)$"""), "$2/$1/$hash")

suspend fun HttpClient.urlFromHash(image: GalleryFiles, dir: String? = null, ext: String? = null) : String {
    val ext = ext ?: dir ?: image.name.takeLastWhile { it != '.' }
    val dir = dir ?: "images"
    return "https://a.hitomi.la/$dir/${fullPathFromHash(image.hash)}.$ext"
}

suspend fun HttpClient.urlFromUrlFromHash(image: GalleryFiles, dir: String? = null, ext: String? = null, base: String? = null) =
    if (base == "tn")
        urlFromUrl("https://a.hitomi.la/$dir/${realFullPathFromHash(image.hash)}.$ext", base)
    else
        urlFromUrl(urlFromHash(image, dir, ext), base)

suspend fun HttpClient.rewriteTnPaths(html: String): String {
    val match = Regex("""//tn\.hitomi\.la/[^/]+/[0-9a-f]/[0-9a-f]{2}/[0-9a-f]{64}""").find(html) ?: return html

    val replacement = urlFromUrl(match.value, "tn")

    return html.replaceRange(match.range, replacement)
}

suspend fun HttpClient.imageUrlFromImage(image: GalleryFiles): List<String> {
    val imageList = mutableListOf<String>()

    if (image.hasavif != 0)
        imageList.add(urlFromUrlFromHash(image, "avif"))

    imageList.add(urlFromUrlFromHash(image, "webp", null, "a"))

    return imageList.toList()
}

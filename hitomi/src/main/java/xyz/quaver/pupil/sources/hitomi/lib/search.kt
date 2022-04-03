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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.security.MessageDigest
import kotlin.math.min

//searchlib.js
const val separator = "-"
const val extension = ".html"
const val index_dir = "tagindex"
const val galleries_index_dir = "galleriesindex"
const val max_node_size = 464
const val B = 16
const val compressed_nozomi_prefix = "n"

private var tag_index_version: String? = null
suspend fun HttpClient.getTagIndexVersion(): String = withContext(Dispatchers.IO) {
    tag_index_version ?: getIndexVersion("tagindex").also {
        tag_index_version = it
    }
}

private var galleries_index_version: String? = null
suspend fun HttpClient.getGalleriesIndexVersion(): String = withContext(Dispatchers.IO) {
    galleries_index_version ?: getIndexVersion("galleriesindex").also {
        galleries_index_version = it
    }
}

fun sha256(data: ByteArray) : ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(data)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun hashTerm(term: String) : UByteArray {
    return sha256(term.toByteArray()).toUByteArray().sliceArray(0 until 4)
}

fun sanitize(input: String) : String {
    return input.replace(Regex("[/#]"), "")
}

suspend fun HttpClient.getIndexVersion(name: String): String =
        get("$protocol//$domain/$name/version?_=${System.currentTimeMillis()}")

//search.js
@OptIn(ExperimentalUnsignedTypes::class)
suspend fun HttpClient.getGalleryIDsForQuery(query: String) : IntBuffer {
    query.replace("_", " ").let {
        if (it.indexOf(':') > -1) {
            val sides = it.split(":")
            val ns = sides[0]
            var tag = sides[1]

            var area : String? = ns
            var language = "all"
            when (ns) {
                "female", "male" -> {
                    area = "tag"
                    tag = it
                }
                "language" -> {
                    area = null
                    language = tag
                    tag = "index"
                }
            }

            return getGalleryIDsFromNozomi(area, tag, language)
        }

        val key = hashTerm(it)
        val field = "galleries"

        val node = getNodeAtAddress(field, 0)

        val data = bSearch(field, key, node)

        if (data != null)
            return getGalleryIDsFromData(data)

        return IntBuffer.allocate(0)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun HttpClient.getSuggestionsForQuery(query: String) : List<Suggestion> {
    query.replace('_', ' ').let {
        var field = "global"
        var term = it

        if (term.indexOf(':') > -1) {
            val sides = it.split(':')
            field = sides[0]
            term = sides[1]
        }

        val key = hashTerm(term)
        val node = getNodeAtAddress(field, 0)
        val data = bSearch(field, key, node)

        if (data != null)
            return getSuggestionsFromData(field, data)

        return emptyList()
    }
}

data class Suggestion(val s: String, val t: Int, val u: String, val n: String)
suspend fun HttpClient.getSuggestionsFromData(field: String, data: Pair<Long, Int>) : List<Suggestion> {
    val url = "$protocol//$domain/$index_dir/$field.${getTagIndexVersion()}.data"
    val (offset, length) = data
    if (length > 10000 || length <= 0)
        throw Exception("length $length is too long")

    val buffer = getURLAtRange(url, offset.until(offset+length))

    val suggestions = ArrayList<Suggestion>()

    val numberOfSuggestions = buffer.int

    if (numberOfSuggestions > 100 || numberOfSuggestions <= 0)
        throw Exception("number of suggestions $numberOfSuggestions is too long")

    for (i in 0.until(numberOfSuggestions)) {
        var top = buffer.int

        val ns = ByteArray(top).apply { buffer.get(this) }.toString(charset("UTF-8"))

        top = buffer.int

        val tag = ByteArray(top).apply { buffer.get(this) }.toString(charset("UTF-8"))

        val count = buffer.int

        val tagname = sanitize(tag)
        val u = when(ns) {
            "female", "male" -> "/tag/$ns:$tagname${separator}1$extension"
            "language" -> "/index-$tagname${separator}1$extension"
            else -> "/$ns/$tagname${separator}all${separator}1$extension"
        }

        suggestions.add(Suggestion(tag, count, u, ns))
    }

    return suggestions
}

suspend fun HttpClient.getGalleryIDsFromNozomi(area: String?, tag: String, language: String) : IntBuffer {
    val nozomiAddress =
            when(area) {
                null -> "$protocol//$domain/$compressed_nozomi_prefix/$tag-$language$nozomiextension"
                else -> "$protocol//$domain/$compressed_nozomi_prefix/$area/$tag-$language$nozomiextension"
            }

    return withContext(Dispatchers.IO) {
        val contentLength = async {
            head<HttpResponse>(nozomiAddress) {
                header("Accept-Encoding", "identity")
            }.headers[HttpHeaders.ContentLength]!!.toInt()
        }

        get<HttpStatement>(nozomiAddress).execute { response ->
            ByteBuffer.allocateDirect(contentLength.await()).apply {
                val channel: ByteReadChannel = response.receive()

                val bytesRead = channel.readFully(this)

                assert(bytesRead == this.capacity())
                assert(!this.hasRemaining())
                assert(channel.availableForRead == 0)

                rewind()
            }.asIntBuffer()
        }
    }
}

suspend fun HttpClient.getGalleryIDsFromData(data: Pair<Long, Int>) : IntBuffer {
    val url = "$protocol//$domain/$galleries_index_dir/galleries.${getGalleriesIndexVersion()}.data"
    val (offset, length) = data
    if (length > 100000000 || length <= 0)
        throw Exception("length $length is too long")

    return getURLAtRange(url, offset.until(offset+length)).asIntBuffer()
}

suspend fun HttpClient.getNodeAtAddress(field: String, address: Long) : Node {
    val url =
            when(field) {
                "galleries" -> "$protocol//$domain/$galleries_index_dir/galleries.${getGalleriesIndexVersion()}.index"
                "languages" -> "$protocol//$domain/$galleries_index_dir/languages.${getGalleriesIndexVersion()}.index"
                "nozomiurl" -> "$protocol//$domain/$galleries_index_dir/nozomiurl.${getGalleriesIndexVersion()}.index"
                else -> "$protocol//$domain/$index_dir/$field.${getTagIndexVersion()}.index"
            }

    val nodedata = getURLAtRange(url, address.until(address+ max_node_size))

    return decodeNode(nodedata)
}

suspend fun HttpClient.getURLAtRange(url: String, range: LongRange) : ByteBuffer = withContext(Dispatchers.IO) {
    get<HttpStatement>(url) {
        header("Range", "bytes=${range.first}-${range.last}")
    }.execute { response ->
        ByteBuffer.allocateDirect((range.last-range.first+1).toInt()).apply {
            val channel: ByteReadChannel = response.receive()

            val bytesRead = channel.readFully(this)

            assert(bytesRead == this.capacity())
            assert(!this.hasRemaining())
            assert(channel.availableForRead == 0)

            rewind()
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
data class Node(val keys: List<UByteArray>, val datas: List<Pair<Long, Int>>, val subNodeAddresses: List<Long>)
@OptIn(ExperimentalUnsignedTypes::class)
fun decodeNode(buffer: ByteBuffer) : Node {
    val numberOfKeys = buffer.int
    val keys = ArrayList<UByteArray>()

    for (i in 0.until(numberOfKeys)) {
        val keySize = buffer.int

        if (keySize == 0 || keySize > 32)
            throw Exception("fatal: !keySize || keySize > 32")

        keys.add(ByteArray(keySize).apply { buffer.get(this) }.toUByteArray())
    }

    val numberOfDatas = buffer.int
    val datas = ArrayList<Pair<Long, Int>>()

    for (i in 0.until(numberOfDatas)) {
        val offset = buffer.long
        val length = buffer.int

        datas.add(Pair(offset, length))
    }

    val numberOfSubNodeAddresses = B +1
    val subNodeAddresses = ArrayList<Long>()

    for (i in 0.until(numberOfSubNodeAddresses)) {
        val subNodeAddress = buffer.long
        subNodeAddresses.add(subNodeAddress)
    }

    return Node(keys, datas, subNodeAddresses)
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun HttpClient.bSearch(field: String, key: UByteArray, node: Node) : Pair<Long, Int>? {
    fun compareArrayBuffers(dv1: UByteArray, dv2: UByteArray) : Int {
        val top = min(dv1.size, dv2.size)

        for (i in 0.until(top)) {
            if (dv1[i] < dv2[i])
                return -1
            else if (dv1[i] > dv2[i])
                return 1
        }

        return 0
    }

    fun locateKey(key: UByteArray, node: Node) : Pair<Boolean, Int> {
        for (i in node.keys.indices) {
            val cmpResult = compareArrayBuffers(key, node.keys[i])

            if (cmpResult <= 0)
                return Pair(cmpResult==0, i)
        }

        return Pair(false, node.keys.size)
    }

    fun isLeaf(node: Node) : Boolean {
        for (subnode in node.subNodeAddresses)
            if (subnode != 0L)
                return false

        return true
    }

    if (node.keys.isEmpty())
        return null

    val (there, where) = locateKey(key, node)
    if (there)
        return node.datas[where]
    else if (isLeaf(node))
        return null

    val nextNode = getNodeAtAddress(field, node.subNodeAddresses[where])
    return bSearch(field, key, nextNode)
}
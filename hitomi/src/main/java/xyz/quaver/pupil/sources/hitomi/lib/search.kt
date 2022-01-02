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
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

private var tagIndexVersion: String? = null
suspend fun DIAware.getTagIndexVersion(): String = tagIndexVersion ?: getIndexVersion("tagindex").also {
    tagIndexVersion = it
}

private var galleriesIndexVersion: String? = null
suspend fun DIAware.getGalleriesIndexVersion(): String = galleriesIndexVersion ?: getIndexVersion("galleriesindex").also {
    galleriesIndexVersion = it
}

fun sha256(data: ByteArray) : ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(data)
}

@ExperimentalUnsignedTypes
fun hashTerm(term: String) : UByteArray {
    return sha256(term.toByteArray()).toUByteArray().sliceArray(0 until 4)
}

fun sanitize(input: String) : String {
    return input.replace(Regex("[/#]"), "")
}

suspend fun DIAware.getIndexVersion(name: String): String {
    val client: HttpClient by instance()
    return client.get("$protocol//$domain/$name/version?_=${System.currentTimeMillis()}")
}

//search.js
@OptIn(ExperimentalUnsignedTypes::class)
suspend fun DIAware.getGalleryIDsForQuery(query: String) : Set<Int> {
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

        val node = getNodeAtAddress(field, 0) ?: return emptySet()

        val data = bSearch(field, key, node)

        if (data != null)
            return getGalleryIDsFromData(data)

        return emptySet()
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun DIAware.getSuggestionsForQuery(query: String) : List<Suggestion> {
    query.replace('_', ' ').let {
        var field = "global"
        var term = it

        if (term.indexOf(':') > -1) {
            val sides = it.split(':')
            field = sides[0]
            term = sides[1]
        }

        val key = hashTerm(term)
        val node = getNodeAtAddress(field, 0) ?: return emptyList()
        val data = bSearch(field, key, node)

        if (data != null)
            return getSuggestionsFromData(field, data)

        return emptyList()
    }
}

data class Suggestion(val s: String, val t: Int, val u: String, val n: String)
suspend fun DIAware.getSuggestionsFromData(field: String, data: Pair<Long, Int>) : List<Suggestion> {
    val url = "$protocol//$domain/$index_dir/$field.${getTagIndexVersion()}.data"
    val (offset, length) = data
    if (length > 10000 || length <= 0)
        throw Exception("length $length is too long")

    val inbuf = getURLAtRange(url, offset.until(offset+length))

    val suggestions = ArrayList<Suggestion>()

    val buffer = ByteBuffer
        .wrap(inbuf)
        .order(ByteOrder.BIG_ENDIAN)
    val numberOfSuggestions = buffer.int

    if (numberOfSuggestions > 100 || numberOfSuggestions <= 0)
        throw Exception("number of suggestions $numberOfSuggestions is too long")

    for (i in 0.until(numberOfSuggestions)) {
        var top = buffer.int

        val ns = inbuf.sliceArray(buffer.position().until(buffer.position()+top)).toString(charset("UTF-8"))
        buffer.position(buffer.position()+top)

        top = buffer.int

        val tag = inbuf.sliceArray(buffer.position().until(buffer.position()+top)).toString(charset("UTF-8"))
        buffer.position(buffer.position()+top)

        val count = buffer.int

        val tagname = sanitize(tag)
        val u =
            when(ns) {
                "female", "male" -> "/tag/$ns:$tagname${separator}1$extension"
                "language" -> "/index-$tagname${separator}1$extension"
                else -> "/$ns/$tagname${separator}all${separator}1$extension"
            }

        suggestions.add(Suggestion(tag, count, u, ns))
    }

    return suggestions
}

suspend fun DIAware.getGalleryIDsFromNozomi(area: String?, tag: String, language: String) : Set<Int> = withContext(Dispatchers.IO) {
    val client: HttpClient by instance()

    val nozomiAddress =
        when(area) {
            null -> "$protocol//$domain/$compressed_nozomi_prefix/$tag-$language$nozomiextension"
            else -> "$protocol//$domain/$compressed_nozomi_prefix/$area/$tag-$language$nozomiextension"
        }

    val bytes: ByteArray = try {
        client.get(nozomiAddress)
    } catch (e: Exception) {
        return@withContext emptySet()
    }

    val nozomi = mutableSetOf<Int>()

    val arrayBuffer = ByteBuffer
        .wrap(bytes)
        .order(ByteOrder.BIG_ENDIAN)

    while (arrayBuffer.hasRemaining())
        nozomi.add(arrayBuffer.int)

    nozomi
}

suspend fun DIAware.getGalleryIDsFromData(data: Pair<Long, Int>) : Set<Int> {
    val url = "$protocol//$domain/$galleries_index_dir/galleries.${getGalleriesIndexVersion()}.data"
    val (offset, length) = data
    if (length > 100000000 || length <= 0)
        throw Exception("length $length is too long")

    val inbuf = getURLAtRange(url, offset.until(offset+length))

    val galleryIDs = mutableSetOf<Int>()

    val buffer = ByteBuffer
        .wrap(inbuf)
        .order(ByteOrder.BIG_ENDIAN)

    val numberOfGalleryIDs = buffer.int

    val expectedLength = numberOfGalleryIDs*4+4

    if (numberOfGalleryIDs > 10000000 || numberOfGalleryIDs <= 0)
        throw Exception("number_of_galleryids $numberOfGalleryIDs is too long")
    else if (inbuf.size != expectedLength)
        throw Exception("inbuf.byteLength ${inbuf.size} != expected_length $expectedLength")

    for (i in 0.until(numberOfGalleryIDs))
        galleryIDs.add(buffer.int)

    return galleryIDs
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun DIAware.getNodeAtAddress(field: String, address: Long) : Node? {
    val url =
        when(field) {
            "galleries" -> "$protocol//$domain/$galleries_index_dir/galleries.${getGalleriesIndexVersion()}.index"
            "languages" -> "$protocol//$domain/$galleries_index_dir/languages.${getGalleriesIndexVersion()}.index"
            "nozomiurl" -> "$protocol//$domain/$galleries_index_dir/nozomiurl.${getGalleriesIndexVersion()}.index"
            else -> "$protocol//$domain/$index_dir/$field.${getTagIndexVersion()}.index"
        }

    val nodedata = getURLAtRange(url, address.until(address+max_node_size))

    return decodeNode(nodedata)
}

suspend fun DIAware.getURLAtRange(url: String, range: LongRange) : ByteArray = withContext(Dispatchers.IO) {
    val client: HttpClient by instance()

    client.get(url) {
        header("Range", "bytes=${range.first}-${range.last}")
    }
}

@ExperimentalUnsignedTypes
data class Node(val keys: List<UByteArray>, val datas: List<Pair<Long, Int>>, val subNodeAddresses: List<Long>)
@ExperimentalUnsignedTypes
fun decodeNode(data: ByteArray) : Node {
    val buffer = ByteBuffer
        .wrap(data)
        .order(ByteOrder.BIG_ENDIAN)

    val uData = data.toUByteArray()

    val numberOfKeys = buffer.int
    val keys = ArrayList<UByteArray>()

    for (i in 0.until(numberOfKeys)) {
        val keySize = buffer.int

        if (keySize == 0 || keySize > 32)
            throw Exception("fatal: !keySize || keySize > 32")

        keys.add(uData.sliceArray(buffer.position().until(buffer.position()+keySize)))
        buffer.position(buffer.position()+keySize)
    }

    val numberOfDatas = buffer.int
    val datas = ArrayList<Pair<Long, Int>>()

    for (i in 0.until(numberOfDatas)) {
        val offset = buffer.long
        val length = buffer.int

        datas.add(Pair(offset, length))
    }

    val numberOfSubNodeAddresses = B+1
    val subNodeAddresses = ArrayList<Long>()

    for (i in 0.until(numberOfSubNodeAddresses)) {
        val subNodeAddress = buffer.long
        subNodeAddresses.add(subNodeAddress)
    }

    return Node(keys, datas, subNodeAddresses)
}

@ExperimentalUnsignedTypes
suspend fun DIAware.bSearch(field: String, key: UByteArray, node: Node) : Pair<Long, Int>? {
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

    val nextNode = getNodeAtAddress(field, node.subNodeAddresses[where]) ?: return null
    return bSearch(field, key, nextNode)
}
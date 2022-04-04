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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

enum class SortOptions(val area: String?, val tag: String) {
    DATE(null, "index"),
    POPULAR_TODAY("popular", "today"),
    POPULAR_WEEK("popular", "week"),
    POPULAR_MONTH("popular", "month"),
    POPULAR_YEAR("popular", "year")
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun HttpClient.doSearch(query: String, sortOption: SortOptions = SortOptions.DATE) : IntBuffer = coroutineScope {
    val terms = query
        .trim()
        .replace(Regex("""^\?"""), "")
        .lowercase()
        .split(Regex("\\s+"))
        .map {
            it.replace('_', ' ')
        }

    val positiveTerms = mutableListOf<String>()
    val negativeTerms = mutableListOf<String>()

    for (term in terms) {
        if (term.startsWith('-'))
            negativeTerms.add(term.drop(1))
        else if (term.isNotBlank())
            positiveTerms.add(term)
    }

    val positiveResultsChannel = produce {
        positiveTerms.forEach { term ->
            launch {
                runCatching {
                    getGalleryIDsForQuery(term)
                }.onSuccess {
                    send(it)
                }.onFailure {
                    close(it)
                }
            }
        }
    }

    val negativeResultsChannel = produce {
        negativeTerms.forEach {
            launch {
                runCatching {
                    getGalleryIDsForQuery(it)
                }.onSuccess {
                    send(it)
                }.onFailure {
                    close(it)
                }
            }
        }
    }

    var set = mutableSetOf<Int>().apply {
        when {
            sortOption != SortOptions.DATE ->
                getGalleryIDsFromNozomi(sortOption.area, sortOption.tag, "all")
            positiveTerms.isEmpty() ->
                getGalleryIDsFromNozomi(null, "index", "all")
            else -> null
        }?.let { buffer ->
            repeat(buffer.limit()) { i ->
                add(buffer[i])
            }
        }
    }

    repeat(positiveTerms.size) {
        val result = positiveResultsChannel.receive()

        if (set.isEmpty()) {
            repeat(result.limit()) { i -> set.add(result[i]) }
            return@repeat
        }

        set = mutableSetOf<Int>().apply {
            repeat(result.limit()) { i -> result[i].let { if (set.contains(it)) add(it) } }
        }
    }

    repeat(negativeTerms.size) {
        val result = negativeResultsChannel.receive()

        repeat(result.limit()) { i -> set.remove(result[i]) }
    }

    ByteBuffer.allocateDirect(set.size*4).asIntBuffer().apply {
        set.forEach {
            put(it)
        }

        rewind()
    }
}
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

suspend fun HttpClient.doSearch(query: String, sortOption: SortOptions = SortOptions.DATE) : IntBuffer = coroutineScope {
    val terms = query
        .trim()
        .replace(Regex("""^\?"""), "")
        .lowercase()
        .split(Regex("\\s+"))
        .map {
            it.replace('_', ' ')
        }

    val positiveTerms = LinkedList<String>()
    val negativeTerms = LinkedList<String>()

    for (term in terms) {
        if (term.matches(Regex("^-.+")))
            negativeTerms.push(term.replace(Regex("^-"), ""))
        else if (term.isNotBlank())
            positiveTerms.push(term)
    }

    val positiveResults = positiveTerms.map {
        async {
            runCatching {
                getGalleryIDsForQuery(it)
            }.getOrElse { IntBuffer.allocate(0) }
        }
    }

    val negativeResults = negativeTerms.map {
        async {
            runCatching {
                getGalleryIDsForQuery(it)
            }.getOrElse { IntBuffer.allocate(0) }
        }
    }

    val set = mutableSetOf<Int>().apply {
        when {
            sortOption != SortOptions.DATE -> getGalleryIDsFromNozomi(sortOption.area, sortOption.tag, "all")
            positiveTerms.isEmpty() -> getGalleryIDsFromNozomi(null, "index", "all")
            else -> positiveResults.first().await()
        }?.let { buffer ->
            repeat(buffer.limit()) { i ->
                add(buffer[i])
            }
        }

        positiveResults.drop(if (sortOption == SortOptions.DATE) 1 else 0).forEach {
            val result = it.await()

            repeat(result.limit()) { i ->
                remove(result[i])
            }
        }

        negativeResults.forEach {
            val result = it.await()

            repeat(result.limit()) { i ->
                remove(result[i])
            }
        }
    }

    ByteBuffer.allocateDirect(set.size*4).asIntBuffer().apply {
        set.forEach {
            put(it)
        }

        rewind()
    }
}
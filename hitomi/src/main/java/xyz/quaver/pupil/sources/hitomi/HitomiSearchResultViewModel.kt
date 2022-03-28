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

package xyz.quaver.pupil.sources.hitomi

import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import xyz.quaver.pupil.sources.base.composables.SearchBaseViewModel
import xyz.quaver.pupil.sources.hitomi.lib.*
import xyz.quaver.pupil.sources.hitomi.lib.logTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

class HitomiSearchResultViewModel(override val di: DI): SearchBaseViewModel<HitomiSearchResult>(), DIAware {
    private val client: HttpClient by instance()

    private var cachedQuery: String? = null
    private var cachedSortByPopularity: Boolean? = null
    private val cache = mutableListOf<Int>()

    private val galleryInfoCache = LruCache<Int, GalleryInfo>(100)

    var sortByPopularity by mutableStateOf(false)

    private var searchJob: Job? = null
    fun search() {
        val resultsPerPage = 25

        viewModelScope.launch {
            searchJob?.cancelAndJoin()

            searchResults.clear()
            searchBarOffset = 0
            loading = true
            error = false

            searchJob = launch {
                if (cachedQuery != query || cachedSortByPopularity != sortByPopularity || cache.isEmpty()) {
                    cachedQuery = null
                    cache.clear()

                    yield()

                    val result = withContext(Dispatchers.Unconfined) {
                        runCatching {
                            logTime("doSearch") {
                                client.doSearch(query, sortByPopularity)
                            }
                        }.onFailure {
                            error = true
                        }.getOrNull().orEmpty()
                    }

                    yield()

                    cache.addAll(result)
                    cachedQuery = query
                    totalItems = result.size
                    maxPage = ceil(result.size / resultsPerPage.toDouble()).toInt()
                }

                yield()

                val range = max((currentPage-1)*resultsPerPage, 0) until min(currentPage*resultsPerPage, totalItems)

                cache.slice(range).map { galleryID ->
                    yield()
                    loading = false
                    async(Dispatchers.Unconfined) {
                        galleryInfoCache.get(galleryID) ?: client.getGalleryInfo(galleryID).also {
                            galleryInfoCache.put(galleryID, it)
                        }
                    }
                }.forEach {
                    kotlin.runCatching {
                        searchResults.add(transform(client, it.await()))
                    }.onFailure {
                        error = true
                    }
                }
            }

            viewModelScope.launch {
                searchJob?.join()
                loading = false
            }
        }
    }

    companion object {
        suspend fun transform(client: HttpClient, galleryInfo: GalleryInfo) =
            HitomiSearchResult(
                galleryInfo.id,
                galleryInfo.title,
                client.urlFromUrlFromHash(galleryInfo.files.first(), "webpbigtn", "webp", "tn"),
                galleryInfo.artists.orEmpty().map { it.artist },
                galleryInfo.parodys.orEmpty().map { it.parody },
                galleryInfo.type,
                galleryInfo.language ?: "N/A",
                galleryInfo.tags.orEmpty().map { it.toString() }
            )
    }
}
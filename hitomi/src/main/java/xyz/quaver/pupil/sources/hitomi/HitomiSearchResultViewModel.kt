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
import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.instance
import org.kodein.di.instance
import xyz.quaver.pupil.sources.base.composables.SearchBaseViewModel
import xyz.quaver.pupil.sources.hitomi.composables.joinTags
import xyz.quaver.pupil.sources.hitomi.lib.*
import xyz.quaver.pupil.sources.hitomi.proto.HitomiSettings
import java.nio.IntBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class HitomiSearchResultViewModel(override val di: DI): ViewModel(), DIAware {
    val searchResults = mutableStateListOf<GalleryInfo>()

    var sortModeIndex by mutableStateOf(0)
        private set

    var currentPage by mutableStateOf(1)

    var totalItems by mutableStateOf(0)

    var maxPage by mutableStateOf(0)

    val prevPageAvailable by derivedStateOf { currentPage > 1 }
    val nextPageAvailable by derivedStateOf { currentPage < maxPage }

    var query by mutableStateOf<List<String>>(emptyList())

    var loading by mutableStateOf(false)
    var exception by mutableStateOf<Throwable?>(null)

    //region UI
    var isFabVisible by  mutableStateOf(true)
    var searchBarOffset by mutableStateOf(0)
    //endregion

    private val client: HttpClient by instance()

    private val settingsDataStore: DataStore<HitomiSettings> by instance()

    private var cachedQuery: String? = null
    private var cachedSortByPopularity: SortOptions? = null
    private var cache: IntBuffer? = null

    private val galleryInfoCache = LruCache<Int, GalleryInfo>(100)

    var sortOption by mutableStateOf(SortOptions.DATE)

    private var searchJob: Job? = null
    fun search() {
        val resultsPerPage = 25

        viewModelScope.launch {
            searchJob?.cancelAndJoin()

            searchResults.clear()
            searchBarOffset = 0
            loading = true
            exception = null

            searchJob = launch {
                val query = buildString {
                    append(query.joinTags())
                    val defaultQuery = settingsDataStore.data.first().defaultQuery

                    if (defaultQuery.isNotEmpty()) {
                        if (this.isNotEmpty()) append(' ')
                        append(defaultQuery)
                    }
                }

                if (cachedQuery != query || cachedSortByPopularity != sortOption || cache == null) {
                    cachedQuery = null
                    cache = null

                    yield()

                    val result = withContext(Dispatchers.Unconfined) {
                        runCatching {
                            client.doSearch(query, sortOption)
                        }.onFailure {
                            exception = it
                        }.getOrNull()
                    }

                    yield()

                    cache = result
                    cachedQuery = query
                    cachedSortByPopularity = sortOption
                    totalItems = result?.capacity() ?: 0
                    maxPage = ceil(totalItems / resultsPerPage.toDouble()).toInt()
                }

                yield()

                val range = max((currentPage-1)*resultsPerPage, 0) until min(currentPage*resultsPerPage, totalItems)

                range.map { cache!![it] }.map { galleryID ->
                    yield()
                    async(Dispatchers.Unconfined) {
                        galleryInfoCache.get(galleryID) ?: client.getGalleryInfo(galleryID).also {
                            galleryInfoCache.put(galleryID, it)
                        }
                    }
                }.forEach {
                    runCatching {
                        searchResults.add(it.await())
                        loading = false
                    }.onFailure {
                        exception = it
                    }
                }
            }

            viewModelScope.launch {
                searchJob?.join()
                loading = false
            }
        }
    }
}
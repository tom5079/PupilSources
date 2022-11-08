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

package xyz.quaver.pupil.sources.manatoki.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.quaver.pupil.sources.manatoki.HistoryDao
import xyz.quaver.pupil.sources.manatoki.ManatokiDatabase
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing
import xyz.quaver.pupil.sources.manatoki.networking.SearchParameters
import xyz.quaver.pupil.sources.manatoki.networking.SearchResult

class SearchViewModel(
    private val client: ManatokiHttpClient,
    private val historyDao: HistoryDao
) : ViewModel() {
    private var params by mutableStateOf(SearchParameters())

    val publish by derivedStateOf {
        params.publish
    }

    fun setPublish(publish: SearchParameters.Publish?) {
        params = params.copy(publish = publish)
    }

    val jaum by derivedStateOf {
        params.jaum
    }

    fun setJaum(jaum: SearchParameters.Jaum?) {
        params = params.copy(jaum = jaum)
    }

    val tag by derivedStateOf {
        params.tag
    }

    fun setTag(tag: Set<SearchParameters.Tag>) {
        params = params.copy(tag = tag)
    }

    val sst by derivedStateOf {
        params.sst
    }

    fun setSst(sst: SearchParameters.Sort?) {
        params = params.copy(sst = sst)
    }

    val sod by derivedStateOf {
        params.sod
    }

    fun setSod(sod: SearchParameters.Order) {
        params = params.copy(sod = sod)
    }

    val stx by derivedStateOf {
        params.stx ?: ""
    }

    fun setStx(stx: String) {
        params = params.copy(stx = stx.ifEmpty { null })
    }

    val artist by derivedStateOf {
        params.artist ?: ""
    }

    fun setArtist(artist: String) {
        params = params.copy(artist = artist.ifEmpty { null })
    }

    val page by derivedStateOf {
        params.page
    }

    fun setPage(page: Int) {
        params = params.copy(page = page)
    }

    private var searchResult by mutableStateOf<SearchResult?>(null)

    val result by derivedStateOf {
        searchResult?.result.orEmpty()
    }

    val maxPage by derivedStateOf {
        searchResult?.maxPage ?: 1
    }

    val availableArtists by derivedStateOf {
        searchResult?.availableArtists.orEmpty()
    }

    val loading by derivedStateOf {
        searchResult == null
    }

    var error by mutableStateOf(false)
        private set

    var mangaListing: MangaListing? by mutableStateOf(null)
        private set

    var recentItem: String? by mutableStateOf(null)
        private set

    private var searchJob: Job? = null
    suspend fun search(resetPage: Boolean = true) = coroutineScope {
        searchJob?.cancelAndJoin()

        if (resetPage) params = params.copy(page = 1)

        searchJob = launch {
            searchResult = client.search(params)
            error = searchResult == null
        }
    }

    fun loadList(itemID: String) {
        viewModelScope.launch {
            val listing = client.getItem(itemID)

            check(listing is MangaListing)

            mangaListing = listing
            recentItem = historyDao.getAll(listing.itemID).firstOrNull()
        }
    }
}
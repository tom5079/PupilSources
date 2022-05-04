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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.quaver.pupil.sources.manatoki.*
import xyz.quaver.pupil.sources.manatoki.networking.MainData
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing
import xyz.quaver.pupil.sources.manatoki.networking.Thumbnail

class MainViewModel(
    private val client: ManatokiHttpClient,
    private val database: ManatokiDatabase
) : ViewModel() {
    private var loadJob: Job? = null

    var mainData by mutableStateOf<MainData?>(null)
        private set

    var error by mutableStateOf(false)
        private set

    var recentManga by mutableStateOf<List<Thumbnail>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            database
                .historyDao()
                .getRecentManga()
                .collectLatest { mangaList ->
                    recentManga = mangaList.map { manga ->
                        val mangaListing = client.getItem(manga) as MangaListing
                        Thumbnail(mangaListing.itemID, mangaListing.title, mangaListing.thumbnail)
                    }
                }
        }
    }

    fun load() {
        viewModelScope.launch {
            loadJob?.cancelAndJoin()
            loadJob = launch {

                mainData = client.main()
            }
        }
    }
}
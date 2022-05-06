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

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.disklrucache.DiskLruCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.internal.closeQuietly
import xyz.quaver.pupil.sources.manatoki.*
import xyz.quaver.pupil.sources.manatoki.networking.MainData
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing
import xyz.quaver.pupil.sources.manatoki.networking.MangaThumbnail
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class MainViewModel(
    private val client: ManatokiHttpClient,
    private val database: ManatokiDatabase,
    private val cacheDirectory: File
) : ViewModel() {
    private var loadJob: Job? = null

    var mainData by mutableStateOf<MainData?>(null)
        private set

    var error by mutableStateOf(false)
        private set

    val recentManga = mutableStateListOf<MangaThumbnail>()

    private val diskLruCache by lazy {
        DiskLruCache.open(cacheDirectory, 1, 1, 100000)
    }

    init {
        viewModelScope.launch {
            database
                .historyDao()
                .getRecentManga()
                .distinctUntilChanged()
                .collectLatest { mangaList ->
                    recentManga.clear()
                    mangaList.forEach { itemID ->
                        val thumbnail = diskLruCache.get(itemID)?.getInputStream(0)?.let {
                            Json.decodeFromStream(it)
                        } ?: run {
                            val listing = client.getItem(itemID)

                            check(listing is MangaListing)

                            MangaThumbnail(listing.itemID, listing.title, listing.thumbnail).also { thumbnail ->
                                with (diskLruCache.edit(itemID)) {
                                    newOutputStream(0).use { outputStream ->
                                        Json.encodeToStream(thumbnail, outputStream)
                                    }

                                    commit()
                                }
                            }
                        }

                        recentManga.add(thumbnail)
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

    override fun onCleared() {
        diskLruCache.close()
    }
}
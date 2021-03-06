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
import kotlinx.coroutines.launch
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaThumbnail

class RecentViewModel(
    private val client: ManatokiHttpClient
): ViewModel() {
    var page by mutableStateOf(1)

    var error by mutableStateOf(false)
    var result by mutableStateOf<List<MangaThumbnail>?>(null)

    private var loadJob: Job? = null
    fun load() {
        viewModelScope.launch {
            loadJob?.cancelAndJoin()

            error = false
            loadJob = launch {
                result = client.recent(page)

                if (result == null) {
                    error = true
                }
            }
        }
    }
}
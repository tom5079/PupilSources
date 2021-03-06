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

package xyz.quaver.pupil.sources.base.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.ui.Scaffold
import kotlin.math.roundToInt

private enum class NavigationIconState {
    MENU,
    ARROW
}

open class SearchBaseViewModel<T> : ViewModel() {
    val searchResults = mutableStateListOf<T>()

    var sortModeIndex by mutableStateOf(0)
        private set

    var currentPage by mutableStateOf(1)

    var totalItems by mutableStateOf(0)

    var maxPage by mutableStateOf(0)

    val prevPageAvailable by derivedStateOf { currentPage > 1 }
    val nextPageAvailable by derivedStateOf { currentPage < maxPage }

    var query by mutableStateOf("")

    var loading by mutableStateOf(false)
    var exception by mutableStateOf<Throwable?>(null)

    //region UI
    var isFabVisible by  mutableStateOf(true)
    var searchBarOffset by mutableStateOf(0)
    //endregion
}

@Composable
fun <T> SearchBase(
    model: SearchBaseViewModel<T> = viewModel(),
    fabSubMenu: List<SubFabItem> = emptyList(),
    navigationIcon: @Composable () -> Unit = { },
    actions: @Composable RowScope.() -> Unit = { },
    onSearch: () -> Unit = { },
    content: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var isFabExpanded by remember { mutableStateOf(FloatingActionButtonState.COLLAPSED) }

    val statusBarsPaddingValues = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars)

    val searchBarDefaultOffset = statusBarsPaddingValues.calculateTopPadding() + 64.dp
    val searchBarDefaultOffsetPx = LocalDensity.current.run { searchBarDefaultOffset.roundToPx() }

    Scaffold(
        floatingActionButton = {
            MultipleFloatingActionButton(
                modifier = Modifier.navigationBarsPadding(),
                items = fabSubMenu,
                visible = model.isFabVisible,
                targetState = isFabExpanded,
                onStateChanged = {
                    isFabExpanded = it
                }
            )
        }
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding).fillMaxSize()) {
            OverscrollPager(
                currentPage = model.currentPage,
                prevPageAvailable = model.prevPageAvailable,
                nextPageAvailable = model.nextPageAvailable,
                onPageTurn = { model.currentPage = it; model.searchBarOffset = 0 },
                prevPageTurnIndicatorOffset = searchBarDefaultOffset,
                nextPageTurnIndicatorOffset = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars).calculateBottomPadding()
            ) {
                Box(
                    Modifier
                        .nestedScroll(object: NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (source == NestedScrollSource.Drag) {
                                    model.searchBarOffset =
                                        (model.searchBarOffset + available.y.roundToInt()).coerceIn(
                                            -searchBarDefaultOffsetPx,
                                            0
                                        )

                                    model.isFabVisible = available.y > 0f
                                }

                                return Offset.Zero
                            }
                        })
                ) {
                    content(PaddingValues(0.dp, searchBarDefaultOffset, 0.dp, rememberInsetsPaddingValues(
                        insets = LocalWindowInsets.current.navigationBars
                    ).calculateBottomPadding()))
                }
            }

            if (model.loading)
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            FloatingSearchBar(
                modifier = Modifier
                    .statusBarsPadding()
                    .offset(0.dp, LocalDensity.current.run { model.searchBarOffset.toDp() }),
                query = model.query,
                onQueryChange = { model.query = it },
                navigationIcon = navigationIcon,
                actions = actions,
                onSearch = { onSearch(); focusManager.clearFocus() }
            )
        }
    }
}
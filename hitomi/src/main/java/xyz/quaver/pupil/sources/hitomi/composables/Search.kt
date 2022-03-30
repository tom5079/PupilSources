package xyz.quaver.pupil.sources.hitomi.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import com.google.accompanist.insets.*
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.rememberViewModel
import xyz.quaver.pupil.proto.Settings
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.hitomi.HitomiDatabase
import xyz.quaver.pupil.sources.hitomi.HitomiSearchResultViewModel
import xyz.quaver.pupil.sources.hitomi.lib.SortOptions
import java.util.*
import com.google.accompanist.insets.ui.Scaffold
import xyz.quaver.pupil.sources.base.composables.*
import kotlin.math.roundToInt

@Composable
fun SearchLayout(
    model: HitomiSearchResultViewModel,
    fabSubMenu: List<SubFabItem>,
    actions: @Composable RowScope.() -> Unit = { },
    onSearch: () -> Unit = { },
    content: @Composable BoxScope.(contentPadding: PaddingValues) -> Unit
) {
    var isFabExpanded by remember { mutableStateOf(FloatingActionButtonState.COLLAPSED) }

    val statusBarsPaddingValues = rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars)

    val searchBarDefaultOffset = statusBarsPaddingValues.calculateTopPadding() + 64.dp
    val searchBarDefaultOffsetPx = LocalDensity.current.run { searchBarDefaultOffset.roundToPx() }

    HitomiSearchBar(
        query = model.query,
        onQueryChange = { model.query = it },
        topOffset = model.searchBarOffset,
        onTopOffsetChange = {
            model.searchBarOffset = it
        }
    ) {
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
                            .nestedScroll(object : NestedScrollConnection {
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
                        content(
                            PaddingValues(
                                0.dp, searchBarDefaultOffset, 0.dp, rememberInsetsPaddingValues(
                                    insets = LocalWindowInsets.current.navigationBars
                                ).calculateBottomPadding()
                            )
                        )
                    }
                }

                if (model.loading)
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun Search(navigateToReader: (itemID: String) -> Unit) {
    val model: HitomiSearchResultViewModel by rememberViewModel()
    val database: HitomiDatabase by rememberInstance()
    val favoritesDao = remember { database.favoritesDao() }
    val coroutineScope = rememberCoroutineScope()

    val settingsDataStore: DataStore<Settings> by rememberInstance()

    val favorites by favoritesDao.getAll().collectAsState(emptyList())
    val favoritesSet by derivedStateOf {
        Collections.unmodifiableSet(favorites.mapTo(mutableSetOf()) { it.item })
    }

    LaunchedEffect(Unit) {
        settingsDataStore.updateData {
            it.toBuilder()
                .setRecentSource("hitomi.la")
                .build()
        }
    }

    LaunchedEffect(model.currentPage, model.sortByPopularity) {
        model.search()
    }

    SearchLayout(
        model,
        fabSubMenu = listOf(
            SubFabItem("Jump to page") {
                withLocalResource {
                    Icon(painterResource(R.drawable.ic_jump), contentDescription = null)
                }
            },
            SubFabItem("Random") {
                Icon(Icons.Default.Shuffle, contentDescription = null)
            },
            SubFabItem("Open with Gallery ID") {
                withLocalResource {
                    Icon(painterResource(R.drawable.numeric), contentDescription = null)
                }
            }
        ),
        onSearch = { model.search() }
    ) { contentPadding ->
        LazyColumn(modifier = Modifier.systemBarsPadding(top = false, bottom = false), contentPadding = contentPadding) {
            items(model.searchResults) {
                DetailedSearchResult(
                    it,
                    favorites = favoritesSet,
                    onFavoriteToggle = {
                        coroutineScope.launch {
                            if (it in favoritesSet) favoritesDao.delete(it)
                            else favoritesDao.insert(it)
                        }
                    },
                ) { result ->
                    navigateToReader(result.id)
                }
            }
        }
    }
}
package xyz.quaver.pupil.sources.hitomi.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import java.util.*
import com.google.accompanist.insets.ui.Scaffold
import xyz.quaver.pupil.sources.base.composables.*
import xyz.quaver.pupil.sources.hitomi.lib.SortOptions
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

    var searchBarState by remember { mutableStateOf(HitomiSearchBarState.NORMAL) }

    HitomiSearchBar(
        query = model.query,
        onQueryChange = { model.query = it },
        topOffset = model.searchBarOffset,
        onTopOffsetChange = {
            model.searchBarOffset = it
        },
        state = searchBarState,
        onStateChange = { searchBarState = it },
        actions = {
            var expanded by remember { mutableStateOf(false) }

            IconButton(onClick = { searchBarState = HitomiSearchBarState.SETTINGS }) {
                withLocalResource {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Sort, contentDescription = null)
            }

            val onClick: (SortOptions) -> Unit = {
                expanded = false
                model.sortOption = it
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                @Composable
                fun SortOptionsMenuItem(text: String, currentSortOption: SortOptions, divider: Boolean = true) {
                    DropdownMenuItem(onClick = { onClick(currentSortOption) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text)
                            RadioButton(selected = model.sortOption == currentSortOption, onClick = { onClick(currentSortOption) })
                        }
                    }

                    if (divider) Divider()
                }

                SortOptionsMenuItem("Date Added", SortOptions.DATE)
                SortOptionsMenuItem("Popular: Today", SortOptions.POPULAR_TODAY)
                SortOptionsMenuItem("Popular: Week", SortOptions.POPULAR_WEEK)
                SortOptionsMenuItem("Popular: Month", SortOptions.POPULAR_MONTH)
                SortOptionsMenuItem("Popular: Year", SortOptions.POPULAR_YEAR, divider = false)
            }
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
            Box(
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize()) {
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

                val exception = model.exception

                when {
                    exception != null -> {
                        var exceptionDetailDialog by remember { mutableStateOf(false) }

                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text("(×_×)⌒☆", style = MaterialTheme.typography.h2)
                            }
                            Text("An Error occurred")
                            Row {
                                TextButton(onClick = { exceptionDetailDialog = true }) {
                                    Text("Detail")
                                }
                                TextButton(onClick = { onSearch() }) {
                                    Text("Retry")
                                }
                            }
                        }

                        if (exceptionDetailDialog)
                            Dialog(onDismissRequest = { exceptionDetailDialog = false }) {
                                Card {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                                            Text(
                                                exception::class.simpleName ?: "Exception",
                                                style = MaterialTheme.typography.h6
                                            )
                                        }

                                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                                            Text(
                                                exception.stackTraceToString(),
                                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                                            )
                                        }

                                        TextButton(
                                            modifier = Modifier.align(Alignment.End),
                                            onClick = { exceptionDetailDialog = false }
                                        ) {
                                            Text("OK")
                                        }
                                    }
                                }
                            }
                    }
                    model.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    model.searchResults.isEmpty() -> {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text("(ﾟoﾟ;;", style = MaterialTheme.typography.h2)
                            }
                            Text("No result")
                        }
                    }
                }
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

    LaunchedEffect(model.currentPage, model.sortOption) {
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
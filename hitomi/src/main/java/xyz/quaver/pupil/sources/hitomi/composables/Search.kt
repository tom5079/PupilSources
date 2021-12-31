package xyz.quaver.pupil.sources.hitomi.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.rememberViewModel
import xyz.quaver.pupil.proto.Settings
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.composables.SearchBase
import xyz.quaver.pupil.sources.composables.SubFabItem
import xyz.quaver.pupil.sources.hitomi.HitomiDatabase
import xyz.quaver.pupil.sources.hitomi.HitomiSearchResultViewModel
import xyz.quaver.pupil.sources.util.withLocalResource
import java.util.*

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun Search(navController: NavController) {
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

    SearchBase(
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
                    Icon(painterResource(R.drawable.ic_jump), contentDescription = null)
                }
            }
        ),
        actions = {
            var expanded by remember { mutableStateOf(false) }

            IconButton(onClick = { }) {
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

            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }

            val onClick: (Boolean?) -> Unit = {
                expanded = false

                it?.let {
                    model.sortByPopularity = it
                }
            }
            DropdownMenu(expanded, onDismissRequest = { onClick(null) }) {
                DropdownMenuItem(onClick = { onClick(false) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Newest")
                        RadioButton(selected = !model.sortByPopularity, onClick = { onClick(false) })
                    }
                }

                Divider()

                DropdownMenuItem(onClick = { onClick(true) }){
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Popular")
                        RadioButton(selected = model.sortByPopularity, onClick = { onClick(true) })
                    }
                }
            }
        },
        onSearch = { model.search() }
    ) { contentPadding ->
        LazyVerticalGrid(
            cells = GridCells.Adaptive(minSize = 500.dp),
            contentPadding = contentPadding
        ) {
            items(model.searchResults) {
                DetailedSearchResult(
                    it,
                    favorites = favoritesSet,
                    onFavoriteToggle = {
                        coroutineScope.launch {
                            if (it in favoritesSet) favoritesDao.delete(it)
                            else favoritesDao.insert(it)
                        }
                    }
                ) { result ->
                    navController.navigate("hitomi.la/reader/${result.itemID}")
                }
            }
        }
    }
}
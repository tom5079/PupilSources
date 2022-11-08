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

package xyz.quaver.pupil.sources.manatoki.composable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberViewModel
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.manatoki.networking.MangaThumbnail
import xyz.quaver.pupil.sources.manatoki.networking.TopWeekly
import xyz.quaver.pupil.sources.manatoki.viewmodel.MainViewModel

@Composable
fun MainRecentViewed(
    recentManga: List<MangaThumbnail>,
    navigateToRecent: () -> Unit,
    onItemClick: (String) -> Unit
) {
    if (recentManga.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "이어 보기",
                style = MaterialTheme.typography.h5
            )

            IconButton(onClick = navigateToRecent) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recentManga) { item ->
                Thumbnail(
                    item,
                    Modifier
                        .width(180.dp)
                        .aspectRatio(6 / 7f),
                    onClick = onItemClick
                )
            }
        }
    }
}

@Composable
fun MainRecentUpload(
    recentUpload: List<MangaThumbnail>,
    navigateToRecent: () -> Unit,
    onItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "최신화",
            style = MaterialTheme.typography.h5
        )

        IconButton(onClick = navigateToRecent) {
            Icon(
                Icons.Default.Add,
                contentDescription = null
            )
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(recentUpload) { item ->
            Thumbnail(
                item,
                Modifier
                    .width(180.dp)
                    .aspectRatio(6 / 7f),
                onClick = onItemClick
            )
        }
    }
}

@Composable
fun MainBoards() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoardButton("마나게시판", Color(0xFF007DB4))
            BoardButton("유머/가십", Color(0xFFF09614))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoardButton("역식자게시판", Color(0xFFA0C850))
            BoardButton("원본게시판", Color(0xFFFF4500))
        }
    }
}

@Composable
fun MainMangaList(
    mangaList: List<MangaThumbnail>,
    navigateToSearch: () -> Unit,
    onItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("만화 목록", style = MaterialTheme.typography.h5)

        IconButton(onClick = navigateToSearch) {
            Icon(
                Icons.Default.Add,
                contentDescription = null
            )
        }
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mangaList) { item ->
            Thumbnail(
                item,
                Modifier
                    .width(180.dp)
                    .aspectRatio(6f / 7),
                onClick = onItemClick
            )
        }
    }

}

@Composable
fun MainTopWeekly(
    topWeekly: List<TopWeekly>,
    onItemClick: (String) -> Unit
) {
    Text("주간 베스트", style = MaterialTheme.typography.h5)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topWeekly.forEachIndexed { index, item ->
            Card(modifier = Modifier.clickable { onItemClick(item.itemID) }) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF64C3F5))
                            .width(24.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        item.title,
                        modifier = Modifier
                            .weight(1f)
                            .padding(0.dp, 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        item.count,
                        color = Color(0xFFFF4500)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Main(
    navigateToReader: (String) -> Unit,
    navigateToRecent: () -> Unit,
    navigateToSearch: () -> Unit,
    navigateToSettings: () -> Unit
) {
    val model: MainViewModel by rememberViewModel()

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val mangaSheetState = rememberMangaListingBottomSheetState(model.mangaListing)

    val coroutineScope = rememberCoroutineScope()

    val data = model.mainData

    LaunchedEffect(Unit) {
        model.load()
    }

    LaunchedEffect(model.recentItem) {
        val recentItem = model.recentItem

        if (recentItem == null) {
            mangaSheetState.actionItem = MangaListingBottomSheetActionItem()
        } else {
            mangaSheetState.actionItem = MangaListingBottomSheetActionItem(
                MangaListingBottomSheetAction.RECENT,
                recentItem
            )

            mangaSheetState.highlightItem(recentItem)
        }

    }

    LaunchedEffect(model.readerID) {
        val readerID = model.readerID ?: return@LaunchedEffect

        if (sheetState.isVisible) {
            coroutineScope.launch {
                sheetState.snapTo(ModalBottomSheetValue.Hidden)
                navigateToReader(readerID)
            }
        }
    }

    BackHandler(sheetState.currentValue != ModalBottomSheetValue.Hidden) {
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
        sheetContent = {
            MangaListingBottomSheet(mangaSheetState) {
                coroutineScope.launch {
                    model.openItem(it)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("마나토끼")
                    },
                    actions = {
                        withLocalResource {
                            IconButton(onClick = navigateToSettings) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    contentPadding = WindowInsets.statusBars.asPaddingValues()
                )
            },
            bottomBar = {
                Spacer(Modifier.navigationBarsPadding().fillMaxWidth())
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = navigateToSearch
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                }
            }
        ) { contentPadding ->
            Box(
                Modifier
                    .padding(contentPadding)
                    .fillMaxSize()) {
                if (data == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    Column(
                        Modifier
                            .padding(8.dp, 0.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val openItem: (String) -> Unit = {
                            model.openItem(it)
                            coroutineScope.launch {
                                sheetState.animateTo(ModalBottomSheetValue.Expanded)
                            }
                        }
                        MainRecentViewed(model.recentManga, navigateToRecent, openItem)

                        MainRecentUpload(data.recentUpload, navigateToRecent, openItem)

                        MainBoards()

                        MainMangaList(data.mangaList, navigateToSearch, openItem)

                        MainTopWeekly(data.topWeekly, openItem)
                    }
                }
            }
        }
    }
}

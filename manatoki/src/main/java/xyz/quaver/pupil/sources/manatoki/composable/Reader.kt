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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.rememberViewModel
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.composables.ReaderBase
import xyz.quaver.pupil.sources.base.composables.ReaderBaseViewModel
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.manatoki.ManatokiDatabase
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing
import xyz.quaver.pupil.sources.manatoki.networking.ReaderInfo

private val imageUserAgent = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Mobile Safari/537.36"

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun Reader(
    itemID: String,
    navigateToReader: (String) -> Unit,
    navigateUp: () -> Unit
) {
    val model: ReaderBaseViewModel by rememberViewModel()

    val client: ManatokiHttpClient by rememberInstance()

    val database: ManatokiDatabase by rememberInstance()
    val favoriteDao = remember { database.favoriteDao() }
    val bookmarkDao = remember { database.bookmarkDao() }
    val historyDao = remember { database.historyDao() }

    val coroutineScope = rememberCoroutineScope()

    var readerInfo: ReaderInfo? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        val reader = client.getItem(itemID)

        check(reader is ReaderInfo)

        coroutineScope.launch {
            historyDao.insert(reader.itemID, reader.listingItemID, 1)
        }
        readerInfo = reader
        model.load(reader.urls) {
            set("User-Agent", imageUserAgent)
        }
    }

    val isFavorite by favoriteDao.contains(itemID).collectAsState(false)

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val navigationBarsPadding = LocalDensity.current.run {
        rememberInsetsPaddingValues(
            LocalWindowInsets.current.navigationBars
        ).calculateBottomPadding().toPx()
    }

    val readerListState = rememberLazyListState()

    LaunchedEffect(readerListState.firstVisibleItemIndex) {
        readerInfo?.let { readerInfo ->
            historyDao.insert(
                readerInfo.itemID,
                readerInfo.listingItemID,
                readerListState.firstVisibleItemIndex
            )
        }
    }

    var scrollDirection by remember { mutableStateOf(0f) }

    BackHandler {
        when {
            sheetState.isVisible -> coroutineScope.launch { sheetState.hide() }
            model.fullscreen -> model.fullscreen = false
            else -> navigateUp()
        }
    }

    var mangaListing: MangaListing? by rememberSaveable { mutableStateOf(null) }
    val mangaListingListState = rememberLazyListState()
    var mangaListingListSize: Size? by remember { mutableStateOf(null) }
    val mangaListingRippleInteractionSource = remember { MutableInteractionSource() }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
        sheetContent = {
            MangaListingBottomSheet(
                mangaListing,
                currentItemID = itemID,
                onListSize = { mangaListingListSize = it },
                rippleInteractionSource = mapOf(itemID to mangaListingRippleInteractionSource),
                listState = mangaListingListState,
                nextItem = readerInfo?.nextItemID
            ) { item ->
                navigateToReader(item)
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!model.fullscreen)
                    TopAppBar(
                        title = {
                            Text(
                                readerInfo?.title ?: "???????????? ???...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = navigateUp) {
                                Icon(
                                    Icons.Default.NavigateBefore,
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {
                            IconButton({ }) {
                                withLocalResource {
                                    Image(
                                        painter = painterResource(R.mipmap.ic_launcher),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if (isFavorite) favoriteDao.delete(itemID)
                                    else            favoriteDao.insert(itemID)
                                }
                            }) {
                                Icon(
                                    if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = Orange500
                                )
                            }
                        },
                        contentPadding = rememberInsetsPaddingValues(
                            LocalWindowInsets.current.statusBars,
                            applyBottom = false
                        )
                    )
            },
            floatingActionButton = {
                val showNextButton by derivedStateOf {
                    (readerInfo?.nextItemID?.isNotEmpty() == true) && with (readerListState.layoutInfo) {
                        visibleItemsInfo.lastOrNull()?.index == totalItemsCount-1
                    }
                }
                val scale by animateFloatAsState(if (!showNextButton && (model.fullscreen || scrollDirection < 0f)) 0f else 1f)

                if (scale > 0f)
                    FloatingActionButton(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .scale(scale),
                        onClick = {
                            readerInfo?.let { readerInfo ->
                                if (showNextButton) {
                                    if (readerInfo.nextItemID != null) {
                                        navigateToReader(readerInfo.nextItemID)
                                    }
                                } else {
                                    coroutineScope.launch {
                                        sheetState.animateTo(ModalBottomSheetValue.Expanded)
                                    }

                                    coroutineScope.launch {
                                        if (mangaListing?.itemID != readerInfo.listingItemID) {
                                            val listing = client.getItem(readerInfo.listingItemID)

                                            check(listing is MangaListing)
                                            mangaListing = listing

                                            coroutineScope.launch {
                                                while (mangaListingListState.layoutInfo.totalItemsCount != listing.entries.size) {
                                                    delay(100)
                                                }

                                                val targetIndex =
                                                    listing.entries.indexOfFirst { it.itemID == itemID }

                                                mangaListingListState.scrollToItem(targetIndex)

                                                mangaListingListSize?.let { sheetSize ->
                                                    val targetItem =
                                                        mangaListingListState.layoutInfo.visibleItemsInfo.first {
                                                            it.key == itemID
                                                        }

                                                    if (targetItem.offset == 0) {
                                                        mangaListingListState.animateScrollBy(
                                                            -(sheetSize.height - navigationBarsPadding - targetItem.size)
                                                        )
                                                    }

                                                    delay(200)

                                                    with(mangaListingRippleInteractionSource) {
                                                        val interaction =
                                                            PressInteraction.Press(
                                                                Offset(
                                                                    sheetSize.width / 2,
                                                                    targetItem.size / 2f
                                                                )
                                                            )


                                                        emit(interaction)
                                                        emit(
                                                            PressInteraction.Release(
                                                                interaction
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (showNextButton) Icons.Default.NavigateNext else Icons.Default.List,
                            contentDescription = null
                        )
                    }
            }
        ) { contentPadding ->
            ReaderBase(
                Modifier.padding(contentPadding),
                model = model,
                listState = readerListState,
                onScroll = { scrollDirection = it }
            )
        }
    }
}

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberViewModel
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.composables.ReaderBase
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.manatoki.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun Reader(
    itemID: String,
    navigateToReader: (String) -> Unit,
    navigateUp: () -> Unit
) {
    val model: ReaderViewModel by rememberViewModel()

    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val readerListState = rememberLazyListState()

    var scrollDirection by remember { mutableStateOf(0f) }

    val mangaSheetState = rememberMangaListingBottomSheetState(
        model.mangaListing,
        currentItem = itemID,
        actionItem = MangaListingBottomSheetActionItem(
            MangaListingBottomSheetAction.NEXT,
            model.readerInfo?.nextItemID
        )
    )

    LaunchedEffect(readerListState.firstVisibleItemIndex) {
        model.updateHistory(readerListState.firstVisibleItemIndex)
    }

    LaunchedEffect(Unit) {
        model.load(itemID).join()
    }

    LaunchedEffect(model.mangaListing) {
        mangaSheetState.highlightItem(itemID)
    }

    BackHandler {
        when {
            sheetState.isVisible -> coroutineScope.launch { sheetState.hide() }
            model.fullscreen -> model.fullscreen = false
            else -> navigateUp()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
        sheetContent = {
            MangaListingBottomSheet(
                mangaSheetState,
                navigateToReader
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (!model.fullscreen)
                    TopAppBar(
                        title = {
                            Text(
                                model.readerInfo?.title ?: "불러오는 중...",
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
                                model.toggleFavorite()
                            }) {
                                Icon(
                                    if (model.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = Orange500
                                )
                            }
                        },
                        contentPadding = WindowInsets.statusBars.asPaddingValues()
                    )
            },
            bottomBar = {
                Spacer(Modifier.navigationBarsPadding().fillMaxWidth())
            },
            floatingActionButton = {
                val showNextButton by remember {
                    derivedStateOf {
                        (model.readerInfo?.nextItemID?.isNotEmpty() == true) && with(readerListState.layoutInfo) {
                            visibleItemsInfo.lastOrNull()?.index == totalItemsCount - 1
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showNextButton || (!model.fullscreen && scrollDirection >= 0f),
                    enter = scaleIn(spring()),
                    exit = scaleOut(spring())
                ) {
                    FloatingActionButton(
                        onClick = onClick@{
                            val readerInfo = model.readerInfo ?: return@onClick

                            if (showNextButton) {
                                navigateToReader(readerInfo.nextItemID!!)
                            } else {
                                coroutineScope.launch {
                                    sheetState.animateTo(ModalBottomSheetValue.Expanded)
                                }

                                model.loadList(readerInfo.listingItemID)
                            }
                        }
                    ) {
                        Icon(
                            if (showNextButton) Icons.Default.NavigateNext else Icons.Default.List,
                            contentDescription = null
                        )
                    }
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

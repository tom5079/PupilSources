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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import kotlinx.coroutines.delay
import xyz.quaver.pupil.sources.manatoki.networking.MangaListing

private val FabSpacing = 8.dp
private val HeightPercentage = 75 // take 75% of the available space
private enum class MangaListingBottomSheetLayoutContent { Top, Bottom, Fab }

@Composable
fun MangaListingBottomSheetLayout(
    floatingActionButton: @Composable () -> Unit,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight * HeightPercentage / 100

        layout(layoutWidth, layoutHeight) {
            val topPlaceables = subcompose(MangaListingBottomSheetLayoutContent.Top, top).map {
                it.measure(constraints)
            }

            val topPlaceableHeight = topPlaceables.maxOfOrNull { it.height } ?: 0

            val bottomConstraints = constraints.copy(
                maxHeight = layoutHeight - topPlaceableHeight
            )

            val bottomPlaceables = subcompose(MangaListingBottomSheetLayoutContent.Bottom, bottom).map {
                it.measure(bottomConstraints)
            }

            val fabPlaceables = subcompose(MangaListingBottomSheetLayoutContent.Fab, floatingActionButton).mapNotNull {
                it.measure(constraints).takeIf { it.height != 0 && it.width != 0 }
            }

            topPlaceables.forEach { it.place(0, 0) }
            bottomPlaceables.forEach { it.place(0, topPlaceableHeight) }

            if (fabPlaceables.isNotEmpty()) {
                val fabWidth = fabPlaceables.maxOf { it.width }
                val fabHeight = fabPlaceables.maxOf { it.height }

                fabPlaceables.forEach {
                    it.place(
                        layoutWidth - fabWidth - FabSpacing.roundToPx(),
                        topPlaceableHeight - fabHeight / 2
                    )
                }
            }
        }
    }
}

@Composable
fun rememberMangaListingBottomSheetState(
    listing: MangaListing?,
    currentItem: String? = null,
    actionItem: MangaListingBottomSheetActionItem? = null
): MangaListingBottomSheetState {
    val listState = rememberLazyListState()
    val navigationBarsPadding = with (LocalDensity.current) {
        rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
            .calculateBottomPadding()
            .toPx()
    }

    return remember(listing) {
        MangaListingBottomSheetState(listing, listState, navigationBarsPadding).also {
            if (currentItem != null) it.currentItem = currentItem
            if (actionItem != null) it.actionItem = actionItem
        }
    }
}

enum class MangaListingBottomSheetAction {
    FIRST, RECENT, NEXT
}

data class MangaListingBottomSheetActionItem(
    val action: MangaListingBottomSheetAction = MangaListingBottomSheetAction.FIRST,
    val item: String? = null
)

class MangaListingBottomSheetState(
    val listing: MangaListing?,
    val listState: LazyListState,
    private val navigationBarsPadding: Float
) {
    val rippleInteractionSource = mutableStateMapOf<String, MutableInteractionSource>()

    var listSize: Size? by mutableStateOf(null)

    var actionItem by mutableStateOf(MangaListingBottomSheetActionItem())
    var currentItem by mutableStateOf("")

    suspend fun highlightItem(itemID: String) {
        listing ?: return

        // Prepare InteractionSource
        val interactionSource = rippleInteractionSource.getOrPut(itemID) {
            MutableInteractionSource()
        }

        // Wait for the list to fully populate
        while (listState.layoutInfo.totalItemsCount != listing.entries.size) {
            delay(100)
        }

        // Rough scroll to the item
        val targetIndex = listing.entries.indexOfFirst { it.itemID == itemID }

        listState.scrollToItem(targetIndex)

        // align the item to the top of the list
        val listSize = listSize ?: return

        val targetItem = listState.layoutInfo.visibleItemsInfo.first {
            it.key == itemID
        }

        if (targetItem.offset == 0)
            listState.animateScrollBy(
                -(listSize.height - navigationBarsPadding - targetItem.size)
            )

        // Wait for the animation to finish
        delay(200)

        // Show ripple effect
        with (interactionSource) {
            val interaction = PressInteraction.Press(Offset(
                listSize.width / 2f,
                targetItem.size / 2f
            ))

            emit(interaction)
            emit(PressInteraction.Release(interaction))
        }
    }
}

@Composable
fun MangaListingBottomSheet(
    state: MangaListingBottomSheetState,
    onOpenItem: (String) -> Unit = { },
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.listing == null)
            CircularProgressIndicator(
                Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.Center)
            )
        else {
            val isItemAvailable = state.listing.entries.any { it.itemID == state.actionItem.item }

            MangaListingBottomSheetLayout(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = {
                            Text(
                                when {
                                    state.actionItem.action == MangaListingBottomSheetAction.RECENT && isItemAvailable -> "이어보기"
                                    state.actionItem.action == MangaListingBottomSheetAction.NEXT && isItemAvailable -> "다음화보기"
                                    else -> "첫화보기"
                                }
                            )
                        },
                        onClick = {
                            when {
                                state.actionItem.action == MangaListingBottomSheetAction.RECENT && isItemAvailable ->
                                    onOpenItem(state.actionItem.item!!)
                                state.actionItem.action == MangaListingBottomSheetAction.NEXT && isItemAvailable ->
                                    onOpenItem(state.actionItem.item!!)
                                else -> state.listing.entries.lastOrNull()
                                    ?.let { onOpenItem(it.itemID) }
                            }
                        }
                    )
                },
                top = {
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .background(MaterialTheme.colors.primary)
                            .padding(0.dp, 0.dp, 0.dp, 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val painter = rememberAsyncImagePainter(state.listing.thumbnail)

                        Box(Modifier.fillMaxHeight()) {
                            Image(
                                modifier = Modifier
                                    .width(150.dp)
                                    .aspectRatio(
                                        with(painter.intrinsicSize) { if (this == Size.Unspecified) 1f else width / height }
                                    )
                                    .align(Alignment.Center),
                                painter = painter,
                                contentDescription = null
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(0.dp, 8.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                state.listing.title,
                                style = MaterialTheme.typography.h5,
                                modifier = Modifier.weight(1f)
                            )

                            CompositionLocalProvider(LocalContentAlpha provides 0.7f) {
                                Text("작가: ${state.listing.author}")

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("분류: ")

                                    CompositionLocalProvider(LocalContentAlpha provides 1f) {
                                        FlowRow(
                                            modifier = Modifier.weight(1f),
                                            mainAxisSpacing = 8.dp
                                        ) {
                                            state.listing.tags.forEach {
                                                Card(
                                                    elevation = 4.dp,
                                                    backgroundColor = Color.White
                                                ) {
                                                    Text(
                                                        it,
                                                        style = MaterialTheme.typography.caption,
                                                        modifier = Modifier.padding(4.dp),
                                                        color = Color.Black
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text("발행구분: ${state.listing.type}")
                            }
                        }
                    }
                },
                bottom = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned {
                                state.listSize = it.size.toSize()
                            },
                        state = state.listState,
                        contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
                    ) {
                        items(state.listing.entries, key = { it.itemID }) { entry ->
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        onOpenItem(entry.itemID)
                                    }
                                    .then(
                                        state.rippleInteractionSource[entry.itemID]?.let {
                                            Modifier.indication(it, rememberRipple())
                                        } ?: Modifier
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (entry.itemID == state.currentItem)
                                    Icon(
                                        Icons.Default.ArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.secondary
                                    )

                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.weight(1f)
                                )

                                Text("★ ${entry.starRating}")
                            }
                            Divider()
                        }
                    }
                }
            )
        }
    }
}
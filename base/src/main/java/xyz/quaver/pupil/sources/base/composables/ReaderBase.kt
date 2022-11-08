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

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.kodein.di.compose.rememberInstance
import xyz.quaver.graphics.subsampledimage.SubSampledImage
import xyz.quaver.graphics.subsampledimage.doubleClickCycleZoom
import xyz.quaver.graphics.subsampledimage.rememberSubSampledImageState
import xyz.quaver.io.FileX
import xyz.quaver.pupil.proto.ReaderOptions
import xyz.quaver.pupil.proto.Settings
import xyz.quaver.pupil.sources.base.util.doubleClickCycleZoom
import xyz.quaver.pupil.sources.base.util.rememberFileXImageSource
import xyz.quaver.pupil.sources.core.util.LocalActivity
import java.io.Reader
import kotlin.math.sign

private var _singleImage: ImageVector? = null
val SingleImage: ImageVector
    get() {
        if (_singleImage != null) {
            return _singleImage!!
        }

        _singleImage = materialIcon(name = "ReaderBase.SingleImage") {
            materialPath {
                moveTo(17.0f, 3.0f)
                lineTo(7.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(10.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(19.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(17.0f, 19.0f)
                lineTo(7.0f, 19.0f)
                lineTo(7.0f, 5.0f)
                horizontalLineToRelative(10.0f)
                verticalLineToRelative(14.0f)
                close()
            }
        }

        return _singleImage!!
    }

private var _doubleImage: ImageVector? = null
val DoubleImage: ImageVector
    get() {
        if (_doubleImage != null) {
            return _doubleImage!!
        }

        _doubleImage = materialIcon(name = "ReaderBase.DoubleImage") {
            materialPath {
                moveTo(9.0f, 3.0f)
                lineTo(2.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(7.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(11.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(9.0f, 19.0f)
                lineTo(2.0f, 19.0f)
                lineTo(2.0f, 5.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(14.0f)
                close()
                moveTo(21.0f, 3.0f)
                lineTo(14.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(7.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(23.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(21.0f, 19.0f)
                lineTo(14.0f, 19.0f)
                lineTo(14.0f, 5.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(14.0f)
                close()
            }
        }

        return _doubleImage!!
    }

open class ReaderBaseViewModel : ViewModel() {
    var fullscreen by mutableStateOf(false)

    var error by mutableStateOf(false)

    open var currentIndex by mutableStateOf(0)

    var items by mutableStateOf<List<ReaderItem>>(emptyList())
        protected set
}

val ReaderOptions.Orientation.isVertical: Boolean
    get() =
        this == ReaderOptions.Orientation.VERTICAL_DOWN ||
        this == ReaderOptions.Orientation.VERTICAL_UP
val ReaderOptions.Orientation.isReverse: Boolean
    get() =
        this == ReaderOptions.Orientation.VERTICAL_UP ||
        this == ReaderOptions.Orientation.HORIZONTAL_LEFT

@Composable
fun ReaderOptionsSheet(readerOptions: ReaderOptions, onOptionsChange: (ReaderOptions.Builder.() -> Unit) -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.h6) {
        Column(Modifier.padding(16.dp, 0.dp)) {
            val layout = readerOptions.layout
            val snap = readerOptions.snap
            val orientation = readerOptions.orientation
            val padding = readerOptions.padding

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Layout")

                Row {
                    listOf(
                        ReaderOptions.Layout.SINGLE_PAGE to SingleImage,
                        ReaderOptions.Layout.DOUBLE_PAGE to DoubleImage,
                        ReaderOptions.Layout.AUTO to Icons.Default.AutoFixHigh
                    ).forEach { (option, icon) ->
                        IconButton(onClick = {
                            onOptionsChange {
                                setLayout(option)
                            }
                        }) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint =
                                if (layout == option) MaterialTheme.colors.secondary
                                else LocalContentColor.current
                            )
                        }
                    }
                }
            }

            val infiniteTransition = rememberInfiniteTransition()

            val isReverse = orientation.isReverse
            val isVertical = orientation.isVertical

            val animationOrientation = if (isReverse) -1f else 1f
            val animationSpacing by animateFloatAsState(if (padding) 48f else 32f)
            val animationOffset by infiniteTransition.animateFloat(
                initialValue = animationOrientation * (if (snap) 0f else animationSpacing/2),
                targetValue = animationOrientation * (if (snap) -animationSpacing else -animationSpacing/2),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        easing = if(snap) FastOutSlowInEasing else LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
            val animationRotation by animateFloatAsState(if (isVertical) 90f else 0f)

            @Suppress("NAME_SHADOWING")
            val setOrientation: (Boolean, Boolean) -> Unit = { isVertical, isReverse ->
                val orientation = when {
                    isVertical && !isReverse -> ReaderOptions.Orientation.VERTICAL_DOWN
                    isVertical && isReverse -> ReaderOptions.Orientation.VERTICAL_UP
                    !isVertical && !isReverse -> ReaderOptions.Orientation.HORIZONTAL_RIGHT
                    !isVertical && isReverse -> ReaderOptions.Orientation.HORIZONTAL_LEFT
                    else -> error("Invalid value")
                }

                onOptionsChange {
                    setOrientation(orientation)
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clipToBounds()
                    .rotate(animationRotation)
                    .align(Alignment.CenterHorizontally)
            ) {
                for (i in 0..4)
                    Icon(
                        SingleImage,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterStart)
                            .offset((animationOffset + animationSpacing * (i - 2)).dp, 0.dp)
                    )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Orientation")

                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.caption) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("H")
                        Switch(checked = isVertical, onCheckedChange = {
                            setOrientation(!isVertical, isReverse)
                        })
                        Text("V")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Reverse")
                Switch(checked = isReverse, onCheckedChange = {
                    setOrientation(isVertical, !isReverse)
                })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Snap")

                Switch(checked = snap, onCheckedChange = {
                    onOptionsChange {
                        setSnap(!snap)
                    }
                })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Padding")

                Switch(checked = padding, onCheckedChange = {
                    onOptionsChange {
                        setPadding(!padding)
                    }
                })
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp))
        }
    }
}

@Composable
fun BoxScope.ReaderLazyList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    orientation: ReaderOptions.Orientation,
    onScroll: (direction: Float) -> Unit,
    content: LazyListScope.() -> Unit
) {
    val isReverse = orientation.isReverse

    val nestedScrollConnection = remember(orientation) { object: NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            onScroll(
                when (orientation) {
                    ReaderOptions.Orientation.VERTICAL_DOWN -> available.y.sign
                    ReaderOptions.Orientation.VERTICAL_UP -> -(available.y.sign)
                    ReaderOptions.Orientation.HORIZONTAL_RIGHT -> available.x.sign
                    ReaderOptions.Orientation.HORIZONTAL_LEFT -> -(available.x.sign)
                }
            )

            return Offset.Zero
        }
    } }

    when (orientation) {
        ReaderOptions.Orientation.VERTICAL_DOWN,
        ReaderOptions.Orientation.VERTICAL_UP ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart)
                    .nestedScroll(nestedScrollConnection),
                state = state,
                contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars),
                reverseLayout = isReverse,
                content = content
            )
        ReaderOptions.Orientation.HORIZONTAL_RIGHT,
        ReaderOptions.Orientation.HORIZONTAL_LEFT ->
            LazyRow(
                modifier = modifier
                    .fillMaxSize()
                    .align(Alignment.CenterStart)
                    .nestedScroll(nestedScrollConnection),
                state = state,
                reverseLayout = isReverse,
                content = content
            )
    }
}
/**
 * @param image Image file to display. If null, show progress.
 * @param progress value between 0.0 and 1.0.
 */
data class ReaderItem(
    val image: FileX?,
    val progress: StateFlow<Float>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
/**
 * Shows Image or Progress bar.
 * if image is null and progress is not between 0 and 1, show broken image icon
 * @param index one-based index of the image for progress bar
 */
fun ReaderElement(
    image: FileX?,
    progress: Float,
    index: Int,
    options: ReaderOptions,
    modifier: Modifier = Modifier,
    fullscreen: Boolean,
    onRequestFullscreen: () -> Unit = { },
    onTap: () -> Unit = { },
    onError: () -> Unit = { }
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            progress in 0f .. 1f -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(progress)
                    Text(index.toString())
                }
            }
            image != null -> {
                val state = rememberSubSampledImageState().apply {
                    isGestureEnabled = true
                }

                val imageSize = state.imageSize

                val imageModifier = when {
                    options.padding -> Modifier
                        .fillMaxSize()
                    imageSize == null -> Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                    options.orientation.isVertical -> Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageSize.width / imageSize.height)
                    else -> Modifier
                        .fillMaxHeight()
                        .aspectRatio(imageSize.width / imageSize.height)
                }

                SubSampledImage(
                    imageModifier.then(
                        if (fullscreen)
                            Modifier.doubleClickCycleZoom(state, 2f) { onTap() }
                        else
                            Modifier.combinedClickable(
                                onLongClick = { /* TODO */ },
                                onClick = onRequestFullscreen
                            )
                    ),
                    imageSource = rememberFileXImageSource(image),
                    state = state,
                    onError = { onError() }
                )
            }
            else -> {
                Icon(Icons.Default.BrokenImage, contentDescription = null)
            }
        }
    }
}

fun LazyListScope.ReaderLazyListContent(
    items: List<ReaderItem>,
    options: ReaderOptions,
    listSize: DpSize,
    fullscreen: Boolean,
    onRequestFullscreen: () -> Unit,
    onNextImage: () -> Unit
) {
    val containerModifier = when {
        options.padding -> Modifier
            .size(listSize)

        options.orientation.isVertical -> Modifier
            .fillMaxWidth()
            .heightIn(max = listSize.height)

        else -> Modifier
            .fillMaxHeight()
            .widthIn(max = listSize.width)
    }

    when (options.layout!!) {
        ReaderOptions.Layout.SINGLE_PAGE -> {
            itemsIndexed(items) { index, item ->
                val progress by item.progress.collectAsState(0f)

                ReaderElement(
                    item.image,
                    progress,
                    index+1,
                    options,
                    containerModifier,
                    fullscreen,
                    onRequestFullscreen,
                    onNextImage
                )
            }
        }
        ReaderOptions.Layout.DOUBLE_PAGE -> {

        }
        ReaderOptions.Layout.AUTO -> {

        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReaderBase(
    modifier: Modifier = Modifier,
    model: ReaderBaseViewModel,
    listState: LazyListState = rememberLazyListState(),
    onScroll: (direction: Float) -> Unit = { }
) {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current

    val settingsDataStore: DataStore<Settings> by rememberInstance()

    val coroutineScope = rememberCoroutineScope()

    val scaffoldState = rememberScaffoldState()

    var scrollDirection by remember { mutableStateOf(0f) }
    val handleOffset by animateDpAsState(if (model.fullscreen || scrollDirection < 0f) (-36).dp else 0.dp)

    val mainReaderOptions by remember {
        settingsDataStore.data.map { it.mainReaderOption }
    }.collectAsState(ReaderOptions.getDefaultInstance())

    LaunchedEffect(scrollDirection) {
        onScroll(scrollDirection)
    }

    LaunchedEffect(model.fullscreen) {
        val window = activity.window

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (model.fullscreen) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else
            insetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    if (model.error)
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                "Failed to find gallery",
                duration = SnackbarDuration.Indefinite
            )
        }

    Box(modifier) {
        ModalTopSheetLayout(
            modifier = Modifier.offset(0.dp, handleOffset),
            drawerContent = {
                ReaderOptionsSheet(mainReaderOptions) { readerOptionsBlock ->
                    coroutineScope.launch {
                        settingsDataStore.updateData {
                            it.toBuilder().setMainReaderOption(
                                mainReaderOptions.toBuilder().apply(readerOptionsBlock).build()
                            ).build()
                        }
                    }
                }
            }
        ) {
            val density = LocalDensity.current
            var listSize: Size? by remember { mutableStateOf(null) }
            val listDpSize: DpSize by remember {
                derivedStateOf {
                    listSize?.let { listSize ->
                        with(density) {
                            DpSize(listSize.width.toDp(), listSize.height.toDp())
                        }
                    } ?: DpSize.Zero
                }
            }

            val nestedScrollConnection = remember { object: NestedScrollConnection {
                override suspend fun onPreFling(available: Velocity): Velocity {
                    return if (
                        mainReaderOptions.snap &&
                        listState.layoutInfo.visibleItemsInfo.size > 1
                    ) {
                        val velocity = when (mainReaderOptions.orientation!!) {
                            ReaderOptions.Orientation.VERTICAL_DOWN -> available.y
                            ReaderOptions.Orientation.VERTICAL_UP -> -(available.y)
                            ReaderOptions.Orientation.HORIZONTAL_RIGHT -> available.x
                            ReaderOptions.Orientation.HORIZONTAL_LEFT -> -(available.x)
                        }

                        val index = listState.firstVisibleItemIndex

                        coroutineScope.launch {
                            when {
                                velocity < 0f -> listState.animateScrollToItem(index+1)
                                else -> listState.animateScrollToItem(index)
                            }
                        }

                        available
                    } else Velocity.Zero

                }
            } }

            ReaderLazyList(
                Modifier
                    .onGloballyPositioned { listSize = it.size.toSize() }
                    .nestedScroll(nestedScrollConnection),
                listState,
                mainReaderOptions.orientation,
                onScroll = { scrollDirection = it },
            ) {
                ReaderLazyListContent(
                    model.items,
                    mainReaderOptions,
                    listDpSize,
                    model.fullscreen,
                    onRequestFullscreen = {
                        model.fullscreen = true
                    },
                    onNextImage = {
                        coroutineScope.launch {
                            listState.scrollToItem(listState.firstVisibleItemIndex + 1)
                        }
                    }
                )
            }

            if (model.items.any { it.progress.value.isFinite() })
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    progress = model.items.fold(0f) { acc, item ->
                        val progress = item.progress.value
                        acc + if (progress.isInfinite()) 1f else progress
                    } / model.items.size,
                    color = MaterialTheme.colors.secondary
                )

            SnackbarHost(
                scaffoldState.snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
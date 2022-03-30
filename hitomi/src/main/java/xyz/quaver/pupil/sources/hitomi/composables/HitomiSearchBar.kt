package xyz.quaver.pupil.sources.hitomi.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.systemBarsPadding
import xyz.quaver.pupil.sources.base.theme.Blue700
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.theme.Pink600

/*
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

            val onClick: (SortOptions) -> Unit = {
                expanded = false
                model.sortByPopularity = it
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                @Composable
                fun SortOptionsMenuItem(text: String, sortOption: SortOptions, divider: Boolean = true) {
                    DropdownMenuItem(onClick = { onClick(sortOption) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(text)
                            RadioButton(selected = model.sortByPopularity == sortOption, onClick = { onClick(sortOption) })
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
*/

enum class HitomiSearchBarState {
    NORMAL,
    SEARCH,
    SETTINGS
}

@Composable
private fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val dismissModifier = if (visible) {
            Modifier.pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

@Composable
fun TagChip(
    tag: String,
    onRemove: () -> Unit
) {
    val (area, tag) = tag.split(':', limit = 2).let {
        if (it.size == 1) listOf("", it[0]) else it
    }
    val isFavorite = false

    val surfaceColor = if (isFavorite) Orange500 else when (area) {
        "male" -> Blue700
        "female" -> Pink600
        else -> MaterialTheme.colors.background
    }

    val contentColor = contentColorFor(surfaceColor)

    val icon = when (area) {
        "male" -> Icons.Filled.Male
        "female" -> Icons.Filled.Female
        else -> null
    }

    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        elevation = 2.dp
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null)
                    Icon(
                        icon,
                        contentDescription = "icon",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    )
                else
                    Box(Modifier.size(16.dp))

                Text(tag)

                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputChip(
    tag: String,
    onTagChange: (String) -> Unit,
    onEnter: () -> Unit,
    onRemove: () -> Unit
) {
    val (area, tag) = tag.split(':', limit = 2).let {
        if (it.size == 1) listOf("", it[0]) else it
    }
    val isFavorite = false

    val surfaceColor = if (isFavorite) Orange500 else when (area) {
        "male" -> Blue700
        "female" -> Pink600
        else -> MaterialTheme.colors.background
    }

    val contentColor = contentColorFor(surfaceColor)

    val icon = when (area) {
        "male" -> Icons.Filled.Male
        "female" -> Icons.Filled.Female
        else -> null
    }

    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        elevation = 2.dp
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null)
                    Icon(
                        icon,
                        contentDescription = "icon",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    )
                else
                    Box(Modifier.size(16.dp))

                BasicTextField(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .onPreviewKeyEvent { event ->
                            if (
                                tag.isEmpty() &&
                                event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace
                            ) {
                                if (area.isEmpty()) onRemove() else onTagChange(area)
                                return@onPreviewKeyEvent false
                            }

                            if (
                                event.type == KeyEventType.KeyUp &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onEnter()
                                return@onPreviewKeyEvent true
                            }

                            false
                        },
                    value = tag,
                    onValueChange = {
                        onTagChange(buildString{
                            if (area.isNotEmpty()) {
                                append(area)
                                append(':')
                            }

                            append(it.filter { it != '\n' })
                        })
                    },
                    textStyle = TextStyle.Default.copy(color = contentColor)
                )

                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HitomiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    topOffset: Int,
    onTopOffsetChange: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    var state by remember { mutableStateOf(HitomiSearchBarState.NORMAL) }
    val isFocused by derivedStateOf { state != HitomiSearchBarState.NORMAL }

    LaunchedEffect(state) {
        if (isFocused && topOffset != 0) {
            val animation = AnimationState(Int.VectorConverter, topOffset)
            animation.animateTo(0) {
                onTopOffsetChange(value)
            }
        }
    }

    val insetsPaddingValues = rememberInsetsPaddingValues(
        LocalWindowInsets.current.systemBars
    )

    BackHandler(isFocused) {
        state = HitomiSearchBarState.NORMAL
    }

    BoxWithConstraints {
        val focusedTransition = updateTransition(isFocused, "focusedTransition")
        val width by focusedTransition.animateDp(label = "hitomiSearchbarWidth") { isFocused ->
            if (isFocused) min(maxWidth, 1000.dp)
            else maxWidth
        }
        val height by focusedTransition.animateDp(label = "hitomiSearchbarHeight") { isFocused ->
            if (isFocused) maxHeight - insetsPaddingValues.calculateTopPadding() - insetsPaddingValues.calculateBottomPadding()
            else 64.dp
        }
        val padding by focusedTransition.animateDp(label = "hitomiSearchbarPadding") { isFocused ->
            if (isFocused) 16.dp else 8.dp
        }

        content()
        Scrim(
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
            onDismiss = { state = HitomiSearchBarState.NORMAL },
            isFocused
        )

        val interactionSource = remember { MutableInteractionSource() }
        Card(
            modifier = Modifier
                .systemBarsPadding()
                .width(width)
                .height(height)
                .padding(padding)
                .align(Alignment.TopCenter)
                .absoluteOffset { IntOffset(0, topOffset) }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    state =
                        if (isFocused) HitomiSearchBarState.NORMAL else HitomiSearchBarState.SEARCH
                },
            elevation = if (isFocused) 16.dp else 8.dp
        ) {
            val tags = query.split(' ')

            when (state) {
                HitomiSearchBarState.NORMAL -> {
                }
                HitomiSearchBarState.SEARCH -> {
                    FlowRow {
                        tags.dropLast(1).forEach { tag ->
                            TagChip(tag) {
                                onQueryChange(query.replace(tag, ""))
                            }
                        }

                        InputChip(
                            tag = tags.last().replace('_', ' '),
                            onTagChange = {
                                onQueryChange(buildString {
                                    tags.dropLast(1).forEach {
                                        append(it)
                                        append(' ')
                                    }

                                    append(it.replace(' ', '_'))
                                })
                            },
                            onEnter = {
                                onQueryChange("$query ")
                            },
                            onRemove = {
                                onQueryChange(query.dropLastWhile { it != ' ' }.dropLast(1))
                            }
                        )
                    }
                }
            }
        }
    }
}
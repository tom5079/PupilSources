package xyz.quaver.pupil.sources.hitomi.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.theme.Blue700
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.theme.Pink600
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.hitomi.lib.SortOptions

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

private val iconMap = mapOf(
    "female" to Icons.Default.Female,
    "male" to Icons.Default.Male,
    "artist" to Icons.Default.Brush,
    "group" to Icons.Default.Group,
    "character" to Icons.Default.Face,
    "series" to Icons.Default.Book,
    "type" to Icons.Default.Folder,
    "language" to Icons.Default.Translate,
)

@Composable
fun TagChipIcon(area: String) {
    val icon = iconMap[area]

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
}

@OptIn(ExperimentalGraphicsApi::class)
@Composable
fun TagChipLayout(
    tag: String,
    leftIcon: @Composable (String, String) -> Unit = { area, _ -> TagChipIcon(area) },
    rightIcon: @Composable (String, String) -> Unit,
    content: @Composable (String, String) -> Unit
) {
    val (area, tagPart) = tag.split(':', limit = 2).let {
        if (it.size == 1 || it[0] !in iconMap.keys) listOf("", tag) else it
    }
    val isFavorite = false

    val surfaceColor = if (isFavorite) Orange500 else when (area) {
        "male" -> Blue700
        "female" -> Pink600
        else -> MaterialTheme.colors.surface
    }

    Surface(
        modifier = Modifier
            .padding(2.dp)
            .height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        elevation = 8.dp
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColorFor(surfaceColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftIcon(area, tagPart)
                content(area, tagPart)
                rightIcon(area, tagPart)
            }
        }
    }
}

@Composable
fun TagChip(
    tag: String,
    onRemove: (() -> Unit)? = null
) {
    TagChipLayout(
        tag,
        rightIcon = { _, _ ->
            if (onRemove != null)
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                )
            else
                Box(Modifier.size(16.dp))
        }
    ) { _, tagPart ->
        Text(tagPart)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputChip(
    tag: String,
    onTagChange: (String) -> Unit,
    onEnter: () -> Unit,
    onRemove: () -> Unit,
    focusRequester: FocusRequester
) {
    TagChipLayout(
        tag,
        rightIcon = { _, _ ->
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
    ) { area, tagPart ->
        BasicTextField(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (
                        tagPart.isEmpty() &&
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
            value = tagPart,
            onValueChange = {
                onTagChange(buildString{
                    if (area.isNotEmpty()) {
                        append(area)
                        append(':')
                    }

                    append(it.filter { it != '\n' })
                })
            },
            textStyle = TextStyle.Default.copy(color = LocalContentColor.current),
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onEnter() }
            )
        )
    }
}

@Composable
private fun Normal(
    query: String,
    sortOption: SortOptions,
    onSortOptionChange: (SortOptions) -> Unit,
    onStateChange: (HitomiSearchBarState) -> Unit
) {
    val tags = query.split(' ')
    val scrollState = rememberScrollState()

    Box {
        Row(
            modifier = Modifier.height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    if (tag.isNotEmpty()) TagChip(tag.replace('_', ' '))
                }
            }

            Row {
                var expanded by remember { mutableStateOf(false) }

                IconButton(onClick = { onStateChange(HitomiSearchBarState.SETTINGS) }) {
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
                    onSortOptionChange(it)
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
                                RadioButton(selected = sortOption == currentSortOption, onClick = { onClick(currentSortOption) })
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
        }
    }
}

@Composable
private fun Search(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val tags = query.split(' ')

    Column {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            mainAxisSpacing = 8.dp
        ) {
            tags.dropLast(1).forEach { tag ->
                TagChip(tag.replace('_', ' ')) {
                    onQueryChange(tags.filter { it != tag }.joinToString(" "))
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
                    if (!query.endsWith(' ')) onQueryChange("$query ")
                },
                onRemove = {
                    onQueryChange(query.dropLastWhile { it != ' ' }.dropLast(1))
                },
                focusRequester = focusRequester
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun HitomiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOption: SortOptions,
    onSortOptionChange: (SortOptions) -> Unit,
    topOffset: Int,
    onTopOffsetChange: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    var state by remember { mutableStateOf(HitomiSearchBarState.NORMAL) }
    val isFocused by derivedStateOf { state != HitomiSearchBarState.NORMAL }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (isFocused && topOffset != 0) {
            val animation = AnimationState(Int.VectorConverter, topOffset)
            animation.animateTo(0) {
                onTopOffsetChange(value)
            }
        }

        if (state == HitomiSearchBarState.SEARCH) focusRequester.requestFocus()
    }

    val imePaddingValues = rememberInsetsPaddingValues(
        LocalWindowInsets.current.ime
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
            if (isFocused) maxHeight - imePaddingValues.calculateBottomPadding()
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
                .statusBarsPadding()
                .navigationBarsWithImePadding()
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
            when (state) {
                HitomiSearchBarState.NORMAL -> Normal(
                    query,
                    sortOption,
                    onSortOptionChange,
                    onStateChange = { state = it }
                )
                HitomiSearchBarState.SEARCH -> Search(
                    query,
                    onQueryChange,
                    focusRequester
                )
                HitomiSearchBarState.SETTINGS -> {

                }
            }
        }
    }
}
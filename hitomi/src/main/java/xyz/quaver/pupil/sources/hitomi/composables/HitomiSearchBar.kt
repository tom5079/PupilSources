package xyz.quaver.pupil.sources.hitomi.composables

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding
import io.ktor.client.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import org.kodein.di.compose.rememberInstance
import xyz.quaver.pupil.sources.base.composables.ErrorMessage
import xyz.quaver.pupil.sources.base.theme.Blue700
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.theme.Pink600
import xyz.quaver.pupil.sources.hitomi.HitomiDatabase
import xyz.quaver.pupil.sources.hitomi.lib.Suggestion
import xyz.quaver.pupil.sources.hitomi.lib.getSuggestionsForQuery

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
    "tag" to Icons.Default.LocalOffer
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
        Spacer(Modifier.width(16.dp))
}

@OptIn(ExperimentalGraphicsApi::class, ExperimentalMaterialApi::class)
@Composable
fun TagChip(
    tag: String,
    isFavorite: Boolean = false,
    enabled: Boolean = true,
    onClick: (String) -> Unit = { },
    leftIcon: @Composable (String, String) -> Unit = { area, _ -> TagChipIcon(area) },
    rightIcon: @Composable (String, String) -> Unit = { _, _ -> Spacer(Modifier.width(16.dp)) },
    content: @Composable (String, String) -> Unit = { _, tagPart -> Text(tagPart) },
) {
    val (area, tagPart) = tag.split(':', limit = 2).let {
        if (it.size == 1 || it[0] !in iconMap.keys) listOf("", tag) else it
    }

    val surfaceColor = if (isFavorite) Orange500 else when (area) {
        "male" -> Blue700
        "female" -> Pink600
        else -> MaterialTheme.colors.surface
    }

    val contentColor =
        if (surfaceColor == MaterialTheme.colors.surface)
            MaterialTheme.colors.onSurface
        else
            Color.White

    val inner = @Composable {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.body2
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

    val modifier = Modifier.height(32.dp)
    val shape = RoundedCornerShape(16.dp)

    if (enabled)
        Surface(
            modifier = modifier,
            shape = shape,
            color = surfaceColor,
            elevation = 8.dp,
            onClick = { onClick(tag) },
            content = inner
        )
    else
        Surface(
            modifier,
            shape = shape,
            color = surfaceColor,
            elevation = 8.dp,
            content = inner
        )
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
    TagChip(
        tag,
        rightIcon = { _, _ ->
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier
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
fun TagGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier.padding(8.dp),
        mainAxisSpacing = 4.dp,
        crossAxisSpacing = 4.dp,
        content = content
    )
}

@Composable
private fun Normal(
    query: String,
    actions: @Composable RowScope.() -> Unit
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
                Box(Modifier.size(8.dp))

                if (query.isEmpty())
                    Text("Search...", style = MaterialTheme.typography.subtitle1)

                tags.forEach { tag ->
                    if (tag.isNotEmpty()) TagChip(tag.replace('_', ' '), enabled = false)
                }
            }

            Row(content = actions)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun Search(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    focusRequester: FocusRequester
) {
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    val database: HitomiDatabase by rememberInstance()
    val favoritesDao = database.favoritesDao()
    val favoriteTags by favoritesDao.getTags().collectAsState(emptyList())

    val client: HttpClient by rememberInstance()
    val suggestions by produceState<List<String>>(emptyList(), textValue, favoriteTags) {
        textValue.text.let { query ->
            value = favoriteTags.filter { it.contains(query, true) }

            if (query.isNotEmpty())
                value = (value + client.getSuggestionsForQuery(query).map { "${it.n}:${it.s}" }).distinct()
        }
    }


    Column(
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Query")

            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    mainAxisSpacing = 8.dp
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            tag,
                            rightIcon = { _, _ ->
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onTagsChange(tags.filter { it != tag })
                                        }
                                )
                            }
                        )
                    }
                }
            } else {
                ErrorMessage(
                    Modifier.align(Alignment.CenterHorizontally),
                    "(＊´д｀)??"
                ) {
                    Text("Empty Search")
                }
            }

            if (suggestions.isNotEmpty()) {
                Divider()
                Text("Suggestions")
                TagGroup {
                    suggestions.forEach { suggestion ->
                        TagChip(
                            suggestion,
                            onClick = {
                                textValue = TextFieldValue()
                                onTagsChange(tags+it)
                            }
                        )
                    }
                }
            }
        }

        TextField(
            textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            placeholder = { Text("Search...") },
            trailingIcon = {
                IconButton(onClick = { textValue = TextFieldValue() }) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun HitomiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    topOffset: Int,
    onTopOffsetChange: (Int) -> Unit,
    state: HitomiSearchBarState,
    onStateChange: (HitomiSearchBarState) -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val isFocused by derivedStateOf { state != HitomiSearchBarState.NORMAL }

    val focusRequester = remember { FocusRequester() }

    val imePaddingValues = rememberInsetsPaddingValues(
        LocalWindowInsets.current.ime
    )

    LaunchedEffect(state) {
        if (isFocused && topOffset != 0) {
            val animation = AnimationState(Int.VectorConverter, topOffset)
            animation.animateTo(0) {
                onTopOffsetChange(value)
            }
        }

        if (state == HitomiSearchBarState.SEARCH)
            while (true) {
                try {
                    focusRequester.requestFocus()
                    break
                } catch (e: IllegalStateException) {
                    delay(100)
                }
            }

    }

    BackHandler(isFocused) {
        onStateChange(HitomiSearchBarState.NORMAL)
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
            onDismiss = { onStateChange(HitomiSearchBarState.NORMAL) },
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
                    onStateChange(
                        if (isFocused) HitomiSearchBarState.NORMAL else HitomiSearchBarState.SEARCH
                    )
                },
            elevation = if (isFocused) 16.dp else 8.dp
        ) {
            when (state) {
                HitomiSearchBarState.NORMAL -> Normal(
                    query,
                    actions
                )
                HitomiSearchBarState.SEARCH -> Search(
                    query.split(' ').filter { it.isNotBlank() }.map { it.replace('_', ' ') },
                    onTagsChange = { tags ->
                        onQueryChange(tags.joinToString(" ") { it.replace(' ', '_') })
                    },
                    focusRequester
                )
                HitomiSearchBarState.SETTINGS -> {

                }
            }
        }
    }
}
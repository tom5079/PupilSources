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

package xyz.quaver.pupil.sources.hitomi.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import io.ktor.client.*
import org.kodein.di.compose.rememberInstance
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.hitomi.lib.GalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.joinToCapitalizedString

private val languageMap = mapOf(
    "indonesian" to "Bahasa Indonesia",
    "catalan" to "català",
    "cebuano" to "Cebuano",
    "czech" to "Čeština",
    "danish" to "Dansk",
    "german" to "Deutsch",
    "estonian" to "eesti",
    "english" to "English",
    "spanish" to "Español",
    "esperanto" to "Esperanto",
    "french" to "Français",
    "italian" to "Italiano",
    "latin" to "Latina",
    "hungarian" to "magyar",
    "dutch" to "Nederlands",
    "norwegian" to "norsk",
    "polish" to "polski",
    "portuguese" to "Português",
    "romanian" to "română",
    "albanian" to "shqip",
    "slovak" to "Slovenčina",
    "finnish" to "Suomi",
    "swedish" to "Svenska",
    "tagalog" to "Tagalog",
    "vietnamese" to "tiếng việt",
    "turkish" to "Türkçe",
    "greek" to "Ελληνικά",
    "mongolian" to "Монгол",
    "russian" to "Русский",
    "ukrainian" to "Українська",
    "hebrew" to "עברית",
    "arabic" to "العربية",
    "persian" to "فارسی",
    "thai" to "ไทย",
    "korean" to "한국어",
    "chinese" to "中文",
    "japanese" to "日本語"
)

@ExperimentalMaterialApi
@Composable
fun DetailedSearchResult(
    result: GalleryInfo,
    favorites: Set<String>,
    onGalleryFavoriteToggle: (String) -> Unit = { },
    onTagFavoriteToggle: (String) -> Unit = { },
    onClick: (GalleryInfo) -> Unit = { }
) {
    val client: HttpClient by rememberInstance()

    val thumbnail by produceState<String?>(null, result) {
        value = result.thumbnail(client)
    }

    Card(
        modifier = Modifier
            .padding(8.dp, 4.dp)
            .fillMaxWidth()
            .clickable { onClick(result) },
        elevation = 4.dp
    ) {
        Column {
            Row {
                AsyncImage(
                    thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .width(150.dp)
                        .padding(0.dp, 0.dp, 8.dp, 0.dp)
                        .align(Alignment.CenterVertically),
                    contentScale = ContentScale.FillWidth
                )
                Column {
                    Text(
                        result.title,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )

                    val artistsAndGroups = buildString {
                        if (!result.artists.isNullOrEmpty())
                            append(result.artists.joinToCapitalizedString())

                        if (!result.groups.isNullOrEmpty()) {
                            if (this.isNotEmpty()) append(' ')
                            append('(')
                            append(result.groups.joinToCapitalizedString())
                            append(')')
                        }
                    }

                    Text(
                        artistsAndGroups,
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    if (result.parodys?.isNotEmpty() == true)
                        Text(
                            "Series: ${result.parodys.joinToCapitalizedString()}",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )

                    Text(
                        "Type: ${result.type}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    languageMap[result.language]?.let {
                        Text(
                            "Language: $it",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    key(result.tags) {
                        TagGroup(
                            tags = result.tags.orEmpty().map { it.toString() },
                            favorites,
                            onFavoriteToggle = onTagFavoriteToggle
                        )
                    }
                }
            }

            Divider()

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        result.id,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    Icon(
                        if (result.id in favorites) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = null,
                        tint = Orange500,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                onGalleryFavoriteToggle(result.id)
                            }
                    )
                }

                Text(
                    "${result.files.size}P",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagGroup(
    tags: List<String>,
    favorites: Set<String>,
    onFavoriteToggle: (String) -> Unit = { }
) {
    var isFolded by remember { mutableStateOf(true) }

    FlowRow(
        Modifier.padding(0.dp, 16.dp),
        mainAxisSpacing = 4.dp,
        crossAxisSpacing = 4.dp
    ) {
        tags.sortedBy { if (favorites.contains(it)) 0 else 1 }
            .let { (if (isFolded) it.take(10) else it) }.forEach { tag ->
                TagChip(
                    tag = tag,
                    isFavorite = favorites.contains(tag),
                    onFavoriteClick = onFavoriteToggle
                )
            }

        if (isFolded && tags.size > 10)
            Surface(
                modifier = Modifier.padding(2.dp),
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(16.dp),
                elevation = 2.dp,
                onClick = { isFolded = false }
            ) {
                Text(
                    "…",
                    modifier = Modifier.padding(16.dp, 8.dp),
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body2
                )
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagChip(
    tag: String,
    isFavorite: Boolean,
    onClick: (String) -> Unit = { },
    onFavoriteClick: (String) -> Unit = { }
) {
    val starIcon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline

    TagChip(
        tag,
        isFavorite,
        onClick = { onClick(tag) },
        rightIcon = { _, _ ->
            Icon(
                starIcon,
                contentDescription = "Favorites",
                modifier = Modifier
                    .padding(8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable { onFavoriteClick(tag) }
            )
        }
    ) { _, tagPart ->
        Text(
            tagPart,
            style = MaterialTheme.typography.body2
        )
    }
}

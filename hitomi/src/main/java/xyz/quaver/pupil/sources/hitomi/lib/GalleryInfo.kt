package xyz.quaver.pupil.sources.hitomi.lib

import io.ktor.client.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GalleryInfo(
    val id: String,
    val title: String,
    val japanese_title: String? = null,
    val language: String? = null,
    val type: String,
    val date: String,
    val artists: List<Artist>? = null,
    val groups: List<Group>? = null,
    val parodys: List<Parody>? = null,
    val tags: List<Tag>? = null,
    val related: List<Int> = emptyList(),
    val languages: List<Language> = emptyList(),
    val characters: List<Character>? = null,
    val scene_indexes: List<Int>? = emptyList(),
    val files: List<GalleryFiles> = emptyList()
) {
    suspend fun thumbnail(client: HttpClient) = files.firstOrNull()?.let { client.urlFromUrlFromHash(it, "webpbigtn", "webp", "tn") }
}

@JvmName("joinToCapitalizedStringArtist")
fun List<Artist>.joinToCapitalizedString() = joinToString { it.artist.replaceFirstChar(Char::titlecase) }
@JvmName("joinToCapitalizedStringGroup")
fun List<Group>.joinToCapitalizedString() = joinToString { it.group.replaceFirstChar(Char::titlecase) }
@JvmName("joinToCapitalizedStringParody")
fun List<Parody>.joinToCapitalizedString() = joinToString { it.parody.replaceFirstChar(Char::titlecase) }
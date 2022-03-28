package xyz.quaver.pupil.sources.hitomi.composables

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.rememberViewModel
import org.kodein.di.instance
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.composables.ReaderBase
import xyz.quaver.pupil.sources.base.composables.ReaderBaseViewModel
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.util.withLocalResource
import xyz.quaver.pupil.sources.hitomi.HitomiDatabase
import xyz.quaver.pupil.sources.hitomi.lib.GalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.imageUrlFromImage

class HitomiReaderViewModel(override val di: DI): ReaderBaseViewModel(di) {

    private val client: HttpClient by instance()

    suspend fun load(galleryInfo: GalleryInfo) {
        runCatching {
            load(
                galleryInfo.files.map {
                    client.imageUrlFromImage(it)
                        .first { it.endsWith("webp") }
                }
            )
        }.onFailure {
            error = true
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun Reader(itemID: String) {
    val model: HitomiReaderViewModel by rememberViewModel()

    val client: HttpClient by rememberInstance()

    val database: HitomiDatabase by rememberInstance()
    val favoritesDao = remember { database.favoritesDao() }

    val coroutineScope = rememberCoroutineScope()

    val isFavorite by favoritesDao.contains(itemID).collectAsState(false)
    val galleryInfo by produceState<GalleryInfo?>(null) {
        runCatching {
            val galleryID = itemID.toInt()

            value = client.getGalleryInfo(galleryID).also {
                model.load(it)
            }
        }.onFailure {
            model.error = true
        }
    }

    BackHandler(model.fullscreen) {
        model.fullscreen = false
    }

    Scaffold(
        topBar = {
            if (!model.fullscreen)
                TopAppBar(
                    title = {
                        withLocalResource {
                            Text(
                                galleryInfo?.title ?: stringResource(R.string.loading),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                if (isFavorite) favoritesDao.delete(itemID)
                                else            favoritesDao.insert(itemID)
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
                        applyBottom = true
                    )
                )
        },
        contentPadding = rememberInsetsPaddingValues(
            LocalWindowInsets.current.navigationBars
        )
    ) { contentPadding ->
        ReaderBase(
            Modifier.padding(contentPadding),
            model
        )
    }
}
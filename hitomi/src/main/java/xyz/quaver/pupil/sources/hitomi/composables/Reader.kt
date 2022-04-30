package xyz.quaver.pupil.sources.hitomi.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import io.ktor.client.*
import kotlinx.coroutines.launch
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
import xyz.quaver.pupil.sources.hitomi.HitomiPlugin
import xyz.quaver.pupil.sources.hitomi.lib.GalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.imageUrlFromImage

class HitomiReaderViewModel(override val di: DI): ReaderBaseViewModel(di) {
    private val client: HttpClient by instance()

    override var currentIndex: Int
        get() = super.currentIndex
        set(value) {
            super.currentIndex = value

            HitomiPlugin.setViewerPosition(value)
        }

    suspend fun load(galleryInfo: GalleryInfo) {
        runCatching {
            val images = galleryInfo.files.map { galleryFiles ->
                client.imageUrlFromImage(galleryFiles).first { it.endsWith("webp") }
            }

            HitomiPlugin.setViewerImages(images)
            load(images)
        }.onFailure {
            error = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        HitomiPlugin.setViewerImages(null)
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun Reader(itemID: String) {
    val model: HitomiReaderViewModel by rememberViewModel()

    val listState = rememberLazyListState()

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

    LaunchedEffect(listState.firstVisibleItemIndex) {
        model.currentIndex = listState.firstVisibleItemIndex
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
                                else            favoritesDao.insertGallery(itemID)
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
            model,
            listState
        )
    }
}
package xyz.quaver.pupil.sources.hitomi.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.common.util.concurrent.RateLimiter
import io.ktor.client.*
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.compose.rememberInstance
import xyz.quaver.pupil.sources.R
import xyz.quaver.pupil.sources.base.composables.ReaderBase
import xyz.quaver.pupil.sources.base.composables.ReaderBaseViewModel
import xyz.quaver.pupil.sources.hitomi.HitomiDatabase
import xyz.quaver.pupil.sources.hitomi.lib.GalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.getGalleryInfo
import xyz.quaver.pupil.sources.hitomi.lib.getReferer
import xyz.quaver.pupil.sources.hitomi.lib.imageUrlFromImage
import xyz.quaver.pupil.sources.base.theme.Orange500
import xyz.quaver.pupil.sources.base.util.withLocalResource

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@Composable
fun Reader(navController: NavController) {
    val model: ReaderBaseViewModel = viewModel()

    val localDI = localDI()
    val client: HttpClient by rememberInstance()

    val database: HitomiDatabase by rememberInstance()
    val favoritesDao = remember { database.favoritesDao() }

    val coroutineScope = rememberCoroutineScope()

    val itemID = navController.currentBackStackEntry?.arguments?.getString("itemID")

    if (itemID == null) model.error = true

    val isFavorite by favoritesDao.contains(itemID ?: "").collectAsState(false)
    val galleryInfo by produceState<GalleryInfo?>(null) {
        runCatching {
            val galleryID = itemID!!.toInt()

            value = with(localDI) { getGalleryInfo(galleryID) }.also {
                model.load(
                    it.files.map { with(localDI) { imageUrlFromImage(galleryID, it, false) } },
                    RateLimiter.create(2.0)
                ) {
                    append("Referer", getReferer(galleryID))
                }
            }
        }.onFailure {
            model.error = true
        }
    }

    BackHandler {
        if (model.fullscreen) model.fullscreen = false
        else navController.popBackStack()
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
                            itemID?.let {
                                coroutineScope.launch {
                                    if (isFavorite) favoritesDao.delete(it)
                                    else            favoritesDao.insert(it)
                                }
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
                        applyBottom = false
                    )
                )
        }
    ) { contentPadding ->
        ReaderBase(
            Modifier.padding(contentPadding),
            model
        )
    }
}
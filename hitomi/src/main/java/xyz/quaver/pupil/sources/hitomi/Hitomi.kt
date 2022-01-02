package xyz.quaver.pupil.sources.hitomi

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.*
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.compose.withDI
import xyz.quaver.pupil.sources.core.Source
import xyz.quaver.pupil.sources.hitomi.composables.Reader
import xyz.quaver.pupil.sources.hitomi.composables.Search
import xyz.quaver.pupil.sources.base.util.LocalResourceContext

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class Hitomi(app: Application): Source(), DIAware {
    override val di by subDI(closestDI(app)) {
        bindSingleton {
            Room.databaseBuilder(app, HitomiDatabase::class.java, name).build()
        }

        bindSingleton {
            WebView(app).apply {
                settings.javaScriptEnabled = true
                loadData("""<script src="https://ltn.hitomi.la/gg.js"></script>""", "text/html", null)
            }
        }

        bindProvider { HitomiSearchResultViewModel(di) }
    }

    private val resourceContext =
        app.createPackageContext("xyz.quaver.pupil.sources.hitomi", 0)

    override val name = "hitomi.la"

    override fun NavGraphBuilder.navGraph(navController: NavController) {
        navigation(route = name, startDestination = "hitomi.la/search") {
            composable("hitomi.la/search") {
                withDI(di) {
                    CompositionLocalProvider(LocalResourceContext provides resourceContext) {
                        Search(navController)
                    }
                }
            }
            composable("hitomi.la/reader/{itemID}") {
                withDI(di) {
                    CompositionLocalProvider(LocalResourceContext provides resourceContext) {
                        Reader(navController)
                    }
                }
            }
        }
    }
}
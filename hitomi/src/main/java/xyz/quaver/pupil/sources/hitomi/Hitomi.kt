package xyz.quaver.pupil.sources.hitomi

import android.annotation.SuppressLint
import android.app.Application
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.compose.withDI
import xyz.quaver.pupil.sources.base.util.LocalResourceContext
import xyz.quaver.pupil.sources.core.Source
import xyz.quaver.pupil.sources.hitomi.composables.Reader
import xyz.quaver.pupil.sources.hitomi.composables.Search

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class Hitomi(app: Application): Source(), DIAware {
    private val packageName = "xyz.quaver.pupil.sources.hitomi"
    private val resourceContext = app.createPackageContext(packageName, 0)

    override val di by subDI(closestDI(app)) {
        bindSingleton {
            Room.databaseBuilder(app, HitomiDatabase::class.java, packageName).build()
        }

        bindSingleton {
            WebView(app).apply {
                settings.javaScriptEnabled = true
                loadData("""<script src="https://ltn.hitomi.la/gg.js"></script>""", "text/html", null)
            }
        }

        bindProvider { HitomiSearchResultViewModel(di) }
    }

    @Composable
    override fun Entry() = withDI(di) {
        val navController = rememberNavController()

        CompositionLocalProvider(LocalResourceContext provides resourceContext) {
            NavHost(navController, startDestination = "search") {
                composable("search") {
                    Search(
                        navigateToReader = {
                            navController.navigate("reader/$it")
                        }
                    )
                }
                composable("reader/{itemID}") {
                    Reader(
                        itemID = navController.currentBackStackEntry?.arguments?.getString("itemID")!!
                    )
                }
            }
        }
    }
}
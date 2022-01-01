package xyz.quaver.pupil.sources.hitomi

import android.app.Application
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
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import xyz.quaver.pupil.sources.core.Source
import xyz.quaver.pupil.sources.hitomi.composables.Reader
import xyz.quaver.pupil.sources.hitomi.composables.Search
import xyz.quaver.pupil.sources.base.util.LocalResourceContext

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class Hitomi(app: Application): Source(), DIAware {
    override val di by subDI(closestDI(app)) {
        bindSingleton {
            Room.databaseBuilder(app, HitomiDatabase::class.java, name).build()
        }

        bindProvider { HitomiSearchResultViewModel(instance()) }
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
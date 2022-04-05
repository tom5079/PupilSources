package xyz.quaver.pupil.sources.hitomi

import android.app.Application
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import io.ktor.client.*
import io.ktor.client.features.*
import org.kodein.di.*
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.compose.withDI
import xyz.quaver.pupil.sources.base.util.LocalResourceContext
import xyz.quaver.pupil.sources.core.Source
import xyz.quaver.pupil.sources.hitomi.composables.HitomiReaderViewModel
import xyz.quaver.pupil.sources.hitomi.composables.Reader
import xyz.quaver.pupil.sources.hitomi.composables.Search

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
class Hitomi(app: Application): Source() {
    private val resourceContext = app.createPackageContext(packageName, 0)

    override val di by subDI(closestDI(app)) {
        bindSingleton {
            Room.databaseBuilder(app, HitomiDatabase::class.java, packageName)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        bindSingleton(overrides = true) {
            val parentDI by closestDI(app)
            val client: HttpClient = parentDI.direct.instance()

            HttpClient(client.engine) {
                install(HitomiPlugin)
                BrowserUserAgent()
            }
        }

        bindProvider { HitomiSearchResultViewModel(di) }
        bindProvider { HitomiReaderViewModel(di) }

        bindInstance { app.hitomiSettingsDataStore }
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
                    val itemID = navController.currentBackStackEntry?.arguments?.getString("itemID") ?: return@composable
                    Reader(itemID)
                }
            }
        }
    }

    companion object {
        val packageName = "xyz.quaver.pupil.sources.hitomi"
    }
}
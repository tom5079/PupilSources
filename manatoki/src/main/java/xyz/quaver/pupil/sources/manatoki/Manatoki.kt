package xyz.quaver.pupil.sources.manatoki

import android.app.Application
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import io.ktor.client.engine.okhttp.*
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.compose.withDI
import org.kodein.di.direct
import org.kodein.di.instance
import xyz.quaver.pupil.sources.base.composables.ReaderBaseViewModel
import xyz.quaver.pupil.sources.base.util.LocalResourceContext
import xyz.quaver.pupil.sources.core.Source
import xyz.quaver.pupil.sources.manatoki.composable.CaptchaDialog
import xyz.quaver.pupil.sources.manatoki.composable.Main
import xyz.quaver.pupil.sources.manatoki.composable.Reader
import xyz.quaver.pupil.sources.manatoki.composable.Recent
import xyz.quaver.pupil.sources.manatoki.networking.ManatokiHttpClient
import xyz.quaver.pupil.sources.manatoki.viewmodel.MainViewModel
import xyz.quaver.pupil.sources.manatoki.viewmodel.RecentViewModel

@OptIn(ExperimentalMaterialApi::class)
class Manatoki(app: Application) : Source() {
    private val resourceContext = app.createPackageContext(packageName, 0)

    override val di by subDI(closestDI(app), allowSilentOverride = true) {
        bindSingleton {
            Room.databaseBuilder(app, ManatokiDatabase::class.java, packageName)
                .build()
        }

        bindSingleton { ManatokiHttpClient(OkHttp.create(), instance()) }
        bindProvider { direct.instance<ManatokiHttpClient>().httpClient }

        bindProvider { MainViewModel(instance(), instance()) }
        bindProvider { ReaderBaseViewModel(di) }
        bindProvider { RecentViewModel(instance()) }
    }

    @Composable
    override fun Entry() = withDI(di) {
        val navController = rememberNavController()

        CaptchaDialog()

        CompositionLocalProvider(LocalResourceContext provides resourceContext) {
            NavHost(navController, startDestination = "main") {
                composable("main") {
                    Main(
                        navigateToReader = { itemID -> navController.navigate("reader/$itemID") },
                        navigateToRecent = { navController.navigate("recent") },
                        navigateToSearch = { navController.navigate("search") },
                        navigateToSettings = { }
                    )
                }

                composable("reader/{itemID}") {
                    val itemID = navController.currentBackStackEntry?.arguments?.getString("itemID") ?: ""

                    Reader(
                        itemID,
                        navigateToReader = { targetItemID ->
                            navController.navigate("reader/$targetItemID") {
                                popUpTo("main")
                            }
                        },
                        navigateUp = { navController.navigateUp() }
                    )
                }

                composable("recent") {
                    Recent(
                        navigateToReader = { itemID -> navController.navigate("reader/$itemID") },
                        navigateUp = { navController.navigateUp() }
                    )
                }
            }
        }
    }

    companion object {
        val packageName = "xyz.quaver.pupil.sources.manatoki"
    }
}
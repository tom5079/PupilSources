package xyz.quaver.pupil.sources.manatoki

import android.app.Application
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import org.kodein.di.android.closestDI
import org.kodein.di.android.subDI
import org.kodein.di.compose.withDI
import xyz.quaver.pupil.sources.core.Source

@OptIn(ExperimentalMaterialApi::class)
class Manatoki(app: Application) : Source() {
    private val resourceContext = app.createPackageContext(packageName, 0)

    override val di by subDI(closestDI(app), allowSilentOverride = true) {

    }

    @Composable
    override fun Entry() = withDI(di) {
        Text("OK")
    }

    companion object {
        val packageName = "xyz.quaver.pupil.sources.manatoki"
    }
}
package xyz.quaver.pupil.sources.util

import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext

val LocalResourceContext = staticCompositionLocalOf<Context> {
    error("LocalResourceContext not found")
}

@Composable
fun withLocalResource(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContext provides LocalResourceContext.current) {
        content()
    }
}
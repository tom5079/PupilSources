package xyz.quaver.pupil.sources.base.util

import android.content.Context
import androidx.compose.runtime.*
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
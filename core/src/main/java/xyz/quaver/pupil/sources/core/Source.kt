package xyz.quaver.pupil.sources.core

import androidx.compose.runtime.Composable
import org.kodein.di.DIAware

abstract class Source: DIAware {
    @Composable
    abstract fun Entry()
}
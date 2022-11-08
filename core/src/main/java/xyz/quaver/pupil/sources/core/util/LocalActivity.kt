package xyz.quaver.pupil.sources.core.util

import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf

val LocalActivity = staticCompositionLocalOf<Activity> {
    error("LocalActivity not found")
}
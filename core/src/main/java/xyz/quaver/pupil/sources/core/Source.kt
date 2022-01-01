package xyz.quaver.pupil.sources.core

import androidx.annotation.Keep
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

abstract class Source {
    abstract val name: String
    abstract fun NavGraphBuilder.navGraph(navController: NavController)
}
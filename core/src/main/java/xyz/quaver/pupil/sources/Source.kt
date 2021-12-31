package xyz.quaver.pupil.sources

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

abstract class Source {
    abstract val name: String

    abstract fun NavGraphBuilder.navGraph(navController: NavController)
}
package xyz.quaver.pupil.sources.hitomi

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import xyz.quaver.pupil.sources.base.util.LocalResourceContext
import xyz.quaver.pupil.sources.hitomi.composables.HitomiSearchBar
import xyz.quaver.pupil.sources.hitomi.composables.HitomiSearchBarState
import xyz.quaver.pupil.sources.hitomi.lib.SortOptions

class HitomiSearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test_Search() = runBlocking {
        composeTestRule.setContent {
            MaterialTheme {
                HitomiSearchBar(
                    "", {}, 0, {}, HitomiSearchBarState.SEARCH, {}, {}
                ) {
                    Text("TEST")
                }
            }
        }

        with (composeTestRule) {
            onNodeWithText("Search...").performClick()

            awaitIdle()

            delay(10000)
        }
    }

}
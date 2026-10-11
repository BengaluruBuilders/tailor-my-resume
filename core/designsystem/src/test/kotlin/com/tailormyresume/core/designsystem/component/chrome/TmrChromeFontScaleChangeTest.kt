package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrChromeFontScaleChangeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabLabelKeepsWholeWordOnFirstFrameAfterFontScaleChange() {
        val fontScale = mutableStateOf(1f)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale.value),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        TmrTabBar(selected = TmrTab.Applications, onApplications = {}, onAdd = {}, onProfile = {})
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { fontScale.value = 2f }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val results = mutableListOf<TextLayoutResult>()
        composeRule
            .onNode(
                hasText("Applications", ignoreCase = true) and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        assertEquals(1, requireNotNull(results.firstOrNull()).lineCount)
    }
}

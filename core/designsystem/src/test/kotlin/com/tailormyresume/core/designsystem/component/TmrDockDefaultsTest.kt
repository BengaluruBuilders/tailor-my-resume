package com.tailormyresume.core.designsystem.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.icon.TmrIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrDockDefaultsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renderedDockOccupiesHeightPlusPadding() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                    Box(Modifier.testTag("dockHost")) {
                        TmrDock {
                            TmrDockItem(
                                selected = true,
                                onClick = {},
                                label = "Home",
                                icon = { TmrDockIcon(TmrIcons.Applications) },
                            )
                        }
                    }
                }
            }
        }
        val expected =
            ((TmrDockDefaults.height + TmrDockDefaults.verticalPadding * 2).value).toInt()
        val actual = rule.onNodeWithTag("dockHost").fetchSemanticsNode().size.height
        assertEquals(expected, actual)
    }

    @Test
    fun insetClearsTheRenderedDock() {
        assertTrue(TmrDockDefaults.inset >= TmrDockDefaults.height + TmrDockDefaults.verticalPadding * 2)
    }
}

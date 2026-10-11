package com.tailormyresume.core.designsystem.component.chrome

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrBottomSheetStatusBarTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aSheetTallerThanTheScreenStopsBelowTheStatusBar() {
        val statusBarHeightPx = 96
        val state = TmrSheetHostState()
        state.show("Tall sheet") { Box(Modifier.fillMaxSize().height(4000.dp)) }
        rule.setContent { TmrPreviewTheme { TmrSheetHost(state) } }
        rule.runOnUiThread {
            val root = rule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusBarHeightPx, 0, 0))
                .setVisible(WindowInsetsCompat.Type.statusBars(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(root as View, insets)
        }
        rule.waitForIdle()

        val sheetTop = rule.onNodeWithTag(TmrChromeTags.SHEET).fetchSemanticsNode().boundsInRoot.top
        assertEquals(statusBarHeightPx.toFloat(), sheetTop, 0.5f)
    }
}

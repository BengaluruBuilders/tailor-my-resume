package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrToastControllerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun withToast(state: TmrToastState, block: (TmrToastState) -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTmrToast provides state) {
                val toast = LocalTmrToast.current
                Text("host")
                block(toast)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun secondToastReplacesFirst() {
        val state = TmrToastState()
        withToast(state) {}
        val shown = mutableListOf<String?>()

        composeRule.runOnIdle {
            state.show("first")
            shown += state.current?.message
            state.show("second", TmrToastAction("Undo") {})
            shown += state.current?.message
        }

        assertEquals(listOf<String?>("first", "second"), shown)
        assertEquals("Undo", state.current?.action?.label)
    }

    @Test
    fun entriesShowThroughTheProvidedController() {
        val state = TmrToastState()
        var seen: TmrToastState? = null
        withToast(state) { seen = it }

        composeRule.runOnIdle { seen?.show("Changes saved") }

        assertEquals("Changes saved", state.current?.message)
    }

    @Test
    fun theSheetHostHoldsOneSheetAndTheNextReplacesIt() {
        val host = TmrSheetHostState()

        host.show("First") {}
        host.show("Second") {}

        assertEquals("Second", host.current?.title)
        host.dismiss()
        assertNull(host.current)
    }
}

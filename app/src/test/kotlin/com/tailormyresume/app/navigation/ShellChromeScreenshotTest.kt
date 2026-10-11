package com.tailormyresume.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.applications.api.navigation.ApplicationDetailNavKey
import com.tailormyresume.feature.applications.api.navigation.DefaultApplicationsNavKey
import com.tailormyresume.feature.onboarding.api.navigation.ReadingNavKey
import com.tailormyresume.feature.onboarding.api.navigation.SignInNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.DefaultProfileNavKey
import com.tailormyresume.feature.tailor.api.navigation.EditResumeNavKey
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val ID = "app-1"

private const val NARROW_QUALIFIERS = "w337dp-h734dp-normal-long-notround-any-480dpi-keyshidden-nonav"

private val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private val STATES: List<Pair<String, List<NavKey>>> = listOf(
    "step1" to listOf(SignInNavKey(), UploadNavKey()),
    "step2" to listOf(SignInNavKey(), JobNavKey()),
    "step3" to listOf(SignInNavKey(), QuickQuestionNavKey(ID)),
    "noBack" to listOf(SignInNavKey(), ReadingNavKey()),
    "close" to listOf(SignInNavKey(), ExportedNavKey(ID)),
    "editResume" to listOf(DefaultApplicationsNavKey, EditResumeNavKey(ID)),
    "appDetail" to listOf(DefaultApplicationsNavKey, ApplicationDetailNavKey(ID)),
    "signIn" to listOf(SignInNavKey()),
    "tabsApplications" to listOf(DefaultApplicationsNavKey),
    "tabsProfile" to listOf(DefaultProfileNavKey),
)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShellChromeScreenshotTest {

    init {
        registerComposeActivity()
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
    fun chromeStates1x() {
        captureStates(TmrTestDevices.prototype)
    }

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun chromeStatesFont200At337dp() {
        captureStates(NARROW_DEVICE)
    }

    private fun captureStates(device: TmrTestDevice) {
        val shell = ShellHarness(rule)
        shell.show(SignInNavKey(), fontScale = device.fontScale)
        STATES.forEach { (name, stack) ->
            shell.root(stack.first())
            shell.go(*stack.drop(1).toTypedArray())
            runBlocking {
                rule.captureForDevice(
                    outputDirectory = "src/test/screenshots",
                    screenName = "shell_$name",
                    device = device,
                )
            }
            assertNoTruncatedText()
        }
    }

    private fun assertNoTruncatedText() {
        textLayouts().forEach { (node, layout) ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
            val widestLine = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
            assertTrue("$text overflows its width: line right $widestLine > ${layout.size.width}", widestLine <= layout.size.width + 0.5f)
            assertFalse("$text overflows its height", layout.didOverflowHeight)
            assertTrue("$text is clipped by its node", layout.size.width <= node.size.width && layout.size.height <= node.size.height)
            val ellipsized = (0 until layout.lineCount).filter { layout.isLineEllipsized(it) }
            assertTrue("$text has ellipsized lines $ellipsized", ellipsized.isEmpty())
        }
    }

    private fun textLayouts(): List<Pair<SemanticsNode, TextLayoutResult>> =
        rule.onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.firstOrNull()?.let { node to it }
            }
}

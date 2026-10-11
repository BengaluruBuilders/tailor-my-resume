package com.tailormyresume.core.designsystem.component.hero

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.LocalTmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import com.tailormyresume.core.designsystem.theme.TmrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrHeroTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    private var captured: () -> Long = { 0L }

    @Test
    fun everyPoseHasSpec() {
        assertSpec(
            pose = TmrPaigePose.Signin1,
            eyeWidth = 14.dp,
            eyeHeight = 19.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = true,
            arms = true,
            sad = false,
            rotationDegrees = 4f,
            shiftX = 0.dp,
            lift = (-46).dp,
            bobPhase = 0,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Signin2,
            eyeWidth = 11.dp,
            eyeHeight = 15.dp,
            eyeShiftX = (-4).dp,
            eyeShiftY = 0.dp,
            mouthWidth = 16.dp,
            mouthHeight = 8.dp,
            ticks = true,
            arms = true,
            sad = false,
            rotationDegrees = -8f,
            shiftX = 56.dp,
            lift = 10.dp,
            bobPhase = 0,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Signin3,
            eyeWidth = 18.dp,
            eyeHeight = 6.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 30.dp,
            mouthHeight = 15.dp,
            ticks = true,
            arms = true,
            sad = false,
            rotationDegrees = -5f,
            shiftX = (-30).dp,
            lift = 16.dp,
            bobPhase = 0,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Upload,
            eyeWidth = 14.dp,
            eyeHeight = 19.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = true,
            arms = true,
            sad = false,
            rotationDegrees = 4f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = 0,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Job,
            eyeWidth = 11.dp,
            eyeHeight = 15.dp,
            eyeShiftX = (-4).dp,
            eyeShiftY = 0.dp,
            mouthWidth = 16.dp,
            mouthHeight = 8.dp,
            ticks = false,
            arms = true,
            sad = false,
            rotationDegrees = -6f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = 1,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Question,
            eyeWidth = 13.dp,
            eyeHeight = 17.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = (-3).dp,
            mouthWidth = 14.dp,
            mouthHeight = 8.dp,
            ticks = false,
            arms = true,
            sad = false,
            rotationDegrees = 6f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = 2,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Tailoring,
            eyeWidth = 16.dp,
            eyeHeight = 6.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 20.dp,
            mouthHeight = 10.dp,
            ticks = true,
            arms = false,
            sad = false,
            rotationDegrees = 0f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = null,
            wobble = true,
        )
        assertSpec(
            pose = TmrPaigePose.Fail,
            eyeWidth = 11.dp,
            eyeHeight = 13.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 4.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = false,
            arms = true,
            sad = true,
            rotationDegrees = -5f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = null,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Home,
            eyeWidth = 14.dp,
            eyeHeight = 19.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = true,
            arms = true,
            sad = false,
            rotationDegrees = 5f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = 3,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Paywall,
            eyeWidth = 14.dp,
            eyeHeight = 19.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 0.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = false,
            arms = true,
            sad = false,
            rotationDegrees = 5f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = 4,
            wobble = false,
        )
        assertSpec(
            pose = TmrPaigePose.Delete,
            eyeWidth = 11.dp,
            eyeHeight = 13.dp,
            eyeShiftX = 0.dp,
            eyeShiftY = 4.dp,
            mouthWidth = 22.dp,
            mouthHeight = 12.dp,
            ticks = false,
            arms = false,
            sad = true,
            rotationDegrees = -4f,
            shiftX = 0.dp,
            lift = 0.dp,
            bobPhase = null,
            wobble = false,
        )
    }

    @Test
    fun translationYIs5dpAtQuarterPeriod() {
        val spec = TmrPaigePose.Signin1.spec()
        val timeMs = (420 * Math.PI / 2).toLong()
        val value = paigeTranslationYDp(TmrMotionDefaults.Default, spec, timeMs)
        assertEquals(5f, value, 0.01f)
    }

    @Test
    fun reducedHasZeroTimeTransformAtAllTimes() {
        val times = listOf(0L, 105L, 420L, 1000L, 2639L)
        TmrPaigePose.entries.forEach { pose ->
            val spec = pose.spec()
            times.forEach { timeMs ->
                assertEquals(0f, paigeTranslationYDp(TmrMotionDefaults.Reduced, spec, timeMs), 0f)
                assertEquals(0f, paigeRotationDegrees(TmrMotionDefaults.Reduced, spec, timeMs), 0f)
            }
        }
    }

    @Test
    fun tailoringWobbleWhenNotReduced() {
        val spec = TmrPaigePose.Tailoring.spec()
        val timeMs = (300 * Math.PI / 2).toLong()
        val value = paigeRotationDegrees(TmrMotionDefaults.Default, spec, timeMs)
        assertEquals(4f, value, 0.01f)
    }

    @Test
    fun reducedClockStaysZero() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Reduced) {
                val clock = rememberPaigeTimeMs(TmrMotionDefaults.Reduced, animated = true)
                SideEffect { captured = clock }
            }
        }
        var previous = 0L
        listOf(0L, 105L, 420L, 1000L, 2639L).forEach { ms ->
            rule.mainClock.advanceTimeBy(ms - previous)
            previous = ms
            assertEquals(0L, captured())
        }
    }

    @Test
    fun nonReducedClockRuns() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Default) {
                val clock = rememberPaigeTimeMs(TmrMotionDefaults.Default, animated = true)
                SideEffect { captured = clock }
            }
        }
        rule.mainClock.advanceTimeBy(1000)
        assertTrue(captured() > 0L)
    }

    @Test
    fun onlyMovingPosesAskForAnimatedClock() {
        val requested = mutableMapOf<TmrPaigePose, Boolean>()
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Default) {
                    TmrPaigePose.entries.forEach { pose ->
                        TmrPaige(pose, Modifier) { motion, animated ->
                            requested[pose] = animated
                            rememberPaigeTimeMs(motion, animated)
                        }
                    }
                }
            }
        }
        val still = setOf(TmrPaigePose.Fail, TmrPaigePose.Delete)
        TmrPaigePose.entries.forEach { pose ->
            assertEquals("${pose.name} clock request", pose !in still, requested[pose])
        }
    }

    @Test
    fun paigeUnderDefaultMotionReachesIdleWithAutoAdvancingClock() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Default) {
                    TmrPaige(TmrPaigePose.Home)
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }

    @Test
    fun reducedTailoringPixelsNeverChange() = assertFramesNeverChange(TmrPaigePose.Tailoring)

    @Test
    fun reducedHomePixelsNeverChange() = assertFramesNeverChange(TmrPaigePose.Home)

    @Test
    fun nonReducedTailoringPixelsChangeOverTime() = assertFramesChange(TmrPaigePose.Tailoring)

    @Test
    fun nonReducedHomePixelsChangeOverTime() = assertFramesChange(TmrPaigePose.Home)

    @Test
    fun paigeIsNotATalkBackStop() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Reduced) {
                    TmrPaige(TmrPaigePose.Home)
                }
            }
        }
        rule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true,
        ).assertCountEquals(0)
        rule.onAllNodes(isFocusable(), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun headlineStyle() {
        val headline =
            "Add the confirmed facts your recruiter already knows about you so the tailored " +
                "application reads like yours from the first line."
        rule.setContent {
            TmrPreviewTheme {
                Box(modifier = Modifier.width(300.dp)) {
                    TmrHeroCard(
                        color = TmrHeroColor.Blue,
                        label = "Step one",
                        headline = headline,
                    )
                }
            }
        }
        assertEquals(28.sp, styleOf(headline).fontSize)
        assertTrue(rule.onAllNodesWithText("STEP ONE").fetchSemanticsNodes().isNotEmpty())
        assertEquals(12.sp, styleOf("STEP ONE").fontSize)
    }

    @Test
    fun headlineFullyDisplayedAtFontScale200() {
        val headline = "Add the confirmed facts your recruiter already knows about you"
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                TmrPreviewTheme {
                    Box(modifier = Modifier.width(374.dp)) {
                        TmrHeroCard(
                            color = TmrHeroColor.Blue,
                            label = "Step one",
                            headline = headline,
                        )
                    }
                }
            }
        }
        val layout = layoutResultOf(headline)
        assertTrue("lineCount ${layout.lineCount}", layout.lineCount > 2)
        assertEquals(false, layout.hasVisualOverflow)
        assertEquals(headline, layout.layoutInput.text.text)
    }

    private fun assertSpec(
        pose: TmrPaigePose,
        eyeWidth: Dp,
        eyeHeight: Dp,
        eyeShiftX: Dp,
        eyeShiftY: Dp,
        mouthWidth: Dp,
        mouthHeight: Dp,
        ticks: Boolean,
        arms: Boolean,
        sad: Boolean,
        rotationDegrees: Float,
        shiftX: Dp,
        lift: Dp,
        bobPhase: Int?,
        wobble: Boolean,
    ) {
        val spec = pose.spec()
        assertEquals("${pose.name}.eyeWidth", eyeWidth, spec.eyeWidth)
        assertEquals("${pose.name}.eyeHeight", eyeHeight, spec.eyeHeight)
        assertEquals("${pose.name}.eyeShiftX", eyeShiftX, spec.eyeShiftX)
        assertEquals("${pose.name}.eyeShiftY", eyeShiftY, spec.eyeShiftY)
        assertEquals("${pose.name}.mouthWidth", mouthWidth, spec.mouthWidth)
        assertEquals("${pose.name}.mouthHeight", mouthHeight, spec.mouthHeight)
        assertEquals("${pose.name}.ticks", ticks, spec.ticks)
        assertEquals("${pose.name}.arms", arms, spec.arms)
        assertEquals("${pose.name}.sad", sad, spec.sad)
        assertEquals("${pose.name}.rotationDegrees", rotationDegrees, spec.rotationDegrees, 0f)
        assertEquals("${pose.name}.shiftX", shiftX, spec.shiftX)
        assertEquals("${pose.name}.lift", lift, spec.lift)
        assertEquals("${pose.name}.bobPhase", bobPhase, spec.bobPhase)
        assertEquals("${pose.name}.wobble", wobble, spec.wobble)
    }

    private fun assertFramesNeverChange(pose: TmrPaigePose) {
        val images = frames(pose, TmrMotionDefaults.Reduced)
        assertEquals(SAMPLE_TIMES_MS.size, images.size)
        images.forEachIndexed { index, image ->
            assertTrue(
                "${pose.name} moved at ${SAMPLE_TIMES_MS[index]}ms under reduced motion",
                images.first().contentEquals(image),
            )
        }
    }

    private fun assertFramesChange(pose: TmrPaigePose) {
        val images = frames(pose, TmrMotionDefaults.Default)
        assertEquals(SAMPLE_TIMES_MS.size, images.size)
        assertTrue(
            "${pose.name} never moved under default motion",
            images.any { !it.contentEquals(images.first()) },
        )
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun frames(pose: TmrPaigePose, motion: TmrMotion): List<ByteArray> {
        System.setProperty("robolectric.useEmbeddedViewRoot", "false")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides motion) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(TmrTheme.colors.background)
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) { TmrPaige(pose) }
                }
            }
        }
        val images = mutableListOf<ByteArray>()
        var previous = 0L
        SAMPLE_TIMES_MS.forEachIndexed { index, ms ->
            rule.mainClock.advanceTimeBy(ms - previous)
            previous = ms
            val file = folder.newFile("${pose.name}_${motion.reduced}_$index.png")
            captureScreenRoboImage(file.path, RoborazziOptions(taskType = RoborazziTaskType.Record))
            images += file.readBytes()
        }
        return images
    }

    private fun layoutResultOf(text: String): TextLayoutResult {
        val action = rule.onNodeWithText(text).fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        val results = mutableListOf<TextLayoutResult>()
        action?.action?.invoke(results)
        return results.first()
    }

    private fun styleOf(text: String): TextStyle = layoutResultOf(text).layoutInput.style

    private companion object {
        val SAMPLE_TIMES_MS = listOf(0L, 105L, 420L, 1000L, 2639L)
    }
}

package com.tailormyresume.core.designsystem.component.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.LocalTmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrHeroScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(screenName: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Reduced) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background).padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { content() }
                }
            }
        }
        runBlocking {
            composeRule.captureForDevice(outputDirectory = SCREENSHOT_DIRECTORY, screenName = screenName)
        }
    }

    private fun capturePose(pose: TmrPaigePose) =
        capture("hero_paige_${pose.name.lowercase()}") { TmrPaige(pose) }

    private fun captureCard(color: TmrHeroColor) = capture("hero_card_${color.name.lowercase()}") {
        TmrHeroCard(
            color = color,
            label = "Step one",
            headline = "Tell Paige about you",
            modifier = Modifier.height(320.dp),
        ) {
            TmrSticker(text = "Ready", rotationDegrees = -6f, modifier = Modifier.align(Alignment.BottomStart).padding(22.dp))
            TmrSticker(text = "New", onCheek = true, rotationDegrees = 8f, modifier = Modifier.align(Alignment.BottomStart).padding(start = 100.dp, bottom = 22.dp))
            TmrPaige(TmrPaigePose.Home, Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 8.dp))
        }
    }

    @Test
    fun paigeSignin1() = capturePose(TmrPaigePose.Signin1)

    @Test
    fun paigeSignin2() = capturePose(TmrPaigePose.Signin2)

    @Test
    fun paigeSignin3() = capturePose(TmrPaigePose.Signin3)

    @Test
    fun paigeUpload() = capturePose(TmrPaigePose.Upload)

    @Test
    fun paigeJob() = capturePose(TmrPaigePose.Job)

    @Test
    fun paigeQuestion() = capturePose(TmrPaigePose.Question)

    @Test
    fun paigeTailoring() = capturePose(TmrPaigePose.Tailoring)

    @Test
    fun paigeFail() = capturePose(TmrPaigePose.Fail)

    @Test
    fun paigeHome() = capturePose(TmrPaigePose.Home)

    @Test
    fun paigePaywall() = capturePose(TmrPaigePose.Paywall)

    @Test
    fun paigeDelete() = capturePose(TmrPaigePose.Delete)

    @Test
    fun cardBlue() = captureCard(TmrHeroColor.Blue)

    @Test
    fun cardAmber() = captureCard(TmrHeroColor.Amber)

    @Test
    fun cardLime() = captureCard(TmrHeroColor.Lime)

    private companion object {
        const val SCREENSHOT_DIRECTORY = "src/test/screenshots"
    }
}

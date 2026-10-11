package com.tailormyresume.feature.tailor.impl

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking

internal const val NARROW_QUALIFIERS = "w337dp-h1600dp-normal-long-notround-any-480dpi-keyshidden-nonav"

internal val NARROW_DEVICE = TmrTestDevice("narrow-337-font-200", NARROW_QUALIFIERS, TmrTestDevices.LARGE_FONT_SCALE)

private const val SCREENSHOT_DIRECTORY = "src/test/screenshots"

internal fun ComposeContentTestRule.captureResultScreen(
    screenName: String,
    device: TmrTestDevice = TmrTestDevices.prototype,
    beforeCapture: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, device.fontScale)) {
            TmrTheme {
                Box(modifier = Modifier.fillMaxSize().background(TmrTheme.colors.background)) { content() }
            }
        }
    }
    waitForIdle()
    beforeCapture()
    waitForIdle()
    runBlocking { captureForDevice(SCREENSHOT_DIRECTORY, screenName, device) }
}

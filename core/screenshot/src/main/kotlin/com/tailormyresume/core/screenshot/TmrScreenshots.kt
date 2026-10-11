package com.tailormyresume.core.screenshot

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.io.File

const val TMR_THEME_LIGHT = "light"
const val TMR_THEME_DARK = "dark"

const val TMR_SCREENSHOT_DIRECTORY_PROPERTY = "roborazzi.output.dir"

private const val TMR_PIXEL_TOLERANCE = 0.02f
private const val TMR_DIFF_SCALE = 10_000L
private const val TMR_MAX_DIFFERING_PER_TEN_THOUSAND = 5L
private const val TMR_ENABLED = "true"
private const val TMR_DEFAULT_OUTPUT_DIRECTORY = "build/outputs/roborazzi"
private const val TMR_DEFAULT_RESULT_DIRECTORY = "build/test-results/roborazzi/results/"
private const val TMR_RECORD_PROPERTY = "roborazzi.test.record"
private const val TMR_COMPARE_PROPERTY = "roborazzi.test.compare"
private const val TMR_VERIFY_PROPERTY = "roborazzi.test.verify"
private const val TMR_COMPARE_DIRECTORY_PROPERTY = "roborazzi.compare.output.dir"
private const val TMR_EMBEDDED_VIEW_ROOT_PROPERTY = "robolectric.useEmbeddedViewRoot"

private fun propertyIsSet(name: String): Boolean = System.getProperty(name) == TMR_ENABLED

private fun propertyOrDefault(name: String, fallback: String): String =
    System.getProperty(name) ?: fallback

@OptIn(ExperimentalRoborazziApi::class)
private fun tmrRoborazziOptions(): RoborazziOptions = RoborazziOptions(
    captureType = RoborazziOptions.CaptureType.Screenshot(),
    taskType = RoborazziTaskType.of(
        isRecording = propertyIsSet(TMR_RECORD_PROPERTY),
        isComparing = propertyIsSet(TMR_COMPARE_PROPERTY),
        isVerifying = propertyIsSet(TMR_VERIFY_PROPERTY),
    ),
    compareOptions = RoborazziOptions.CompareOptions(
        outputDirectoryPath = propertyOrDefault(TMR_COMPARE_DIRECTORY_PROPERTY, TMR_DEFAULT_OUTPUT_DIRECTORY),
        imageComparator = SimpleImageComparator(
            maxDistance = TMR_PIXEL_TOLERANCE,
            hShift = 0,
            vShift = 0,
        ),
        resultValidator = { result ->
            result.pixelDifferences.toLong() * TMR_DIFF_SCALE <=
                result.pixelCount.toLong() * TMR_MAX_DIFFERING_PER_TEN_THOUSAND
        },
    ),
)

@OptIn(ExperimentalRoborazziApi::class)
fun captureScreenHh(filePath: String) {
    stopCapturesSharingTheWindowRenderer()
    captureScreenRoboImage(filePath, tmrRoborazziOptions())
}

private fun stopCapturesSharingTheWindowRenderer() {
    System.setProperty(TMR_EMBEDDED_VIEW_ROOT_PROPERTY, "false")
}

fun TmrTestDevice.imageFileName(
    screenName: String,
    theme: String,
): String = "${screenName}_${name}_$theme.png"

fun TmrTestDevice.imageFile(
    outputDirectory: String,
    screenName: String,
    theme: String,
): File = File(outputDirectory, imageFileName(screenName, theme))

suspend fun ComposeTestRule.captureForDevice(
    outputDirectory: String,
    screenName: String,
    device: TmrTestDevice = TmrTestDevices.prototype,
    theme: String = TMR_THEME_DARK,
): String {
    val file = device.imageFile(outputDirectory, screenName, theme)
    captureScreenHh(file.path)
    return file.path
}

suspend fun ComposeTestRule.captureForDevices(
    outputDirectory: String,
    screenName: String,
    devices: List<TmrTestDevice> = TmrTestDevices.all,
    theme: String = TMR_THEME_DARK,
): List<String> = devices.map { captureForDevice(outputDirectory, screenName, it, theme) }

@OptIn(ExperimentalRoborazziApi::class)
suspend fun ComposeTestRule.captureMultiTheme(
    outputDirectory: String,
    screenName: String,
    device: TmrTestDevice = TmrTestDevices.prototype,
    setTheme: suspend (Boolean) -> Unit,
): List<String> {
    val paths = mutableListOf<String>()
    setTheme(false)
    waitForIdle()
    paths += captureForDevice(outputDirectory, screenName, device, TMR_THEME_LIGHT)
    setTheme(true)
    waitForIdle()
    paths += captureForDevice(outputDirectory, screenName, device, TMR_THEME_DARK)
    return paths
}

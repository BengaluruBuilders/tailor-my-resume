package com.tailormyresume.feature.onboarding.impl.upload

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFileResolver
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import com.tailormyresume.feature.onboarding.impl.reading.ReadingFileUi
import com.tailormyresume.feature.onboarding.impl.reading.ReadingRowKind
import com.tailormyresume.feature.onboarding.impl.reading.ReadingRowState
import com.tailormyresume.feature.onboarding.impl.reading.ReadingRowUi
import com.tailormyresume.feature.onboarding.impl.reading.ReadingUiState
import com.tailormyresume.feature.onboarding.impl.signin.captureSignIn
import com.tailormyresume.feature.onboarding.impl.signin.showSignIn

internal fun ComposeContentTestRule.showFlowScreen(fontScale: Float = 1f, content: @Composable () -> Unit) =
    showSignIn(fontScale, content)

internal fun ComposeContentTestRule.captureFlowScreen(name: String, device: TmrTestDevice = TmrTestDevices.prototype) =
    captureSignIn(name, device)

internal val ImageOnlyFailure = UploadFailure(UploadFailureKind.ImageOnly, "Resume_scan.pdf", RESUME_PDF_MIME, 412L * 1024L)

internal val NeutralFailure = UploadFailure(UploadFailureKind.Neutral, "Priya_Deshmukh_Resume.pdf", RESUME_PDF_MIME, 112L * 1024L)

internal val UnseparatedNameFailure = UploadFailure(UploadFailureKind.Neutral, "PriyaDeshmukhResume2026.pdf", RESUME_PDF_MIME, 112L * 1024L)

internal val SampleReadingFile = ReadingFileUi("Priya_Deshmukh_Resume.pdf", RESUME_PDF_MIME, 112L * 1024L, 0)

internal fun readingStateAt(percent: Int): ReadingUiState {
    val kinds = ReadingRowKind.entries
    val counts = listOf(3, 3, 1, 12, 6)
    val rows = kinds.mapIndexed { index, kind ->
        when (percent) {
            0 -> ReadingRowUi(kind, if (index == 0) ReadingRowState.Active else ReadingRowState.Pending)
            100 -> ReadingRowUi(kind, ReadingRowState.Done, counts[index])
            else -> ReadingRowUi(kind, ReadingRowState.Active)
        }
    }
    return ReadingUiState(SampleReadingFile, percent, rows)
}

internal object NoopResumeFileResolver : ResumeFileResolver {
    override suspend fun resolve(uri: Uri): ResumeFile = ResumeFile("cv.pdf", RESUME_PDF_MIME, 2_048L, uri.toString())
}

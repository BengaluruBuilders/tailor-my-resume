package com.tailormyresume.feature.onboarding.impl.upload

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeSource
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnreadableViewModelTest {

    private val file = ResumeFile("scan.pdf", RESUME_PDF_MIME, 2_048L, "content://scan")

    @Test
    fun showsTheFailureHeldByTheDraftAndNeutralWhenNone() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UnreadableViewModel(draft, NoopResumeFileResolver)
        assertThat(viewModel.uiState.value).isEqualTo(UploadFailure(UploadFailureKind.Neutral))

        val failure = UploadFailure(UploadFailureKind.ImageOnly, "scan.pdf", RESUME_PDF_MIME, 2_048L)
        draft.fail(failure)

        assertThat(viewModel.uiState.value).isEqualTo(failure)
    }

    @Test
    fun pickReplacesWithReading() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UnreadableViewModel(draft, NoopResumeFileResolver)

        viewModel.events.test {
            viewModel.onFilePicked(file)

            assertThat(awaitItem()).isEqualTo(UnreadableEvent.OpenReading)
        }
        assertThat(draft.source.value).isEqualTo(ResumeSource.PickedFile(file))
    }

    @Test
    fun cancelledPickDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UnreadableViewModel(draft, NoopResumeFileResolver)

        viewModel.events.test {
            viewModel.onFilePicked(null)

            expectNoEvents()
        }
    }

    @Test
    fun pasteReplacesWithPasteResume() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UnreadableViewModel(draft, NoopResumeFileResolver)

        viewModel.events.test {
            viewModel.onPasteAsText()

            assertThat(awaitItem()).isEqualTo(UnreadableEvent.OpenPaste)
        }
    }
}

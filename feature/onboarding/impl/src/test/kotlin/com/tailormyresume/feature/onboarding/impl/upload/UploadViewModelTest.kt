package com.tailormyresume.feature.onboarding.impl.upload

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.feature.onboarding.impl.importresume.RESUME_PDF_MIME
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    private val file = ResumeFile("cv.pdf", RESUME_PDF_MIME, 2_048L, "content://cv")

    @Test
    fun pickedFileGoesToDraftAndNavigatesToReading() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UploadViewModel(draft, NoopResumeFileResolver)

        viewModel.events.test {
            viewModel.onFilePicked(file)

            assertThat(awaitItem()).isEqualTo(UploadEvent.OpenReading)
        }
        assertThat(draft.source.value).isEqualTo(ResumeSource.PickedFile(file))
    }

    @Test
    fun cancelledPickDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(TestSessionRepository(), backgroundScope)
        val viewModel = UploadViewModel(draft, NoopResumeFileResolver)

        viewModel.events.test {
            viewModel.onFilePicked(null)

            expectNoEvents()
        }
        assertThat(draft.source.value).isNull()
    }
}

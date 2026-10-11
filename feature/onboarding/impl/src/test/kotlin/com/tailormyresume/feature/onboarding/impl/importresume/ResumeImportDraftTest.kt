package com.tailormyresume.feature.onboarding.impl.importresume

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeImportDraftTest {

    private val session = TestSessionRepository()
    private val file = ResumeFile("cv.pdf", RESUME_PDF_MIME, 1_000L, "content://cv")

    @Test
    fun consumeReturnsTheSourceOnceThenNull() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(session, backgroundScope)
        draft.setFile(file)

        assertThat(draft.consumeSource()).isEqualTo(ResumeSource.PickedFile(file))
        assertThat(draft.consumeSource()).isNull()
    }

    @Test
    fun settingANewSourceClearsThePreviousFailure() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(session, backgroundScope)
        draft.fail(UploadFailure(UploadFailureKind.FileProblem, fileName = "cv.pdf"))
        assertThat(draft.failure.value?.fileName).isEqualTo("cv.pdf")

        draft.setText("pasted resume text")

        assertThat(draft.failure.value).isNull()
        assertThat(draft.source.value).isEqualTo(ResumeSource.PastedText("pasted resume text"))
    }

    @Test
    fun failureDropsThePendingSourceAndKeepsOnlyTheName() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(session, backgroundScope)
        draft.setText("pasted resume text")

        draft.fail(UploadFailure(UploadFailureKind.Neutral))

        assertThat(draft.source.value).isNull()
        assertThat(draft.failure.value).isEqualTo(UploadFailure(UploadFailureKind.Neutral))
    }

    @Test
    fun accountChangeClearsSourceAndFailure() = runTest(UnconfinedTestDispatcher()) {
        val draft = ResumeImportDraft(session, backgroundScope)
        draft.setFile(file)
        draft.fail(UploadFailure(UploadFailureKind.TooLarge, fileName = "cv.pdf"))
        draft.setText("pasted resume text")

        session.sendAccount(SignInAccount("a1", "A", "a@example.com"))

        assertThat(draft.source.value).isNull()
        assertThat(draft.failure.value).isNull()
    }

    @Test
    fun theCurrentAccountAtCreationDoesNotClearAPickedFile() = runTest(UnconfinedTestDispatcher()) {
        session.sendAccount(SignInAccount("a1", "A", "a@example.com"))
        val draft = ResumeImportDraft(session, backgroundScope)
        draft.setFile(file)

        assertThat(draft.source.value).isEqualTo(ResumeSource.PickedFile(file))
    }
}

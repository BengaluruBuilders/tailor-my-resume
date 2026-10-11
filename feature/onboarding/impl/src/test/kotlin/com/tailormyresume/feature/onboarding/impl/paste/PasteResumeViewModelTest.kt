package com.tailormyresume.feature.onboarding.impl.paste

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.feature.onboarding.impl.importresume.MAX_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.MIN_PASTED_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PasteResumeViewModelTest {

    private fun draftIn(scope: CoroutineScope) = ResumeImportDraft(TestSessionRepository(), scope)

    @Test
    fun readDisabledBelow50TrimmedChars() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = PasteResumeViewModel(draftIn(backgroundScope))

        viewModel.onTextChange("a".repeat(MIN_PASTED_RESUME_CHARS - 1))
        assertThat(viewModel.uiState.value.canRead).isFalse()

        viewModel.onTextChange("   " + "a".repeat(MIN_PASTED_RESUME_CHARS - 1) + "   ")
        assertThat(viewModel.uiState.value.canRead).isFalse()
    }

    @Test
    fun enabledAtExactly50() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = PasteResumeViewModel(draftIn(backgroundScope))

        viewModel.onTextChange("  " + "a".repeat(MIN_PASTED_RESUME_CHARS) + "  ")

        assertThat(viewModel.uiState.value.canRead).isTrue()
    }

    @Test
    fun inputCappedAt30000() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = PasteResumeViewModel(draftIn(backgroundScope))

        viewModel.onTextChange("a".repeat(MAX_RESUME_CHARS + 500))

        assertThat(viewModel.uiState.value.text).hasLength(MAX_RESUME_CHARS)
    }

    @Test
    fun readBelowTheMinimumDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        val draft = draftIn(backgroundScope)
        val viewModel = PasteResumeViewModel(draft)
        viewModel.onTextChange("too short")

        viewModel.events.test {
            viewModel.onRead()

            expectNoEvents()
        }
        assertThat(draft.source.value).isNull()
    }

    @Test
    fun readReplacesWithReading() = runTest(UnconfinedTestDispatcher()) {
        val draft = draftIn(backgroundScope)
        val viewModel = PasteResumeViewModel(draft)
        val text = "Priya Deshmukh. " + "Business analyst at Infosys. ".repeat(3)
        viewModel.onTextChange(text)

        viewModel.events.test {
            viewModel.onRead()

            assertThat(awaitItem()).isEqualTo(PasteResumeEvent.OpenReading)
        }
        assertThat(draft.source.value).isEqualTo(ResumeSource.PastedText(text))
    }
}

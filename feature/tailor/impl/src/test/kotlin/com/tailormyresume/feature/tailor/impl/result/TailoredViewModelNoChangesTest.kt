package com.tailormyresume.feature.tailor.impl.result

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AcceptChangesUseCase
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TailoredViewModelNoChangesTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val unchanged = testBullet("b1", original = "Built an internal tool", proposed = "Built an internal tool")

    @Test
    fun aRunWithNoChangesNeverShowsChangesAccepted() = runTest {
        val viewModel = TailoredViewModel(
            applicationRepository = applicationRepository,
            profileRepository = profileRepository,
            assembler = ResumeDocumentAssembler(TestResumeHeadings),
            applicationId = "app-1",
        )
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.state.collect() }
        applicationRepository.sendApplications(listOf(testApplication(listOf(unchanged))))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", unchanged))))

        AcceptChangesUseCase(applicationRepository, profileRepository, TestClock())("app-1")

        val ready = viewModel.state.value as TailoredUiState.Ready
        assertThat(ready.changes).isEmpty()
        assertThat(ready.accepted).isFalse()
        assertThat(ready.exportEnabled).isTrue()
    }
}

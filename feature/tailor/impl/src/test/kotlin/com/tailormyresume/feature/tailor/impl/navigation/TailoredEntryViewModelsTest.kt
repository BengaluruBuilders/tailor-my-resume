package com.tailormyresume.feature.tailor.impl.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.api.navigation.ExportedNavKey
import com.tailormyresume.feature.tailor.impl.TailorEvent
import com.tailormyresume.feature.tailor.impl.TailorViewModel
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.result.TailoredUiState
import com.tailormyresume.feature.tailor.impl.result.TailoredViewModel
import com.tailormyresume.feature.tailor.impl.testApplication
import com.tailormyresume.feature.tailor.impl.testBullet
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class TailoredEntryViewModelsTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val clock = TestClock()
    private val bullet = testBullet("b1", original = "Built an internal tool", proposed = "Developed an internal tool")

    private val factory = tailorEntryViewModelFactory(applicationRepository, profileRepository, clock)

    private fun hostBoth(): Pair<TailorViewModel, TailoredViewModel> {
        val provider = ViewModelProvider.create(ViewModelStore(), factory)
        val tailor = provider[TailorViewModelKeys.tailor("app-1"), TailorViewModel::class]
        val tailored = provider[TailorViewModelKeys.tailored("app-1"), TailoredViewModel::class]
        return tailor to tailored
    }

    private fun seed() {
        applicationRepository.sendApplications(listOf(testApplication(listOf(bullet), entryIds = listOf("exp-1"))))
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))))
    }

    @Test
    fun hostingBothEntryViewModelsLeavesTailorViewModelAlive() {
        val (tailor, _) = hostBoth()

        assertThat(tailor.viewModelScope.isActive).isTrue()
    }

    @Test
    fun undoAcceptAndExportReactWhenBothViewModelsShareTheEntryStore() = runTest {
        seed()
        val (tailor, tailored) = hostBoth()
        tailored.state.onEach { }.launchIn(backgroundScope)

        tailor.onUndoChange("b1")
        val rejected = applicationRepository.observeApplication("app-1").first()?.tailoredResume?.bullets?.single()
        assertThat(rejected?.decision).isEqualTo(BulletDecision.REJECTED)

        tailor.onAcceptChanges()
        assertThat(applicationRepository.observeApplication("app-1").first()?.changesAcceptedAt).isNotNull()
        val ready = tailored.state.first { it is TailoredUiState.Ready } as TailoredUiState.Ready
        assertThat(ready.exportEnabled).isTrue()

        tailor.events.test {
            tailor.onExportTapped()
            assertThat(awaitItem()).isEqualTo(TailorEvent.Navigate(ExportedNavKey("app-1")))
            cancelAndIgnoreRemainingEvents()
        }
    }
}

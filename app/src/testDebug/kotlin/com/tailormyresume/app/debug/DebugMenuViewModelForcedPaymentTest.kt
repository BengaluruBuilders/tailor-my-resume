package com.tailormyresume.app.debug

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.ForcedPaymentScenario
import com.tailormyresume.core.model.DebugScenario
import com.tailormyresume.core.testing.sample.TestSampleDataController
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DebugMenuViewModelForcedPaymentTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val forced = ForcedPaymentScenario()
    private fun viewModel() = DebugMenuViewModel(TestSampleDataController(), forced)

    @Test
    fun openingCreditsForcesTheChosenScenario() = runTest {
        viewModel().openPreview(DebugScenarioTarget.Credits, DebugScenario.PENDING) {}

        assertThat(forced.scenario).isEqualTo(DebugScenario.PENDING)
    }

    @Test
    fun otherScreensForceNothing() = runTest {
        viewModel().openPreview(DebugScenarioTarget.Profile, DebugScenario.PENDING) {}

        assertThat(forced.scenario).isEqualTo(DebugScenario.DEFAULT)
    }

    @Test
    fun closingThePreviewClearsTheForcedScenario() = runTest {
        val subject = viewModel()
        subject.openPreview(DebugScenarioTarget.Credits, DebugScenario.FAILED) {}

        subject.closePreview(DebugScenarioTarget.Credits)

        assertThat(forced.scenario).isEqualTo(DebugScenario.DEFAULT)
    }

    @Test
    fun closingThePreviewIsSeenByCollectorsThatAreAlreadyRunning() = runTest {
        val subject = viewModel()
        subject.openPreview(DebugScenarioTarget.Credits, DebugScenario.PENDING) {}
        val seen = mutableListOf<DebugScenario>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { forced.scenarios.collect { seen += it } }

        subject.closePreview(DebugScenarioTarget.Credits)
        collecting.cancel()

        assertThat(seen).containsExactly(DebugScenario.PENDING, DebugScenario.DEFAULT).inOrder()
    }

    @Test
    fun releasingAndReforcingTheScenarioSurvivesAnExternalActivityCoveringThePreview() = runTest {
        val subject = viewModel()
        subject.openPreview(DebugScenarioTarget.Credits, DebugScenario.PENDING) {}

        subject.releasePaymentScenario()
        assertThat(forced.scenario).isEqualTo(DebugScenario.DEFAULT)

        subject.forcePaymentScenario(DebugScenarioTarget.Credits, DebugScenario.PENDING)
        assertThat(forced.scenario).isEqualTo(DebugScenario.PENDING)
    }

    @Test
    fun reforcingForAScreenThatForcesNothingKeepsTheDefault() = runTest {
        viewModel().forcePaymentScenario(DebugScenarioTarget.Profile, DebugScenario.PENDING)

        assertThat(forced.scenario).isEqualTo(DebugScenario.DEFAULT)
    }
}

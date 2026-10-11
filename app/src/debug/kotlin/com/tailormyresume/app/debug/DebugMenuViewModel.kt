package com.tailormyresume.app.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.offline.ForcedPaymentScenario
import com.tailormyresume.core.domain.sample.SampleDataController
import com.tailormyresume.core.model.DebugScenario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebugDataMessage { SampleLoaded, DataReset, Failed }

data class DebugMenuUiState(
    val busy: Boolean = false,
    val message: DebugDataMessage? = null,
)

@HiltViewModel
class DebugMenuViewModel @Inject constructor(
    private val sampleDataController: SampleDataController,
    private val forcedPaymentScenario: ForcedPaymentScenario = ForcedPaymentScenario(),
) : ViewModel() {

    private val work = MutableStateFlow(DebugMenuUiState())

    val uiState: StateFlow<DebugMenuUiState> = work

    fun loadSampleData() = run(DebugDataMessage.SampleLoaded) { sampleDataController.load() }

    fun resetAppData() = run(DebugDataMessage.DataReset) { sampleDataController.reset() }

    fun openPreview(target: DebugScenarioTarget, scenario: DebugScenario = DebugScenario.defaultValue, onReady: () -> Unit) {
        viewModelScope.launch {
            if (target.needsSampleJob) sampleDataController.keepSampleJobDescription()
            forcePaymentScenario(target, scenario)
            onReady()
        }
    }

    fun forcePaymentScenario(target: DebugScenarioTarget, scenario: DebugScenario) {
        forcedPaymentScenario.scenario = if (target.forcesPayment) scenario else DebugScenario.defaultValue
    }

    fun releasePaymentScenario() {
        forcedPaymentScenario.scenario = DebugScenario.defaultValue
    }

    fun closePreview(target: DebugScenarioTarget) {
        releasePaymentScenario()
        if (target.needsSampleJob) viewModelScope.launch { sampleDataController.clearSampleJobDescription() }
    }

    private fun run(success: DebugDataMessage, action: suspend () -> Unit) {
        if (work.value.busy) return
        work.update { DebugMenuUiState(busy = true) }
        viewModelScope.launch {
            val message = try {
                action()
                success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DebugDataMessage.Failed
            }
            work.update { DebugMenuUiState(message = message) }
        }
    }
}

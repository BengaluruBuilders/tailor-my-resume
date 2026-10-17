package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.model.DebugScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForcedWalletScenario @Inject constructor() {
    private val current = MutableStateFlow(DebugScenario.DEFAULT)

    val scenarios: StateFlow<DebugScenario> = current

    var scenario: DebugScenario
        get() = current.value
        set(value) {
            current.value = value
        }
}

package com.tailormyresume.core.testing.gateway

import com.tailormyresume.core.domain.ReferralGateway
import com.tailormyresume.core.domain.ReferralUiState
import kotlinx.coroutines.flow.MutableStateFlow

class TestReferralGateway : ReferralGateway {
    override val state = MutableStateFlow<ReferralUiState>(ReferralUiState.Content())
    val actions = mutableListOf<String>()
    override suspend fun refresh() { actions += "refresh" }
    override suspend fun save(code: String, replace: Boolean) { actions += "save:$code:$replace" }
    override suspend fun clear() { actions += "clear" }
    override suspend fun issue() { actions += "issue" }
    override suspend fun apply() { actions += "apply" }
    override suspend fun acceptTransfer() { actions += "review" }
}

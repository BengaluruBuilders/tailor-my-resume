package com.tailormyresume.core.testing.gateway

import com.tailormyresume.core.domain.GuestTrialGateway
import com.tailormyresume.core.domain.GuestTrialOffer
import com.tailormyresume.core.domain.GuestTrialResult

class TestGuestTrialGateway : GuestTrialGateway {
    var offer: GuestTrialOffer = GuestTrialOffer.Available(2)
    var result: GuestTrialResult = GuestTrialResult.Granted(2)
    var prepares = 0
    var claims = 0
    var continues = 0
    var beforeClaim: suspend () -> Unit = {}
    override suspend fun prepare(): GuestTrialOffer {
        prepares++
        return offer
    }
    override suspend fun claim(): GuestTrialResult {
        claims++
        beforeClaim()
        return result
    }
    override suspend fun continueAsGuest() {
        continues++
    }
}

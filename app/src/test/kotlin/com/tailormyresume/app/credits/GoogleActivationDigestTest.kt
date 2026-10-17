package com.tailormyresume.app.credits

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.dto.ActivationChallengeRequest
import com.tailormyresume.core.network.dto.GoogleActivationRequest
import com.tailormyresume.core.network.tailormyresumeJson
import org.junit.Test

class GoogleActivationDigestTest {
    @Test fun explicitActionSetAndNullIntentMatchFrozenVectors() {
        assertThat(activationDigest("GUEST_LINK", "44444444-4444-4444-8444-444444444444", null))
            .isEqualTo("0-G3Cfz6Oqj9L_rPf0gKg3yRc_HUTSE4nWVufO61AgM")
        assertThat(activationDigest("GUEST_LINK", null, null)).isEqualTo("E_4gRFpR2Co_c6-rA5jHhrHeU00nHbrAzj7Qu1PyD6M")
        assertThat(activationDigest("DIRECT_GOOGLE", null, "0123456789abcdef")).isEqualTo("RU3Wn6-pVXneW4fdmkjIS5_YemQgqwNZpwobbQFrPDE")
        assertThat(activationDigest("DIRECT_GOOGLE", null, null)).isEqualTo("eEJHBidZKola_AoxQM6fktE2kj7BPapAlpe3PZAd_XQ")
    }

    @Test fun addingBaseOrChangingSignalCannotReuseTheSameBusinessDigest() {
        val signup = activationDigest("DIRECT_GOOGLE", null, null)
        val base = activationDigest("DIRECT_GOOGLE", null, "0123456789abcdef")
        assertThat(base).isNotEqualTo(signup)
        assertThat(activationDigest("DIRECT_GOOGLE", null, "fedcba9876543210")).isNotEqualTo(base)
    }

    @Test fun nullRevisionAndUnionMembersArePresentInBothRequests() {
        val json = tailormyresumeJson()
        val challenge = json.encodeToString(ActivationChallengeRequest.serializer(), ActivationChallengeRequest("GOOGLE_ACTIVATION", "request", null, "trial-v1", "digest"))
        assertThat(challenge).contains("\"policyVersion\":null")
        val body = json.encodeToString(GoogleActivationRequest.serializer(), GoogleActivationRequest("request", null, null, "GUEST_LINK", null, null, "challenge", "token"))
        listOf("policyVersion", "trialPolicyVersion", "upgradeIntent", "continuity").forEach { field ->
            assertThat(body).contains("\"$field\":null")
        }
    }
}

package com.tailormyresume.app.credits

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

class GuestEvidenceTest {
    @Test fun businessDigestsMatchEveryFrozenServerVector() {
        val vectors = Json.parseToJsonElement(File("../docs/testing/earn-credits/2026-10-17/01-guest-trial/fixtures/hash-vectors.json").readText()).jsonObject
        vectors.getValue("positive").jsonArray.forEach { entry ->
            val vector = entry.jsonObject
            val signal = vector.getValue("businessPayload").jsonArray[2].jsonPrimitive.content
            assertThat(guestPayloadDigest(signal)).isEqualTo(vector.getValue("payloadDigest").jsonPrimitive.content)
        }
    }

    @Test fun signalValidationRejectsUnknownUppercaseMalformedAndKnownUnusableValues() {
        listOf("", "0000000000000000", "9774d56d682e549c", "0123456789ABCDEF", " 0123456789abcdef", "0123456789abcde").forEach {
            assertThat(usableAndroidId(it)).isFalse()
            assertThat(runCatching { guestPayloadDigest(it) }.isFailure).isTrue()
        }
    }
}

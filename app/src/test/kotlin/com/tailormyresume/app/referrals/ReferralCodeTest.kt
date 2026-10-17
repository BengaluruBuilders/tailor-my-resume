package com.tailormyresume.app.referrals

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

class ReferralCodeTest {
    @Test fun frozenNullSignalVectorsAndInvalidCodes() {
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }.first {
            File(it, "docs/testing/earn-credits/2026-10-17/03-manual-referrals/contract/vectors.json").isFile
        }
        val vectors = Json.parseToJsonElement(File(root, "docs/testing/earn-credits/2026-10-17/03-manual-referrals/contract/vectors.json").readText()).jsonObject
        vectors.getValue("valid").jsonArray.filter { it.jsonObject.getValue("continuity").toString() == "null" }.forEach {
            val value = it.jsonObject
            val code = canonicalReferralCode(value.getValue("rawCode").jsonPrimitive.content, "staging")
            assertThat(code).isEqualTo(value.getValue("canonicalCode").jsonPrimitive.content)
            assertThat(referralDigest(checkNotNull(code), value.getValue("source").jsonPrimitive.content)).isEqualTo(value.getValue("payloadDigest").jsonPrimitive.content)
        }
        vectors.getValue("invalidCodes").jsonArray.forEach { assertThat(canonicalReferralCode(it.jsonPrimitive.content, "staging")).isNull() }
    }
}

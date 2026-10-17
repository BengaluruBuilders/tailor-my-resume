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
            val code = canonicalReferralCode(value.getValue("rawCode").jsonPrimitive.content)
            assertThat(code).isEqualTo(value.getValue("canonicalCode").jsonPrimitive.content)
            assertThat(referralDigest(checkNotNull(code), value.getValue("source").jsonPrimitive.content)).isEqualTo(value.getValue("payloadDigest").jsonPrimitive.content)
        }
        vectors.getValue("invalidCodes").jsonArray.forEach {
            val raw = it.jsonPrimitive.content
            val syntax = canonicalReferralCode(raw)
            if (raw.startsWith("tailormyresume.production.") || raw.startsWith("t0ilormyresume.staging.")) {
                assertThat(syntax).isEqualTo(raw)
            } else {
                assertThat(syntax).isNull()
            }
        }
    }

    @Test fun configuredPrefixesRemainWholeAndDoNotChangeCanonicalIdentity() {
        val code = "resume-demo.preview-2.abcdefghijklmnopqrstuvwxyz234567"
        assertThat(canonicalReferralCode(" \t${code.uppercase(java.util.Locale.ROOT)}\r\n")).isEqualTo(code)
        assertThat(canonicalReferralCode("foreign-app.other-environment.abcdefghijklmnopqrstuvwxyz234567")).isNotNull()
        assertThat(referralDigest(code)).isNotEqualTo(referralDigest("tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567"))
        listOf(
            "https://example.invalid/$code",
            "$code.extra",
            code.replace('.', '/'),
            "é$code",
            "a.b.abc",
            " ".repeat(257) + code,
            "1resume.preview-2.abcdefghijklmnopqrstuvwxyz234567",
            "a".repeat(33) + ".b.abcdefghijklmnopqrstuvwxyz234567",
        ).forEach {
            assertThat(canonicalReferralCode(it)).isNull()
        }
    }
}

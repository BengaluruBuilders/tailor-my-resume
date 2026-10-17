package com.tailormyresume.app.referrals

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

class ReferralLinksTest {
    @Test fun frozenTransportVectorsUseOneStrictParser() {
        val name = "docs/testing/earn-credits/2026-10-17/04-referral-links/contract/transport-vectors.json"
        val root = generateSequence(File(".").absoluteFile) { it.parentFile }.first { File(it, name).isFile }
        val fixture = Json.parseToJsonElement(File(root, name).readText()).jsonObject
        val links = ReferralLinks(fixture.getValue("origin").jsonPrimitive.content)
        listOf("url" to links::parseUrl, "referrer" to { raw: String -> links.parseReferrer(raw) }).forEach { (type, parse) ->
            fixture.getValue("${type}Valid").jsonArray.forEach {
                val value = it.jsonObject
                assertThat(parse(value.getValue("raw").jsonPrimitive.content)).isEqualTo(value.getValue("code").jsonPrimitive.content)
            }
            fixture.getValue("${type}Invalid").jsonArray.forEach {
                val value = it.jsonObject
                assertThat(parse(value.getValue("raw").jsonPrimitive.content)).isNull()
            }
        }
    }

    @Test fun disabledAndForeignShareUrlsNeverExpandTrust() {
        val code = "tailormyresume.staging.abcdefghijklmnopqrstuvwxyz234567"
        val url = "https://links.example.invalid/r/$code"
        assertThat(ReferralLinks("").parseUrl(url)).isNull()
        assertThat(ReferralLinks("").parseReferrer("tmr_referral=$code")).isNull()
        val links = ReferralLinks("https://links.example.invalid")
        assertThat(links.shareUrl(url, code)).isEqualTo(url)
        assertThat(links.shareUrl(url, code.replace("staging", "production"))).isNull()
        assertThat(links.shareUrl(url.replace("links.", "foreign."), code)).isNull()
        listOf("http://links.example.invalid", "https://127.0.0.1", "https://links.local", "https://LINKS.example.invalid", "https://links.example.invalid/", "https://links.example.invalid:443").forEach {
            assertThat(ReferralLinks(it).enabled).isFalse()
        }
    }
}

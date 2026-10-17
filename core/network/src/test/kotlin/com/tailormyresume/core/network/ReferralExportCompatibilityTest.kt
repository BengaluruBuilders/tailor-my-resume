package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.dto.ServerExportResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class ReferralExportCompatibilityTest {
    private val json = tailormyresumeJson()

    @Test
    fun fullStableLedgerAndOwnReferralReceiptSurviveArchiveReencoding() {
        val legacy = Json.parseToJsonElement(checkNotNull(javaClass.getResource("/contract/me-export-response.json")).readText()).jsonObject.getValue("export").jsonObject
        val credits = Json.parseToJsonElement("""[{"id":"stable-referral-ledger","kind":"REFERRAL_REWARD","amount":13,"createdAt":"2026-10-17T00:00:00Z","applicationId":null,"productId":null}]""")
        val promotions = Json.parseToJsonElement("""{"referrals":{"code":null,"attachment":{"id":"own-receipt","status":"ATTACHED","source":"MANUAL","attachedAt":"2026-10-17T00:00:00Z","reward":{"recipient":"INVITER","amount":13,"status":"GRANTED","policyVersion":"referral-v1"}},"stats":{"creditedCount":0,"earnedCredits":0}}}""")
        val body = JsonObject(mapOf("export" to JsonObject(legacy + mapOf("credits" to credits, "promotions" to promotions))))
        val decoded = json.decodeFromJsonElement(ServerExportResponse.serializer(), body)
        assertThat(decoded.export.credits?.single()?.id).isEqualTo("stable-referral-ledger")
        val encoded = json.encodeToJsonElement(ServerExportResponse.serializer(), decoded).jsonObject.getValue("export").jsonObject
        assertThat(encoded.getValue("credits").jsonArray.single().jsonObject.getValue("id")).isEqualTo(credits.jsonArray.single().jsonObject.getValue("id"))
        assertThat(encoded.getValue("promotions")).isEqualTo(promotions)
    }
}

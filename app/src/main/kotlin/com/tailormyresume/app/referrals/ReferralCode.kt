package com.tailormyresume.app.referrals

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Base64

internal fun canonicalReferralCode(raw: String): String? {
    if (raw.length !in 1..256 || raw.any { it.code > 127 }) return null
    val code = raw.trim(' ', '\t', '\r', '\n').lowercase(java.util.Locale.ROOT)
    val pattern = Regex("[a-z][a-z0-9-]{0,31}\\.[a-z][a-z0-9-]{0,31}\\.[a-z2-7]{32}")
    return code.takeIf { it.length <= 98 && pattern.matches(it) }
}

internal fun referralDigest(code: String, source: String = "MANUAL"): String {
    val values = listOf("TMR_REFERRAL_ATTACH_PAYLOAD_V1", code, source, "", "")
    val bytes = Json.encodeToString(ListSerializer(String.serializer()), values).toByteArray(Charsets.UTF_8)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes))
}

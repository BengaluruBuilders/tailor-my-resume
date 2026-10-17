package com.tailormyresume.app.credits

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Base64

internal fun activationDigest(path: String, intentId: String?, continuity: String?): String {
    val payload = listOf(
        "TMR_GOOGLE_ACTIVATION_PAYLOAD_V1", path, intentId.orEmpty(),
        if (continuity == null) "SIGNUP_ONLY" else "SIGNUP_AND_BASE",
        if (continuity == null) "" else "ANDROID_ID", continuity.orEmpty(),
    )
    val bytes = Json.encodeToString(ListSerializer(String.serializer()), payload).toByteArray(Charsets.UTF_8)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes))
}

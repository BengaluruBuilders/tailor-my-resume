package com.tailormyresume.app.auth

import java.security.MessageDigest
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Local routing hint only; Firebase still validates the credential before any account handoff. */
internal object GoogleCredentialIdentity {
    fun digest(token: String): String {
        val payload = token.split('.').getOrNull(1) ?: error("Google credential is unavailable")
        val subject = Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8))
            .jsonObject["sub"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: error("Google credential is unavailable")
        return digestSubject(subject)
    }

    fun digestSubject(subject: String): String = MessageDigest.getInstance("SHA-256")
        .digest(subject.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

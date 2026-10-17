package com.tailormyresume.app.referrals

import com.tailormyresume.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferralLinks @Inject constructor() {
    internal var origin: String = BuildConfig.TAILORMYRESUME_REFERRAL_ORIGIN
        private set
    internal constructor(origin: String) : this() {
        this.origin = origin
    }
    val enabled: Boolean get() = validReferralOrigin(origin)

    fun parseUrl(raw: String?): String? {
        if (!enabled || raw == null || raw.length !in 1..512 || raw.any { it.code !in 33..126 || it in "%\\?#" }) return null
        val prefix = "$origin/r/"
        if (!raw.startsWith(prefix)) return null
        val value = raw.removePrefix(prefix)
        return canonicalReferralCode(value)?.takeIf { value.equals(it, ignoreCase = true) }
    }

    fun parseReferrer(raw: String): String? {
        if (!enabled || raw.length !in 1..1024 || raw.any { it.code !in 33..126 || it in "%&+" }) return null
        val prefix = "tmr_referral="
        if (!raw.startsWith(prefix)) return null
        val value = raw.removePrefix(prefix)
        return canonicalReferralCode(value)?.takeIf { value.equals(it, ignoreCase = true) }
    }

    fun shareUrl(raw: String?, code: String): String? = raw?.takeIf { parseUrl(it) == code && it == "$origin/r/$code" }
}

internal fun validReferralOrigin(origin: String): Boolean {
    if (origin.length > 253 || !origin.startsWith("https://")) return false
    val host = origin.removePrefix("https://")
    if (!Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+").matches(host)) return false
    if (host.split('.').any { it.length > 63 } || host.all { it.isDigit() || it == '.' }) return false
    return listOf("local", "localhost", "internal", "flycast").none { host == it || host.endsWith(".$it") }
}

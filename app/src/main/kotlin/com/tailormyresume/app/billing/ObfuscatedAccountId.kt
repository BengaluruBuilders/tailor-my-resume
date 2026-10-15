package com.tailormyresume.app.billing

import java.security.MessageDigest

fun obfuscatedAccountId(firebaseUid: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(firebaseUid.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

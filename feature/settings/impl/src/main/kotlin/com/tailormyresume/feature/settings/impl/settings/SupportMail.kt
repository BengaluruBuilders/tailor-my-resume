package com.tailormyresume.feature.settings.impl.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun supportMailIntent(address: String, subject: String): Intent? {
    if (address.isBlank()) return null
    return Intent(Intent.ACTION_SENDTO, Uri.fromParts(MAILTO_SCHEME, address.trim(), null))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
}

internal fun Context.openSupportMail(address: String, subject: String): Boolean {
    val intent = supportMailIntent(address, subject) ?: return false
    return try {
        startActivity(intent)
        true
    } catch (missing: ActivityNotFoundException) {
        false
    }
}

private const val MAILTO_SCHEME = "mailto"

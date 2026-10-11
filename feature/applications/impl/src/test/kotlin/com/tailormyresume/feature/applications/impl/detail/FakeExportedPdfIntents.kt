package com.tailormyresume.feature.applications.impl.detail

import android.content.Intent
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import kotlinx.coroutines.delay

internal class FakeExportedPdfIntents(
    var share: Intent? = null,
    var view: Intent? = null,
    var latencyMs: Long = 0L,
) : ExportedPdfIntents {
    val shareRequests = mutableListOf<String>()
    val viewRequests = mutableListOf<String>()

    override suspend fun shareIntent(applicationId: String): Intent? {
        shareRequests += applicationId
        delay(latencyMs)
        return share
    }

    override suspend fun viewIntent(applicationId: String): Intent? {
        viewRequests += applicationId
        delay(latencyMs)
        return view
    }
}

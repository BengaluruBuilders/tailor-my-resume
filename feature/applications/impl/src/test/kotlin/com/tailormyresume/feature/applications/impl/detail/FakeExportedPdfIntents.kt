package com.tailormyresume.feature.applications.impl.detail

import android.content.Intent
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents

internal class FakeExportedPdfIntents(
    var share: Intent? = null,
    var view: Intent? = null,
) : ExportedPdfIntents {
    val shareRequests = mutableListOf<String>()
    val viewRequests = mutableListOf<String>()

    override suspend fun shareIntent(applicationId: String): Intent? {
        shareRequests += applicationId
        return share
    }

    override suspend fun viewIntent(applicationId: String): Intent? {
        viewRequests += applicationId
        return view
    }
}

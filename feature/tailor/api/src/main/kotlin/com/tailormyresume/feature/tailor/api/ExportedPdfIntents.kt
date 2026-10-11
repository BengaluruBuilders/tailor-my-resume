package com.tailormyresume.feature.tailor.api

import android.content.Intent

interface ExportedPdfIntents {
    suspend fun shareIntent(applicationId: String): Intent?

    suspend fun viewIntent(applicationId: String): Intent?
}

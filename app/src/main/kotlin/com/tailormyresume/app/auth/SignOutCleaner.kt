package com.tailormyresume.app.auth

import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.billing.RemotePaymentGateway
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.PendingReportQueue
import com.tailormyresume.core.domain.account.ExportedFiles
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignOutCleaner @Inject constructor(
    private val payments: RemotePaymentGateway,
    private val analysis: RemoteJobAnalysisSource,
    private val reports: PendingReportQueue,
    private val store: MockStateStore,
    private val credits: CreditsRepository,
    private val exportedFiles: ExportedFiles = ExportedFiles.None,
) {
    suspend fun clear() {
        payments.clearCredits()
        credits.clear()
        analysis.clear()
        reports.clear()
        store.removeWithPrefix(TAILORING_REQUEST_PREFIX)
        exportedFiles.deleteAll()
    }

    private companion object {
        const val TAILORING_REQUEST_PREFIX = "tailoring.request."
    }
}

package com.tailormyresume.core.domain.account

import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.model.SignInAccount
import java.io.File
import kotlin.time.Instant

data class AccountData(
    val generatedAt: Instant,
    val account: SignInAccount?,
    val profile: CandidateProfile?,
    val applications: List<JobApplication>,
    val entitlement: PurchaseEntitlement,
    val purchases: List<PurchaseRecord>,
    val exports: List<ExportRecord>,
    val creditLedger: List<CreditLedgerEntry> = emptyList(),
    val resumeSettings: ResumeSettings = ResumeSettings(),
)

data class AccountDataArchive(val fileName: String, val file: File)

interface AccountDataExporter {
    suspend fun export(data: AccountData): AccountDataArchive
}

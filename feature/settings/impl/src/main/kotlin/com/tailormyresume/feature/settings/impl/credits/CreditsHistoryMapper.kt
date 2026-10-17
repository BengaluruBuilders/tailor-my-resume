package com.tailormyresume.feature.settings.impl.credits

import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.JobApplication
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal sealed interface HistoryTitle {
    data object ReferralReward : HistoryTitle
    data object Unknown : HistoryTitle
    data object GoogleSignup : HistoryTitle
    data object GuestTrial : HistoryTitle

    data object FreeFirstResume : HistoryTitle

    data class BoughtCredits(val credits: Int) : HistoryTitle

    data class Tailored(val jobTitle: String) : HistoryTitle

    data object TailoredResume : HistoryTitle

    data object Refund : HistoryTitle

    data object CarriedOver : HistoryTitle
}

internal sealed interface HistoryDetail {
    data object Welcome : HistoryDetail

    data class Text(val value: String) : HistoryDetail

    data object None : HistoryDetail
}

internal data class CreditsHistoryRow(
    val key: String,
    val title: HistoryTitle,
    val detail: HistoryDetail,
    val date: String,
    val amount: Int,
) {
    val amountText: String
        get() = if (amount > 0) "+$amount" else "−${-amount}"
}

internal fun toHistoryRows(
    entries: List<CreditLedgerEntry>,
    applications: List<JobApplication>,
    zone: ZoneId,
): List<CreditsHistoryRow> {
    val applicationsById = applications.associateBy { it.id }
    val dateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH).withZone(zone)
    val occurrences = mutableMapOf<String, Int>()
    return entries.sortedByDescending { it.createdAt }.map { entry ->
        val millis = entry.createdAt.toEpochMilliseconds()
        val identity = "$millis:${entry.kind}:${entry.applicationId}:${entry.productId}"
        val occurrence = (occurrences[identity] ?: 0) + 1
        occurrences[identity] = occurrence
        val (title, detail) = entry.describe(applicationsById)
        CreditsHistoryRow(
            key = entry.id ?: "$identity#$occurrence",
            title = title,
            detail = detail,
            date = dateFormat.format(Instant.ofEpochMilli(millis)),
            amount = entry.amount,
        )
    }
}

private fun CreditLedgerEntry.describe(
    applicationsById: Map<String, JobApplication>,
): Pair<HistoryTitle, HistoryDetail> = when (kind) {
    CreditLedgerKind.REFERRAL_REWARD -> HistoryTitle.ReferralReward to HistoryDetail.None
    CreditLedgerKind.UNKNOWN -> HistoryTitle.Unknown to HistoryDetail.None
    CreditLedgerKind.GOOGLE_SIGNUP -> HistoryTitle.GoogleSignup to HistoryDetail.None
    CreditLedgerKind.GUEST_TRIAL -> HistoryTitle.GuestTrial to HistoryDetail.None
    CreditLedgerKind.FREE_GRANT -> HistoryTitle.FreeFirstResume to HistoryDetail.Welcome
    CreditLedgerKind.PURCHASE -> HistoryTitle.BoughtCredits(amount) to HistoryDetail.None
    CreditLedgerKind.SPEND -> applicationId?.let(applicationsById::get)?.let { application ->
        HistoryTitle.Tailored(application.job.title) to
            (application.job.company.takeIf { it.isNotBlank() }?.let { HistoryDetail.Text(it) } ?: HistoryDetail.None)
    } ?: (HistoryTitle.TailoredResume to HistoryDetail.None)
    CreditLedgerKind.REFUND -> HistoryTitle.Refund to HistoryDetail.None
    CreditLedgerKind.MIGRATION -> HistoryTitle.CarriedOver to HistoryDetail.None
}

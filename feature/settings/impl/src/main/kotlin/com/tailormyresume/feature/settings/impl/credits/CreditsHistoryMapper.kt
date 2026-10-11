package com.tailormyresume.feature.settings.impl.credits

import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.JobApplication
import java.time.ZoneId

internal sealed interface HistoryTitle {
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
    packs: List<ApplicationPack>,
    zone: ZoneId,
): List<CreditsHistoryRow> = TODO()

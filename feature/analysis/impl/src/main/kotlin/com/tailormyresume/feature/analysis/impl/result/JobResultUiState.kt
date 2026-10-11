package com.tailormyresume.feature.analysis.impl.result

import com.tailormyresume.core.model.MatchStatus

internal data class MustHaveRow(
    val requirementId: String,
    val text: String,
    val status: MatchStatus,
    val reason: String?,
    val unclear: Boolean,
) {
    val marker: MustHaveMarkerKind
        get() = when {
            unclear -> MustHaveMarkerKind.UNCLEAR
            status == MatchStatus.MET -> MustHaveMarkerKind.MET
            status == MatchStatus.PARTIAL -> MustHaveMarkerKind.PARTIAL
            else -> MustHaveMarkerKind.GAP
        }
}

internal enum class MustHaveMarkerKind { MET, PARTIAL, GAP, UNCLEAR }

internal sealed interface JobResultUiState {
    data object Loading : JobResultUiState

    data object Unavailable : JobResultUiState

    data class Ready(
        val title: String,
        val company: String,
        val location: String,
        val now: Int,
        val upTo: Int,
        val have: List<String>,
        val missing: List<String>,
        val mustHaves: List<MustHaveRow>,
        val credits: Int,
        val asksQuestion: Boolean,
    ) : JobResultUiState {
        val keywordCount: Int get() = have.size + missing.size

        val subtitle: String get() = listOf(company, location).filter { it.isNotBlank() }.joinToString(" · ")
    }
}

internal sealed interface JobResultEvent {
    data class Paywall(val applicationId: String) : JobResultEvent

    data class QuickQuestion(val applicationId: String) : JobResultEvent

    data class Tailor(val applicationId: String) : JobResultEvent
}

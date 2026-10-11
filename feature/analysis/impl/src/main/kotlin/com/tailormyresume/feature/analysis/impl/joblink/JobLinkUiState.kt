package com.tailormyresume.feature.analysis.impl.joblink

internal data class JobLinkUiState(
    val link: String = "",
    val importing: Boolean = false,
)

internal sealed interface JobLinkEvent {
    data object Close : JobLinkEvent
}

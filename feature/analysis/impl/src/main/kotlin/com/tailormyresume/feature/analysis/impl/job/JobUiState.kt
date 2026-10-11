package com.tailormyresume.feature.analysis.impl.job

internal sealed interface JobUiState {
    val text: String

    data object Empty : JobUiState {
        override val text: String = ""
    }

    data class HasText(
        override val text: String,
        val detected: String? = null,
        val notAJobPost: Boolean = false,
    ) : JobUiState {
        val charCount: Int get() = text.length
    }

    data class Analyzing(
        override val text: String,
        val percent: Int,
    ) : JobUiState
}

internal sealed interface JobEvent {
    data class Analyzed(val applicationId: String) : JobEvent

    data class Imported(val host: String) : JobEvent

    data object AnalysisFailed : JobEvent
}

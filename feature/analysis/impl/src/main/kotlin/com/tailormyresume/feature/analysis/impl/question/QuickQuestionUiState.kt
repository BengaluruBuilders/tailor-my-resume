package com.tailormyresume.feature.analysis.impl.question

internal const val QUICK_DETAIL_MAX = 400

internal enum class QuickChoice(val takesDetail: Boolean) {
    YES_REGULARLY(true),
    A_FEW_TIMES(true),
    NOT_YET(false),
}

internal sealed interface QuickQuestionUiState {
    data object Loading : QuickQuestionUiState

    data object NoQuestion : QuickQuestionUiState

    data class Ready(
        val question: String,
        val why: String,
        val picked: QuickChoice?,
        val detail: String,
    ) : QuickQuestionUiState {
        val showDetail: Boolean get() = picked?.takesDetail == true

        val canContinue: Boolean get() = picked != null
    }
}

internal sealed interface QuickQuestionEvent {
    data object ToastPickAnswer : QuickQuestionEvent

    data class Tailor(val applicationId: String) : QuickQuestionEvent
}

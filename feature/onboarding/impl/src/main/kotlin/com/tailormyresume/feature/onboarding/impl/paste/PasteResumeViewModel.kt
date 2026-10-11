package com.tailormyresume.feature.onboarding.impl.paste

import androidx.lifecycle.ViewModel
import com.tailormyresume.feature.onboarding.impl.importresume.MAX_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.MIN_PASTED_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal data class PasteResumeUiState(val text: String = "", val canRead: Boolean = false)

internal enum class PasteResumeEvent { OpenReading }

@HiltViewModel
internal class PasteResumeViewModel @Inject constructor(
    private val draft: ResumeImportDraft,
) : ViewModel() {

    private val state = MutableStateFlow(PasteResumeUiState())

    private val eventChannel = Channel<PasteResumeEvent>(Channel.BUFFERED)

    val uiState: StateFlow<PasteResumeUiState> = state.asStateFlow()

    val events: Flow<PasteResumeEvent> = eventChannel.receiveAsFlow()

    fun onTextChange(text: String) {
        val capped = text.take(MAX_RESUME_CHARS)
        state.update { PasteResumeUiState(capped, capped.trim().length >= MIN_PASTED_RESUME_CHARS) }
    }

    fun onRead() {
        val current = state.value
        if (!current.canRead) return
        draft.setText(current.text)
        eventChannel.trySend(PasteResumeEvent.OpenReading)
    }
}

package com.tailormyresume.feature.analysis.impl.joblink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.domain.JobImporter
import com.tailormyresume.feature.analysis.impl.job.JobDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class JobLinkViewModel @Inject constructor(
    private val importer: JobImporter,
    private val draftStore: JobDraftStore,
) : ViewModel() {

    private val state = MutableStateFlow(JobLinkUiState())
    private val eventChannel = Channel<JobLinkEvent>(Channel.BUFFERED)

    val uiState: StateFlow<JobLinkUiState> = state

    val events: Flow<JobLinkEvent> = eventChannel.receiveAsFlow()

    fun onLinkChange(link: String) {
        state.update { it.copy(link = link) }
    }

    fun onImport() {
        val current = state.value
        if (current.importing) return
        val url = current.link.trim()
        if (!url.isImportableHttpsUrl()) {
            draftStore.markNotAJobPost()
            eventChannel.trySend(JobLinkEvent.Close)
            return
        }
        state.update { it.copy(importing = true) }
        viewModelScope.launch {
            var cancelled = false
            try {
                val job = importer.import(url)
                draftStore.set(text = job.jobText, importedFrom = job.sourceHost)
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            } catch (e: Exception) {
                draftStore.markNotAJobPost()
            } finally {
                state.update { it.copy(importing = false) }
                if (!cancelled) eventChannel.trySend(JobLinkEvent.Close)
            }
        }
    }

    private fun String.isImportableHttpsUrl(): Boolean =
        length <= MAX_LINK_CHARS &&
            startsWith(HTTPS_PREFIX, ignoreCase = true) &&
            length > HTTPS_PREFIX.length

    private companion object {
        const val HTTPS_PREFIX = "https://"
        const val MAX_LINK_CHARS = 2048
    }
}

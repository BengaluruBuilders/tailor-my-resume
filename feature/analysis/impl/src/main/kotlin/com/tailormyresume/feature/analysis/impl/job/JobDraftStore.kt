package com.tailormyresume.feature.analysis.impl.job

import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

internal data class JobDraft(
    val text: String? = null,
    val importedFrom: String? = null,
    val notAJobPost: Boolean = false,
)

@Singleton
internal class JobDraftStore @Inject constructor(
    sessionRepository: SessionRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val current = MutableStateFlow<JobDraft?>(null)

    val draft: StateFlow<JobDraft?> = current

    init {
        scope.launch {
            sessionRepository.observeAccount().map { it?.id }.distinctUntilChanged().collect { clear() }
        }
    }

    fun set(text: String, importedFrom: String) {
        current.value = JobDraft(text = text, importedFrom = importedFrom)
    }

    fun markNotAJobPost() {
        current.value = JobDraft(notAJobPost = true)
    }

    fun consume(): JobDraft? = current.getAndUpdate { null }

    fun clear() {
        current.value = null
    }
}

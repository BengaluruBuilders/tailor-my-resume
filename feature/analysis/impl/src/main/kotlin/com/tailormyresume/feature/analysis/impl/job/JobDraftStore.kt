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
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) {
    private val current = MutableStateFlow<JobDraft?>(null)
    private val currentLink = MutableStateFlow("")

    val draft: StateFlow<JobDraft?> = current
    val link: StateFlow<String> = currentLink

    init {
        scope.launch {
            sessionRepository.observeAccount().map { it?.id }.distinctUntilChanged().collect { clear() }
        }
    }

    fun set(text: String, importedFrom: String) {
        workFence.check(workFence.ticket())
        current.value = JobDraft(text = text, importedFrom = importedFrom)
        updateDirty()
    }

    fun markNotAJobPost() {
        current.value = JobDraft(notAJobPost = true)
        updateDirty()
    }

    fun setLink(link: String) {
        currentLink.value = link
        updateDirty()
    }

    fun clearLink() {
        currentLink.value = ""
        updateDirty()
    }

    fun consume(): JobDraft? = current.getAndUpdate { null }.also { updateDirty() }

    private fun updateDirty() {
        workFence.editing(this, !current.value?.text.isNullOrBlank() || currentLink.value.isNotBlank())
    }

    fun clear() {
        current.value = null
        currentLink.value = ""
        updateDirty()
    }
}

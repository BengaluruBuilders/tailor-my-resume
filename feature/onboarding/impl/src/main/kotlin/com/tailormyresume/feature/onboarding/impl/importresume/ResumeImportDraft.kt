package com.tailormyresume.feature.onboarding.impl.importresume

import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface ResumeSource {
    data class PickedFile(val file: ResumeFile) : ResumeSource
    data class PastedText(val text: String) : ResumeSource
}

internal enum class UploadFailureKind { ImageOnly, FileProblem, TooLarge, Neutral }

internal data class UploadFailure(
    val kind: UploadFailureKind,
    val fileName: String? = null,
    val mimeType: String? = null,
    val byteSize: Long = 0L,
)

@Singleton
internal class ResumeImportDraft @Inject constructor(
    sessionRepository: SessionRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val pending = MutableStateFlow<ResumeSource?>(null)
    private val failed = MutableStateFlow<UploadFailure?>(null)
    private val shown = MutableStateFlow(NEUTRAL_FAILURE)

    val source: StateFlow<ResumeSource?> = pending
    val failure: StateFlow<UploadFailure?> = failed
    val failureOrNeutral: StateFlow<UploadFailure> = shown

    init {
        scope.launch {
            sessionRepository.observeAccount().map { it?.id }.distinctUntilChanged().drop(1).collect { clear() }
        }
    }

    fun setFile(file: ResumeFile) = begin(ResumeSource.PickedFile(file))

    fun setText(text: String) = begin(ResumeSource.PastedText(text))

    fun consumeSource(): ResumeSource? = pending.getAndUpdate { null }

    fun fail(failure: UploadFailure) {
        pending.value = null
        setFailure(failure)
    }

    fun clear() {
        pending.value = null
        setFailure(null)
    }

    private fun begin(source: ResumeSource) {
        setFailure(null)
        pending.value = source
    }

    private fun setFailure(failure: UploadFailure?) {
        failed.value = failure
        shown.value = failure ?: NEUTRAL_FAILURE
    }

    private companion object {
        val NEUTRAL_FAILURE = UploadFailure(UploadFailureKind.Neutral)
    }
}

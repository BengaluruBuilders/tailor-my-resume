package com.tailormyresume.feature.onboarding.impl.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFileResolver
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class UploadEvent { OpenReading }

@HiltViewModel
internal class UploadViewModel @Inject constructor(
    private val draft: ResumeImportDraft,
    private val fileResolver: ResumeFileResolver,
) : ViewModel() {

    private val eventChannel = Channel<UploadEvent>(Channel.BUFFERED)

    val events: Flow<UploadEvent> = eventChannel.receiveAsFlow()

    fun onUriPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch { onFilePicked(fileResolver.resolve(uri)) }
    }

    fun onFilePicked(file: ResumeFile?) {
        if (file == null) return
        draft.setFile(file)
        eventChannel.trySend(UploadEvent.OpenReading)
    }
}

package com.tailormyresume.feature.onboarding.impl.reading

import androidx.lifecycle.ViewModel
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.onboarding.SaveImportedProfileUseCase
import com.tailormyresume.core.domain.onboarding.personNameOrNull
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.continues
import com.tailormyresume.feature.onboarding.impl.importresume.MAX_RESUME_CHARS
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFile
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeImportDraft
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeRead
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeSource
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeTextSource
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailure
import com.tailormyresume.feature.onboarding.impl.importresume.UploadFailureKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReadingViewModel @Inject constructor(
    private val draft: ResumeImportDraft,
    private val textSource: ResumeTextSource,
    private val parser: ResumeTextParser,
    private val saveImported: SaveImportedProfileUseCase,
    private val profileRepository: ProfileRepository,
    @Dispatcher(TmrDispatchers.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + io)

    private val eventChannel = Channel<ReadingEvent>(Channel.BUFFERED)

    val events: Flow<ReadingEvent> = eventChannel.receiveAsFlow()

    private val state = MutableStateFlow(
        ReadingUiState(
            file = ReadingFileUi(name = null, mimeType = null, byteSize = 0L, characters = 0),
            percent = 0,
            rows = ReadingRowKind.entries.mapIndexed { index, kind ->
                ReadingRowUi(
                    kind = kind,
                    state = if (index == 0) ReadingRowState.Active else ReadingRowState.Pending,
                )
            },
        ),
    )

    val uiState: StateFlow<ReadingUiState> = state.asStateFlow()

    init {
        val source = draft.consumeSource()
        if (source == null) {
            eventChannel.trySend(ReadingEvent.NothingToRead)
        } else {
            state.value = state.value.copy(file = fileUi(source))
            scope.launch { runImport(source) }
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private suspend fun runImport(source: ResumeSource) {
        val file = (source as? ResumeSource.PickedFile)?.file
        val text = when (val reading = readText(source)) {
            is TextReading.Ready -> reading.text
            is TextReading.Unreadable -> {
                fail(reading.kind, file)
                return
            }
        }
        state.value = state.value.copy(
            percent = READING_PERCENT,
            rows = ReadingRowKind.entries.map { ReadingRowUi(it, ReadingRowState.Active) },
        )
        val parsed = try {
            parser.parse(text, keptEntries())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            fail(UploadFailureKind.Neutral, file)
            return
        }
        if (parsed.entries.none { it.category in READABLE_CATEGORIES } && parsed.skills.isEmpty()) {
            fail(UploadFailureKind.Neutral, file)
            return
        }
        try {
            saveImported(parsed, file?.displayName)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            fail(UploadFailureKind.Neutral, file)
            return
        }
        state.value = state.value.copy(percent = 100, rows = doneRows(parsed))
        eventChannel.trySend(ReadingEvent.Done)
    }

    private suspend fun readText(source: ResumeSource): TextReading = when (source) {
        is ResumeSource.PastedText -> TextReading.Ready(source.text.take(MAX_RESUME_CHARS))
        is ResumeSource.PickedFile -> {
            val read = try {
                textSource.read(source.file)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                ResumeRead.Unreadable(source.file.displayName)
            }
            when (read) {
                is ResumeRead.Text -> TextReading.Ready(read.text.take(MAX_RESUME_CHARS))
                is ResumeRead.NoTextLayer -> TextReading.Unreadable(UploadFailureKind.ImageOnly)
                is ResumeRead.TooLarge -> TextReading.Unreadable(UploadFailureKind.TooLarge)
                else -> TextReading.Unreadable(UploadFailureKind.FileProblem)
            }
        }
    }

    private suspend fun keptEntries(): Int = try {
        profileRepository.observeProfile().first()?.entries?.count { it.source != FactSource.IMPORTED } ?: 0
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        0
    }

    private fun doneRows(parsed: CandidateProfile): List<ReadingRowUi> = ReadingRowKind.entries.map { kind ->
        val count = when (kind) {
            ReadingRowKind.Contact -> listOf(personNameOrNull(parsed.fullName).orEmpty(), parsed.email, parsed.phone).count { it.isNotBlank() }
            ReadingRowKind.Experience -> parsed.entries.countRoles(EntryCategory.EXPERIENCE)
            ReadingRowKind.Education -> parsed.entries.countRoles(EntryCategory.EDUCATION)
            ReadingRowKind.Skills -> parsed.skills.size
            ReadingRowKind.Achievements -> parsed.entries.countRoles(EntryCategory.ACHIEVEMENT)
        }
        ReadingRowUi(kind = kind, state = ReadingRowState.Done, count = count)
    }

    private fun List<ProfileEntry>.countRoles(category: EntryCategory): Int =
        withIndex().count { (index, entry) ->
            entry.category == category && (index == 0 || !entry.continues(this[index - 1]))
        }

    private fun fileUi(source: ResumeSource): ReadingFileUi = when (source) {
        is ResumeSource.PickedFile -> ReadingFileUi(
            name = source.file.displayName,
            mimeType = source.file.mimeType,
            byteSize = source.file.byteSize,
            characters = 0,
        )
        is ResumeSource.PastedText -> ReadingFileUi(
            name = null,
            mimeType = null,
            byteSize = 0L,
            characters = source.text.length,
        )
    }

    private fun fail(kind: UploadFailureKind, file: ResumeFile?) {
        draft.fail(
            UploadFailure(
                kind = kind,
                fileName = file?.displayName,
                mimeType = file?.mimeType,
                byteSize = file?.byteSize ?: 0L,
            ),
        )
        eventChannel.trySend(ReadingEvent.Failed)
    }

    private companion object {
        const val READING_PERCENT = 40
        val READABLE_CATEGORIES = setOf(EntryCategory.EXPERIENCE, EntryCategory.EDUCATION)
    }
}

private sealed interface TextReading {
    data class Ready(val text: String) : TextReading
    data class Unreadable(val kind: UploadFailureKind) : TextReading
}

package com.tailormyresume.feature.profile.impl.experience

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.model.continues
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val PRESENT_LABEL = "Present"

private const val PRESENT_IGNORING_CASE = "present"

private const val UNIQUE_SUFFIX_LENGTH = 8

internal data class EditRoleDraft(
    val title: String = "",
    val company: String = "",
    val start: String = "",
    val end: String = "",
    val current: Boolean = false,
    val bullets: List<String> = listOf(""),
    val bulletIds: List<String?> = listOf(null),
)

internal sealed interface EditRoleUiState {
    data object Loading : EditRoleUiState

    data class Editing(
        val isNew: Boolean,
        val draft: EditRoleDraft,
        val canSave: Boolean,
        val canAddBullet: Boolean,
    ) : EditRoleUiState
}

internal enum class EditRoleEvent { Saved, Deleted }

@HiltViewModel(assistedFactory = EditRoleViewModel.Factory::class)
internal class EditRoleViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    @Assisted private val entryId: String?,
) : ViewModel() {

    private val draft = MutableStateFlow<EditRoleDraft?>(null)

    private val channel = Channel<EditRoleEvent>(Channel.BUFFERED)

    val events: Flow<EditRoleEvent> = channel.receiveAsFlow()

    private var finished = false

    private var busy = false

    val uiState: StateFlow<EditRoleUiState> = combine(
        profileRepository.observeProfile(),
        draft,
    ) { profile, roleDraft ->
        if (profile == null || roleDraft == null) {
            EditRoleUiState.Loading
        } else {
            EditRoleUiState.Editing(
                isNew = entryId == null,
                draft = roleDraft,
                canSave = canSave(profile, roleDraft),
                canAddBullet = roleDraft.bullets.size < ProfileLimits.MAX_BULLETS_PER_ENTRY,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditRoleUiState.Loading,
    )

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            if (draft.value != null) return@launch
            val entry = entryId?.let { id ->
                profile.entries.firstOrNull { it.id == id && it.category == EntryCategory.EXPERIENCE }
            }
            draft.value = when {
                entryId == null -> EditRoleDraft()
                else -> entry?.toDraft() ?: return@launch
            }
        }
    }

    fun onTitleChange(value: String): Unit = edit { it.copy(title = value) }

    fun onCompanyChange(value: String): Unit = edit { it.copy(company = value) }

    fun onStartChange(value: String): Unit = edit { it.copy(start = value) }

    fun onEndChange(value: String): Unit = edit { it.copy(end = value) }

    fun onCurrentChange(value: Boolean): Unit = edit { it.copy(current = value) }

    fun onBulletChange(index: Int, value: String): Unit = edit { roleDraft ->
        if (index in roleDraft.bullets.indices) {
            roleDraft.copy(bullets = roleDraft.bullets.toMutableList().apply { set(index, value) })
        } else {
            roleDraft
        }
    }

    fun onAddBullet(): Unit = edit { roleDraft ->
        if (roleDraft.bullets.size < ProfileLimits.MAX_BULLETS_PER_ENTRY) {
            roleDraft.copy(bullets = roleDraft.bullets + "", bulletIds = roleDraft.bulletIds + null)
        } else {
            roleDraft
        }
    }

    fun save(): Unit = singleFlight { write() }

    fun delete(): Unit = singleFlight { remove() }

    private fun singleFlight(action: suspend () -> Unit) {
        if (finished || busy) return
        busy = true
        viewModelScope.launch {
            try {
                action()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun write() {
        if (finished) return
        val profile = profileRepository.observeProfile().first() ?: return
        val roleDraft = draft.value ?: return
        if (!canSave(profile, roleDraft)) return
        val original = entryId?.let { id ->
            profile.entries.firstOrNull { it.id == id && it.category == EntryCategory.EXPERIENCE }
                ?: return
        }
        val edited = if (original != null) {
            roleDraft.toEntry(editedId = original.id, original = original, source = FactSource.USER_EDITED)
        } else {
            val newId = FactIdAllocator().neverUsedId(EntryCategory.EXPERIENCE, profile.entries, roleDraft.title.trim())
            roleDraft.toEntry(editedId = newId, original = null, source = FactSource.USER_STATED)
        }
        val entries = if (original == null) {
            profile.entries.withExperienceInserted(edited)
        } else {
            val group = profile.entries.continuationGroupOf(original.id)
            profile.entries.map { entry ->
                when {
                    entry.id == edited.id -> edited
                    entry.id in group -> entry.copy(
                        title = edited.title,
                        organization = edited.organization,
                        startDate = edited.startDate,
                        endDate = edited.endDate,
                    )
                    else -> entry
                }
            }
        }
        finished = true
        profileRepository.saveProfile(profile.copy(entries = entries))
        channel.send(EditRoleEvent.Saved)
    }

    private suspend fun remove() {
        val id = entryId ?: return
        val profile = profileRepository.observeProfile().first() ?: return
        finished = true
        val group = profile.entries.continuationGroupOf(id)
        profileRepository.saveProfile(profile.copy(entries = profile.entries.filterNot { it.id in group }))
        channel.send(EditRoleEvent.Deleted)
    }

    private fun edit(transform: (EditRoleDraft) -> EditRoleDraft) {
        draft.update { roleDraft -> roleDraft?.let(transform) }
    }

    private fun canSave(profile: CandidateProfile, roleDraft: EditRoleDraft): Boolean =
        roleDraft.title.isNotBlank() &&
            roleDraft.company.isNotBlank() &&
            roleDraft.bullets.all { it.trim().length <= ProfileLimits.MAX_BULLET_LENGTH } &&
            roleDraft.bullets.count { it.isNotBlank() } <= ProfileLimits.MAX_BULLETS_PER_ENTRY &&
            (entryId != null || profile.entries.size < ProfileLimits.MAX_ENTRIES)

    @AssistedFactory
    interface Factory {
        fun create(entryId: String?): EditRoleViewModel
    }
}

private fun EditRoleDraft.toEntry(
    editedId: String,
    original: ProfileEntry?,
    source: FactSource,
): ProfileEntry = ProfileEntry(
    id = editedId,
    category = EntryCategory.EXPERIENCE,
    title = title.trim(),
    organization = company.trim(),
    startDate = start.trim(),
    endDate = if (current) PRESENT_LABEL else end.trim(),
    bullets = toBullets(editedId, original),
    source = source,
    isConfirmed = true,
)

private fun EditRoleDraft.toBullets(editedId: String, original: ProfileEntry?): List<EvidenceBullet> {
    val taken = bulletIds.filterNotNullTo(mutableSetOf())
    return bullets.mapIndexedNotNull { index, rawText ->
        val text = rawText.trim()
        if (text.isEmpty()) return@mapIndexedNotNull null
        val id = bulletIds.getOrNull(index)
            ?: if (original == null) nextFreeBulletId(editedId, taken) else randomBulletId(editedId, taken)
        taken += id
        EvidenceBullet(id = id, text = text)
    }
}

private fun randomBulletId(entryId: String, taken: Set<String>): String {
    while (true) {
        val id = "$entryId-b${UUID.randomUUID().toString().take(UNIQUE_SUFFIX_LENGTH)}"
        if (id !in taken) return id
    }
}

private fun nextFreeBulletId(entryId: String, taken: Set<String>): String {
    var index = 1
    while ("$entryId-b$index" in taken) index++
    return "$entryId-b$index"
}

private fun ProfileEntry.toDraft(): EditRoleDraft {
    val isPresent = endDate.trim().equals(PRESENT_IGNORING_CASE, ignoreCase = true)
    return EditRoleDraft(
        title = title,
        company = organization,
        start = startDate,
        end = if (isPresent) "" else endDate,
        current = isPresent,
        bullets = bullets.map { it.text }.ifEmpty { listOf("") },
        bulletIds = bullets.map { it.id }.ifEmpty { listOf(null) },
    )
}

private fun List<ProfileEntry>.withExperienceInserted(entry: ProfileEntry): List<ProfileEntry> {
    val position = indexOfFirst { it.category == EntryCategory.EXPERIENCE }
    return if (position < 0) this + entry else toMutableList().apply { add(position, entry) }
}

private fun List<ProfileEntry>.continuationGroupOf(entryId: String): Set<String> {
    val index = indexOfFirst { it.id == entryId }
    if (index < 0) return emptySet()
    var first = index
    while (first > 0 && this[first].continues(this[first - 1])) first--
    var last = index
    while (last < lastIndex && this[last + 1].continues(this[last])) last++
    return subList(first, last + 1).mapTo(mutableSetOf()) { it.id }
}

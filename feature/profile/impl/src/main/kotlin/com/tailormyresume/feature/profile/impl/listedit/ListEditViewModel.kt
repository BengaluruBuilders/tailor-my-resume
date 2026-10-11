package com.tailormyresume.feature.profile.impl.listedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
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

private const val MAX_SUMMARY_LENGTH = 400

private const val LINE_SEPARATOR = " · "

private const val DATE_SEPARATOR = " – "

private data class ListEditItem(
    val entryId: String?,
    val originalText: String,
    val text: String,
) {
    val isChanged: Boolean get() = text.trim() != originalText.trim()
}

internal sealed interface ListEditUiState {
    data object Loading : ListEditUiState

    data class Editing(
        val section: ProfileListSection,
        val texts: List<String>,
        val canSave: Boolean,
        val canAdd: Boolean,
    ) : ListEditUiState
}

internal enum class ListEditEvent { Saved }

@HiltViewModel(assistedFactory = ListEditViewModel.Factory::class)
internal class ListEditViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    @Assisted private val section: ProfileListSection,
) : ViewModel() {

    private val items = MutableStateFlow<List<ListEditItem>?>(null)

    private val channel = Channel<ListEditEvent>(Channel.BUFFERED)

    val events: Flow<ListEditEvent> = channel.receiveAsFlow()

    private var finished = false

    private var busy = false

    val uiState: StateFlow<ListEditUiState> = combine(
        profileRepository.observeProfile(),
        items,
    ) { profile, listItems ->
        if (profile == null || listItems == null) {
            ListEditUiState.Loading
        } else {
            ListEditUiState.Editing(
                section = section,
                texts = listItems.map { it.text },
                canSave = canSave(listItems),
                canAdd = section != ProfileListSection.SUMMARY &&
                    profile.entries.size + listItems.count { it.entryId == null } < ProfileLimits.MAX_ENTRIES,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListEditUiState.Loading,
    )

    init {
        viewModelScope.launch {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            items.compareAndSet(null, initialItems(profile))
        }
    }

    fun onTextChange(index: Int, value: String) {
        items.update { listItems ->
            listItems?.takeIf { index in it.indices }?.toMutableList()?.apply { set(index, this[index].copy(text = value)) }
                ?: listItems
        }
    }

    fun onAdd() {
        items.update { listItems ->
            if (section == ProfileListSection.SUMMARY) listItems else listItems?.plus(ListEditItem(null, "", ""))
        }
    }

    fun save() {
        if (finished || busy) return
        busy = true
        viewModelScope.launch {
            try {
                write()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun write() {
        val listItems = items.value ?: return
        if (!canSave(listItems)) return
        val profile = profileRepository.observeProfile().first() ?: return
        val updated = when (section) {
            ProfileListSection.SUMMARY -> listItems.single().let { item ->
                profile.copy(
                    summary = item.text.trim(),
                    contactUserStated = profile.contactUserStated || item.isChanged,
                )
            }
            ProfileListSection.EDUCATION -> profile.withListSaved(EntryCategory.EDUCATION, listItems)
            ProfileListSection.ACHIEVEMENTS -> profile.withListSaved(EntryCategory.ACHIEVEMENT, listItems)
        }
        finished = true
        profileRepository.saveProfile(updated)
        channel.send(ListEditEvent.Saved)
    }

    private fun initialItems(profile: CandidateProfile): List<ListEditItem> = when (section) {
        ProfileListSection.SUMMARY -> listOf(ListEditItem(null, profile.summary, profile.summary))
        ProfileListSection.EDUCATION -> profile.entryItems(EntryCategory.EDUCATION)
        ProfileListSection.ACHIEVEMENTS -> profile.entryItems(EntryCategory.ACHIEVEMENT)
    }

    private fun canSave(listItems: List<ListEditItem>): Boolean = when (section) {
        ProfileListSection.SUMMARY -> listItems.all { it.text.trim().length <= MAX_SUMMARY_LENGTH }
        else -> listItems.all { !it.isChanged || it.text.trim().length <= ProfileLimits.MAX_TEXT_LENGTH }
    }

    @AssistedFactory
    interface Factory {
        fun create(section: ProfileListSection): ListEditViewModel
    }
}

private fun CandidateProfile.entryItems(category: EntryCategory): List<ListEditItem> =
    entries.filter { it.category == category }.map { entry ->
        val line = entry.toLine()
        ListEditItem(entryId = entry.id, originalText = line, text = line)
    }

private fun ProfileEntry.toLine(): String {
    val dates = listOf(startDate, endDate).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(DATE_SEPARATOR)
    return (listOf(title, organization, dates) + bullets.map { it.text })
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(LINE_SEPARATOR)
}

private fun CandidateProfile.withListSaved(category: EntryCategory, items: List<ListEditItem>): CandidateProfile {
    val originalIds = entries.mapTo(mutableSetOf()) { it.id }
    val allocator = FactIdAllocator()
    var result = entries
    items.filter { it.entryId != null }.forEach { item ->
        val text = item.text.trim()
        result = when {
            !item.isChanged -> if (text.isEmpty()) result else result.map { if (it.id == item.entryId) it.copy(isConfirmed = true) else it }
            text.isEmpty() -> result.filterNot { it.id == item.entryId }
            else -> result.map { if (it.id == item.entryId) it.asEditedLine(text) else it }
        }
    }
    items.filter { it.entryId == null && it.text.isNotBlank() }.forEach { item ->
        val text = item.text.trim()
        val id = allocator.neverUsedId(category, result.filter { it.id !in originalIds } + entries, text)
        result = result.withInsertedAfterLast(category, text.asNewEntry(id, category))
    }
    return copy(entries = result)
}

private fun ProfileEntry.asEditedLine(text: String): ProfileEntry = copy(
    title = text,
    organization = "",
    startDate = "",
    endDate = "",
    bullets = emptyList(),
    source = FactSource.USER_EDITED,
    isConfirmed = true,
)

private fun String.asNewEntry(id: String, category: EntryCategory): ProfileEntry = ProfileEntry(
    id = id,
    category = category,
    title = this,
    organization = "",
    startDate = "",
    endDate = "",
    bullets = emptyList(),
    source = FactSource.USER_STATED,
    isConfirmed = true,
)

private fun List<ProfileEntry>.withInsertedAfterLast(category: EntryCategory, entry: ProfileEntry): List<ProfileEntry> {
    val position = indexOfLast { it.category == category }
    return if (position < 0) this + entry else toMutableList().apply { add(position + 1, entry) }
}

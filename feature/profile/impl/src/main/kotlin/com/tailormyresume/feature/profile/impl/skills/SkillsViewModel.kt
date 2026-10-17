package com.tailormyresume.feature.profile.impl.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.ProfileLimits
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
import javax.inject.Inject

private data class SkillsDraft(
    val skills: List<String>,
    val addedThisSession: List<String> = emptyList(),
    val newSkill: String = "",
)

internal sealed interface SkillsUiState {
    data object Loading : SkillsUiState

    data class Editing(
        val skills: List<String>,
        val newSkill: String,
        val canAdd: Boolean,
    ) : SkillsUiState
}

internal enum class SkillsEvent { Saved }

@HiltViewModel
internal class SkillsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val workFence: com.tailormyresume.core.data.account.LocalWorkFence = com.tailormyresume.core.data.account.LocalWorkFence(),
) : ViewModel() {
    private val workTicket = workFence.ticket()


    private val draft = MutableStateFlow<SkillsDraft?>(null)

    private val channel = Channel<SkillsEvent>(Channel.BUFFERED)

    val events: Flow<SkillsEvent> = channel.receiveAsFlow()

    private var finished = false

    private var busy = false

    val uiState: StateFlow<SkillsUiState> = combine(
        profileRepository.observeProfile(),
        draft,
    ) { profile, skillsDraft ->
        if (profile == null || skillsDraft == null) {
            SkillsUiState.Loading
        } else {
            SkillsUiState.Editing(skillsDraft.skills, skillsDraft.newSkill, skillsDraft.canAdd())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SkillsUiState.Loading,
    )

    init {
        viewModelScope.launch(workFence.context(workTicket)) {
            val profile = profileRepository.observeProfile().filterNotNull().first()
            draft.compareAndSet(null, SkillsDraft(profile.skills))
        }
    }

    fun onNewSkillChange(value: String): Unit = edit { it.copy(newSkill = value) }

    fun onAdd(): Unit = edit { it.withPendingSkillAdded() }

    fun onRemove(skill: String): Unit = edit { skillsDraft ->
        skillsDraft.copy(skills = skillsDraft.skills.filterNot { it == skill })
    }

    fun save() {
        if (finished || busy) return
        busy = true
        viewModelScope.launch(workFence.context(workTicket)) {
            try {
                write()
            } finally {
                busy = false
            }
        }
    }

    private suspend fun write() {
        val skillsDraft = draft.value?.withPendingSkillAdded() ?: return
        val profile = profileRepository.observeProfile().first() ?: return
        val kept = profile.userStatedSkills.filter { skillsDraft.skills.containsIgnoringCase(it) }
        val added = skillsDraft.addedThisSession.filter {
            skillsDraft.skills.containsIgnoringCase(it) && !kept.containsIgnoringCase(it)
        }
        finished = true
        workFence.check(workTicket)
        profileRepository.saveProfile(profile.copy(skills = skillsDraft.skills, userStatedSkills = kept + added))
        workFence.editing(this, false)
        channel.send(SkillsEvent.Saved)
    }

    private fun edit(transform: (SkillsDraft) -> SkillsDraft) {
        workFence.editing(this, true)
        draft.update { skillsDraft -> skillsDraft?.let(transform) }
    }
    override fun onCleared() {
        workFence.editing(this, false)
        super.onCleared()
    }

}

private fun List<String>.containsIgnoringCase(name: String): Boolean = any { it.equals(name, ignoreCase = true) }

private fun SkillsDraft.canAdd(): Boolean {
    val name = newSkill.trim()
    return name.isNotEmpty() &&
        name.length <= ProfileLimits.MAX_SKILL_LENGTH &&
        skills.size < ProfileLimits.MAX_SKILLS &&
        !skills.containsIgnoringCase(name)
}

private fun SkillsDraft.withPendingSkillAdded(): SkillsDraft {
    if (!canAdd()) return this
    val name = newSkill.trim()
    return copy(skills = listOf(name) + skills, addedThisSession = addedThisSession + name, newSkill = "")
}

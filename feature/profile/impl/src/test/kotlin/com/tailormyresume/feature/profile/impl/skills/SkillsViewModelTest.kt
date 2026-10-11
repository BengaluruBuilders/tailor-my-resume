package com.tailormyresume.feature.profile.impl.skills

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SkillsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val profileRepository = TestProfileRepository()

    private val profile = PrototypeFixtures.returning().profile.copy(userStatedSkills = listOf("Tableau"))

    private fun viewModel() = SkillsViewModel(profileRepository)

    private fun TestScope.collectUiState(viewModel: SkillsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
    }

    private fun SkillsViewModel.editing(): SkillsUiState.Editing {
        val state = uiState.value
        assertThat(state).isInstanceOf(SkillsUiState.Editing::class.java)
        return state as SkillsUiState.Editing
    }

    private suspend fun savedProfile(): CandidateProfile = checkNotNull(profileRepository.observeProfile().first())

    private suspend fun SkillsViewModel.doneAndAwait() {
        events.test {
            save()
            assertThat(awaitItem()).isEqualTo(SkillsEvent.Saved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun SkillsViewModel.add(name: String) {
        onNewSkillChange(name)
        onAdd()
    }

    private suspend fun TestScope.loaded(): SkillsViewModel {
        profileRepository.sendProfile(profile)
        return viewModel().also { collectUiState(it) }
    }

    @Test
    fun initialState_beforeProfileIsLoaded_isLoading() {
        assertThat(viewModel().uiState.value).isEqualTo(SkillsUiState.Loading)
    }

    @Test
    fun countAndOrder() = runTest {
        val viewModel = loaded()

        val editing = viewModel.editing()

        assertThat(editing.skills).containsExactlyElementsIn(profile.skills).inOrder()
        assertThat(editing.newSkill).isEmpty()
        assertThat(editing.canAdd).isFalse()
    }

    @Test
    fun addPutsSkillFirstAndMarksUserStated() = runTest {
        val viewModel = loaded()

        viewModel.add("  Snowflake  ")

        assertThat(viewModel.editing().skills.first()).isEqualTo("Snowflake")
        assertThat(viewModel.editing().newSkill).isEmpty()
        viewModel.doneAndAwait()
        val saved = savedProfile()
        assertThat(saved.skills.first()).isEqualTo("Snowflake")
        assertThat(saved.userStatedSkills).containsExactly("Tableau", "Snowflake")
    }

    @Test
    fun duplicateCaseInsensitiveAndBlankAndOverLengthAndHundredthPlusAreRejected() = runTest {
        val viewModel = loaded()
        val existing = profile.skills.first()

        viewModel.onNewSkillChange(existing.uppercase())
        assertThat(viewModel.editing().canAdd).isFalse()
        viewModel.onNewSkillChange("   ")
        assertThat(viewModel.editing().canAdd).isFalse()
        viewModel.onNewSkillChange("x".repeat(ProfileLimits.MAX_SKILL_LENGTH + 1))
        assertThat(viewModel.editing().canAdd).isFalse()
        viewModel.onNewSkillChange("x".repeat(ProfileLimits.MAX_SKILL_LENGTH))
        assertThat(viewModel.editing().canAdd).isTrue()

        viewModel.onNewSkillChange(existing.uppercase())
        viewModel.onAdd()
        assertThat(viewModel.editing().skills).isEqualTo(profile.skills)

        (1..ProfileLimits.MAX_SKILLS).forEach { viewModel.add("Extra $it") }
        assertThat(viewModel.editing().skills).hasSize(ProfileLimits.MAX_SKILLS)
        viewModel.onNewSkillChange("One more")
        assertThat(viewModel.editing().canAdd).isFalse()
    }

    @Test
    fun removeDropsFromSkillsAndUserStated() = runTest {
        val viewModel = loaded()

        viewModel.onRemove("Tableau")
        viewModel.onRemove(profile.skills.first())
        viewModel.doneAndAwait()

        val saved = savedProfile()
        assertThat(saved.skills).doesNotContain("Tableau")
        assertThat(saved.skills).doesNotContain(profile.skills.first())
        assertThat(saved.userStatedSkills).isEmpty()
    }

    @Test
    fun addedThenRemovedInTheSameSessionIsNotUserStated() = runTest {
        val viewModel = loaded()

        viewModel.add("Snowflake")
        viewModel.onRemove("Snowflake")
        viewModel.doneAndAwait()

        val saved = savedProfile()
        assertThat(saved.skills).doesNotContain("Snowflake")
        assertThat(saved.userStatedSkills).containsExactly("Tableau")
    }

    @Test
    fun doneCommitsPendingDraftText() = runTest {
        val viewModel = loaded()

        viewModel.onNewSkillChange("Snowflake")
        viewModel.doneAndAwait()

        val saved = savedProfile()
        assertThat(saved.skills.first()).isEqualTo("Snowflake")
        assertThat(saved.userStatedSkills).contains("Snowflake")
    }

    @Test
    fun doneIgnoresPendingTextThatCannotBeAdded() = runTest {
        val viewModel = loaded()

        viewModel.onNewSkillChange(profile.skills.first().lowercase())
        viewModel.doneAndAwait()

        assertThat(savedProfile().skills).isEqualTo(profile.skills)
    }

    @Test
    fun backWithoutDoneWritesNothing() = runTest {
        val viewModel = loaded()

        viewModel.add("Snowflake")
        viewModel.onRemove("Tableau")

        assertThat(savedProfile()).isEqualTo(profile)
    }

    @Test
    fun saveLeavesEntriesAndContact() = runTest {
        val viewModel = loaded()
        viewModel.add("Snowflake")

        viewModel.doneAndAwait()

        val saved = savedProfile()
        assertThat(saved.entries).isEqualTo(profile.entries)
        assertThat(saved.copy(skills = profile.skills, userStatedSkills = profile.userStatedSkills)).isEqualTo(profile)
    }

    @Test
    fun doubleDoneWritesOnce() = runTest {
        val viewModel = loaded()

        viewModel.events.test {
            viewModel.save()
            viewModel.save()
            assertThat(awaitItem()).isEqualTo(SkillsEvent.Saved)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

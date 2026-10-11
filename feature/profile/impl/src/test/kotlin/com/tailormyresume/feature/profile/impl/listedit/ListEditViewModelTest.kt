package com.tailormyresume.feature.profile.impl.listedit

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ListEditViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val profileRepository = TestProfileRepository()

    private val profile = PrototypeFixtures.returning().profile

    private fun viewModel(section: ProfileListSection) = ListEditViewModel(profileRepository, section)

    private fun TestScope.loaded(section: ProfileListSection, base: CandidateProfile = profile): ListEditViewModel {
        profileRepository.sendProfile(base)
        return viewModel(section).also { backgroundScope.launch(UnconfinedTestDispatcher()) { it.uiState.collect() } }
    }

    private fun ListEditViewModel.editing(): ListEditUiState.Editing {
        val state = uiState.value
        assertThat(state).isInstanceOf(ListEditUiState.Editing::class.java)
        return state as ListEditUiState.Editing
    }

    private suspend fun savedProfile(): CandidateProfile = checkNotNull(profileRepository.observeProfile().first())

    private suspend fun ListEditViewModel.saveAndAwait() {
        events.test {
            save()
            assertThat(awaitItem()).isEqualTo(ListEditEvent.Saved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun CandidateProfile.category(category: EntryCategory) = entries.filter { it.category == category }

    private fun ListEditViewModel.addItem(text: String) {
        onAdd()
        onTextChange(editing().texts.lastIndex, text)
    }

    @Test
    fun initialState_beforeProfileIsLoaded_isLoading() {
        assertThat(viewModel(ProfileListSection.SUMMARY).uiState.value).isEqualTo(ListEditUiState.Loading)
    }

    @Test
    fun summaryShowsNoteOneItemNoAdd() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)

        val editing = viewModel.editing()

        assertThat(editing.texts).containsExactly(profile.summary)
        assertThat(editing.canAdd).isFalse()
        assertThat(editing.canSave).isTrue()
    }

    @Test
    fun educationHasNoNoteAndAddLabel() = runTest {
        val viewModel = loaded(ProfileListSection.EDUCATION)

        val editing = viewModel.editing()

        assertThat(editing.texts).containsExactly("B.Com, Finance · Symbiosis College, Pune · 2020")
        assertThat(editing.canAdd).isTrue()
    }

    @Test
    fun achievementsShowNoteSixItemsAndAddLabel() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)

        val editing = viewModel.editing()

        assertThat(editing.texts).hasSize(6)
        assertThat(editing.texts.first()).isEqualTo("Cut monthly reporting prep time by 40%")
        assertThat(editing.canAdd).isTrue()
    }

    @Test
    fun laterProfileEmissionsDoNotOverwriteTheDraft() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)
        viewModel.onTextChange(0, "Mine")

        profileRepository.sendProfile(profile.copy(summary = "Imported again"))

        assertThat(viewModel.editing().texts).containsExactly("Mine")
    }

    @Test
    fun summarySaveStoresTrimmedText() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)
        viewModel.onTextChange(0, "  Analyst who ships.  ")

        viewModel.saveAndAwait()

        assertThat(savedProfile().summary).isEqualTo("Analyst who ships.")
    }

    @Test
    fun blankSummaryClearsIt() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)
        viewModel.onTextChange(0, "   ")

        viewModel.saveAndAwait()

        assertThat(savedProfile().summary).isEmpty()
    }

    @Test
    fun unchangedItemKeepsImportedSourceBulletsAndId() = runTest {
        val withDetail = profile.copy(
            entries = profile.entries.map {
                if (it.id == "edu-1") {
                    it.copy(organization = "Symbiosis", startDate = "2017", endDate = "2020", bullets = listOf(EvidenceBullet("edu-1-b1", "Gold medal")))
                } else {
                    it
                }
            },
        )
        val viewModel = loaded(ProfileListSection.EDUCATION, withDetail)
        assertThat(viewModel.editing().texts.single()).contains("Gold medal")

        viewModel.saveAndAwait()

        assertThat(savedProfile().entries).isEqualTo(withDetail.entries)
    }

    @Test
    fun changedItemBecomesUserEditedSameId() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)
        viewModel.onTextChange(1, "  Built 14 dashboards  ")

        viewModel.saveAndAwait()

        val saved = savedProfile().category(EntryCategory.ACHIEVEMENT)
        val edited = saved.single { it.id == "ach-2" }
        assertThat(edited.title).isEqualTo("Built 14 dashboards")
        assertThat(edited.source).isEqualTo(FactSource.USER_EDITED)
        assertThat(edited.isConfirmed).isTrue()
        assertThat(edited.organization).isEmpty()
        assertThat(edited.bullets).isEmpty()
        assertThat(saved.filter { it.id != "ach-2" }).isEqualTo(profile.category(EntryCategory.ACHIEVEMENT).filter { it.id != "ach-2" })
    }

    @Test
    fun changedItemDropsHiddenDetailBecauseTheTextShowedIt() = runTest {
        val withDetail = profile.copy(
            entries = profile.entries.map {
                if (it.id == "edu-1") it.copy(organization = "Symbiosis", startDate = "2017", endDate = "2020") else it
            },
        )
        val viewModel = loaded(ProfileListSection.EDUCATION, withDetail)
        viewModel.onTextChange(0, "B.Com")

        viewModel.saveAndAwait()

        val edited = savedProfile().category(EntryCategory.EDUCATION).single()
        assertThat(edited.title).isEqualTo("B.Com")
        assertThat(edited.organization).isEmpty()
        assertThat(edited.startDate).isEmpty()
        assertThat(edited.endDate).isEmpty()
    }

    @Test
    fun blankedExistingItemIsRemoved() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)
        viewModel.onTextChange(0, "  ")

        viewModel.saveAndAwait()

        assertThat(savedProfile().category(EntryCategory.ACHIEVEMENT).map { it.id })
            .containsExactly("ach-2", "ach-3", "ach-4", "ach-5", "ach-6").inOrder()
    }

    @Test
    fun newItemIsUserStatedConfirmedWithNeverUsedId() = runTest {
        val viewModel = loaded(ProfileListSection.EDUCATION)
        viewModel.addItem("  PGDM, IIM Indore  ")
        assertThat(viewModel.editing().texts).hasSize(2)

        viewModel.saveAndAwait()

        val saved = savedProfile()
        val added = saved.category(EntryCategory.EDUCATION).last()
        assertThat(added.title).isEqualTo("PGDM, IIM Indore")
        assertThat(added.source).isEqualTo(FactSource.USER_STATED)
        assertThat(added.isConfirmed).isTrue()
        assertThat(added.id).matches("E-[0-9a-f]{8}")
        assertThat(profile.entries.map { it.id }).doesNotContain(added.id)
        val position = saved.entries.indexOf(added)
        assertThat(saved.entries[position - 1].id).isEqualTo("edu-1")
    }

    @Test
    fun deletedThenAddedNeverReusesTheDeletedId() = runTest {
        val viewModel = loaded(ProfileListSection.EDUCATION)
        viewModel.onTextChange(0, "")
        viewModel.addItem("PGDM, IIM Indore")

        viewModel.saveAndAwait()

        val ids = savedProfile().category(EntryCategory.EDUCATION).map { it.id }
        assertThat(ids).hasSize(1)
        assertThat(ids).doesNotContain("edu-1")
    }

    @Test
    fun blankNewItemIsDropped() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)
        viewModel.onAdd()

        viewModel.saveAndAwait()

        assertThat(savedProfile().entries).isEqualTo(profile.entries)
    }

    @Test
    fun saveLeavesContactSkillsAndOtherCategoriesUntouched() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)
        viewModel.addItem("Won the quarterly award")

        viewModel.saveAndAwait()

        val saved = savedProfile()
        assertThat(saved.copy(entries = profile.entries)).isEqualTo(profile)
        assertThat(saved.entries.filter { it.category != EntryCategory.ACHIEVEMENT })
            .isEqualTo(profile.entries.filter { it.category != EntryCategory.ACHIEVEMENT })
    }

    @Test
    fun overLengthItemDisablesSave() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)

        viewModel.onTextChange(0, "x".repeat(ProfileLimits.MAX_TEXT_LENGTH + 1))
        assertThat(viewModel.editing().canSave).isFalse()

        viewModel.onTextChange(0, "x".repeat(ProfileLimits.MAX_TEXT_LENGTH))
        assertThat(viewModel.editing().canSave).isTrue()
    }

    @Test
    fun summaryOver400DisablesSave() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)

        viewModel.onTextChange(0, "x".repeat(401))
        assertThat(viewModel.editing().canSave).isFalse()

        viewModel.onTextChange(0, "x".repeat(400))
        assertThat(viewModel.editing().canSave).isTrue()
    }

    @Test
    fun addDisabledAtEntryLimit() = runTest {
        val filler = (1..ProfileLimits.MAX_ENTRIES - profile.entries.size).map {
            ProfileEntry("filler-$it", EntryCategory.PROJECT, "Project $it", "", "", "", emptyList(), FactSource.USER_STATED, true)
        }
        val viewModel = loaded(ProfileListSection.EDUCATION, profile.copy(entries = profile.entries + filler))

        assertThat(viewModel.editing().canAdd).isFalse()
    }

    @Test
    fun saveWhileInvalidWritesNothing() = runTest {
        val viewModel = loaded(ProfileListSection.ACHIEVEMENTS)
        viewModel.onTextChange(0, "x".repeat(ProfileLimits.MAX_TEXT_LENGTH + 1))

        viewModel.events.test {
            viewModel.save()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(savedProfile()).isEqualTo(profile)
    }

    @Test
    fun doubleSaveWritesOnce() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY)

        viewModel.events.test {
            viewModel.save()
            viewModel.save()
            assertThat(awaitItem()).isEqualTo(ListEditEvent.Saved)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}

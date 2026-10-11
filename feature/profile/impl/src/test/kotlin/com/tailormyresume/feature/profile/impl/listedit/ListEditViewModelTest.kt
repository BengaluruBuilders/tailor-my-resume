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
    fun savingWithoutEditsConfirmsUnconfirmedImportedItemsAndChangesNothingElse() = runTest {
        val imported = profile.entries.map {
            if (it.category == EntryCategory.ACHIEVEMENT || it.category == EntryCategory.EDUCATION) {
                it.copy(source = FactSource.IMPORTED, isConfirmed = false)
            } else {
                it
            }
        }
        val base = profile.copy(entries = imported)
        listOf(ProfileListSection.ACHIEVEMENTS, ProfileListSection.EDUCATION).forEach { section ->
            profileRepository.sendProfile(base)
            val viewModel = loaded(section, base)

            viewModel.saveAndAwait()

            val category = if (section == ProfileListSection.ACHIEVEMENTS) EntryCategory.ACHIEVEMENT else EntryCategory.EDUCATION
            val saved = savedProfile().category(category)
            assertThat(saved).isNotEmpty()
            assertThat(saved).isEqualTo(base.category(category).map { it.copy(isConfirmed = true) })
            assertThat(savedProfile().entries.filter { it.category != category }).isEqualTo(base.entries.filter { it.category != category })
        }
    }

    @Test
    fun summarySaveThatChangesTextMarksContactUserStated() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY, profile.copy(contactUserStated = false))
        viewModel.onTextChange(0, "Analyst who ships.")

        viewModel.saveAndAwait()

        assertThat(savedProfile().contactUserStated).isTrue()
    }

    @Test
    fun summarySaveWithoutChangeKeepsContactUserStated() = runTest {
        val viewModel = loaded(ProfileListSection.SUMMARY, profile.copy(contactUserStated = false))

        viewModel.saveAndAwait()

        assertThat(savedProfile().contactUserStated).isFalse()
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
    fun educationEditKeepsStructuredFieldsBulletsAndFactIds() = runTest {
        val withDetail = profile.copy(
            entries = profile.entries.map {
                if (it.id == "edu-1") {
                    it.copy(
                        organization = "Symbiosis",
                        startDate = "2017",
                        endDate = "2020",
                        bullets = listOf(EvidenceBullet("edu-1-b1", "Gold medal")),
                        source = FactSource.IMPORTED,
                    )
                } else {
                    it
                }
            },
        )
        val viewModel = loaded(ProfileListSection.EDUCATION, withDetail)
        viewModel.onEducationChange(0, EducationField.ORGANIZATION, "Symbiosis College")

        viewModel.saveAndAwait()

        val edited = savedProfile().category(EntryCategory.EDUCATION).single()
        val original = withDetail.category(EntryCategory.EDUCATION).single()
        assertThat(edited).isEqualTo(original.copy(organization = "Symbiosis College", source = FactSource.USER_EDITED, isConfirmed = true))
    }

    @Test
    fun educationEditUpdatesOneDraftAcrossContinuationChunks() = runTest {
        val first = profile.category(EntryCategory.EDUCATION).single().copy(
            bullets = listOf(EvidenceBullet("edu-1-b1", "First achievement")),
            source = FactSource.IMPORTED,
        )
        val continuation = first.copy(
            id = "edu-1-2",
            bullets = listOf(EvidenceBullet("edu-1-2-b1", "Second achievement")),
        )
        val other = first.copy(id = "edu-other", title = "M.Com", bullets = emptyList())
        val base = profile.copy(entries = profile.entries.flatMap { if (it.id == first.id) listOf(first, continuation, other) else listOf(it) })
        val viewModel = loaded(ProfileListSection.EDUCATION, base)
        assertThat(viewModel.editing().education).hasSize(2)

        viewModel.onEducationChange(0, EducationField.ORGANIZATION, "Symbiosis College")
        viewModel.saveAndAwait()

        val saved = savedProfile().category(EntryCategory.EDUCATION)
        assertThat(saved.map { it.id }).containsExactly(first.id, continuation.id, other.id).inOrder()
        assertThat(saved.take(2)).containsExactly(
            first.copy(organization = "Symbiosis College", source = FactSource.USER_EDITED, isConfirmed = true),
            continuation.copy(organization = "Symbiosis College", source = FactSource.USER_EDITED, isConfirmed = true),
        ).inOrder()
        assertThat(saved.last()).isEqualTo(other.copy(isConfirmed = true))
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
        viewModel.onAdd()
        viewModel.onEducationChange(1, EducationField.TITLE, "  PGDM  ")
        viewModel.onEducationChange(1, EducationField.ORGANIZATION, "IIM Indore")
        assertThat(viewModel.editing().education).hasSize(2)

        viewModel.saveAndAwait()

        val saved = savedProfile()
        val added = saved.category(EntryCategory.EDUCATION).last()
        assertThat(added.title).isEqualTo("PGDM")
        assertThat(added.organization).isEqualTo("IIM Indore")
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
        viewModel.onEducationChange(0, EducationField.TITLE, "")
        viewModel.onAdd()
        viewModel.onEducationChange(1, EducationField.TITLE, "PGDM, IIM Indore")

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

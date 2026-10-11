package com.tailormyresume.feature.onboarding.impl.manual

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.domain.onboarding.SaveManualProfileUseCase
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ManualProfileViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val session = TestSessionRepository().apply {
        sendAccount(SignInAccount("a1", "Priya Deshmukh", "priya.deshmukh@gmail.com"))
    }
    private val profile = TestProfileRepository()

    private fun viewModel() = ManualProfileViewModel(session, profile, SaveManualProfileUseCase(profile, FactIdAllocator()))

    @Test
    fun startsWithTheAccountNameAndEmail() = runTest {
        val state = viewModel().uiState.value

        assertThat(state.fullName).isEqualTo("Priya Deshmukh")
        assertThat(state.email).isEqualTo("priya.deshmukh@gmail.com")
        assertThat(state.phone).isEmpty()
    }

    @Test
    fun emailIsReadOnlyFromAccountAndContinueSavesThenNavigates() = runTest {
        val viewModel = viewModel()
        viewModel.onFieldChange(ManualField.Phone, "+91 98000 00000")
        viewModel.onFieldChange(ManualField.City, "Pune")
        viewModel.onFieldChange(ManualField.JobTitle, "Business Analyst")
        viewModel.onFieldChange(ManualField.Company, "Infosys")

        viewModel.events.test {
            viewModel.onContinue()

            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }

        val saved = checkNotNull(profile.observeProfile().first())
        assertThat(saved.email).isEqualTo("priya.deshmukh@gmail.com")
        assertThat(saved.fullName).isEqualTo("Priya Deshmukh")
        assertThat(saved.phone).isEqualTo("+91 98000 00000")
        assertThat(saved.city).isEqualTo("Pune")
        val entry = saved.entries.single()
        assertThat(entry.title).isEqualTo("Business Analyst")
        assertThat(entry.organization).isEqualTo("Infosys")
        assertThat(entry.source).isEqualTo(FactSource.USER_STATED)
    }

    @Test
    fun continueWithBlankRoleSavesNoEntry() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onContinue()

            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }

        assertThat(checkNotNull(profile.observeProfile().first()).entries).isEmpty()
    }

    @Test
    fun continuingAgainAfterEditingTheRoleReplacesItInsteadOfAddingASecond() = runTest {
        val viewModel = viewModel()
        viewModel.onFieldChange(ManualField.JobTitle, "Busines Analyst")
        viewModel.onFieldChange(ManualField.Company, "Infosys")
        viewModel.events.test {
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
            viewModel.onFieldChange(ManualField.JobTitle, "Business Analyst")
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }

        val roles = checkNotNull(profile.observeProfile().first()).entries.filter { it.category == EntryCategory.EXPERIENCE }
        assertThat(roles.map { it.title }).containsExactly("Business Analyst")
    }

    private suspend fun saveRoleThenReenter(): ManualProfileViewModel {
        val first = viewModel()
        first.onFieldChange(ManualField.JobTitle, "Business Analyst")
        first.onFieldChange(ManualField.Company, "Infosys")
        first.events.test {
            first.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }
        return viewModel()
    }

    @Test
    fun reenteringFillInMyselfPrefillsTheSavedRoleAndContinueKeepsOne() = runTest {
        val second = saveRoleThenReenter()

        assertThat(second.uiState.value.jobTitle).isEqualTo("Business Analyst")
        assertThat(second.uiState.value.company).isEqualTo("Infosys")
        second.events.test {
            second.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }
        val roles = checkNotNull(profile.observeProfile().first()).entries.filter { it.category == EntryCategory.EXPERIENCE }
        assertThat(roles).hasSize(1)
    }

    @Test
    fun severalManualRolesPrefillNothing() = runTest {
        saveRoleThenReenter()
        val saved = checkNotNull(profile.observeProfile().first())
        profile.sendProfile(saved.copy(entries = saved.entries + saved.entries.single().copy(id = "W-other", title = "Analyst II")))

        val third = viewModel()

        assertThat(third.uiState.value.jobTitle).isEmpty()
        assertThat(third.uiState.value.company).isEmpty()
    }

    @Test
    fun returningFromReviewRefreshesEditorChangesAndKeepsOneRole() = runTest {
        val viewModel = viewModel()
        viewModel.onFieldChange(ManualField.Phone, "+91 98000 00000")
        viewModel.onFieldChange(ManualField.City, "Pune")
        viewModel.onFieldChange(ManualField.JobTitle, "Business Analyst")
        viewModel.onFieldChange(ManualField.Company, "Infosys")
        viewModel.events.test {
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }
        val saved = checkNotNull(profile.observeProfile().first())
        val editedRole = saved.entries.single().copy(
            title = "Senior Analyst",
            bullets = listOf(EvidenceBullet("b-1", "Built monthly reports")),
            source = FactSource.USER_EDITED,
        )
        profile.sendProfile(
            saved.copy(
                fullName = "Priya D.",
                phone = "+91 99000 00000",
                city = "Mumbai",
                entries = listOf(editedRole),
            ),
        )

        viewModel.onRouteActive()

        assertThat(viewModel.uiState.value.fullName).isEqualTo("Priya D.")
        assertThat(viewModel.uiState.value.phone).isEqualTo("+91 99000 00000")
        assertThat(viewModel.uiState.value.city).isEqualTo("Mumbai")
        assertThat(viewModel.uiState.value.jobTitle).isEqualTo("Senior Analyst")
        viewModel.events.test {
            viewModel.onContinue()
            assertThat(awaitItem()).isEqualTo(ManualProfileEvent.Saved)
        }
        val final = checkNotNull(profile.observeProfile().first())
        assertThat(final.entries).containsExactly(editedRole)
        assertThat(final.fullName).isEqualTo("Priya D.")
        assertThat(final.phone).isEqualTo("+91 99000 00000")
        assertThat(final.city).isEqualTo("Mumbai")
    }

    @Test
    fun routeRefreshKeepsUnsavedLocalFields() = runTest {
        profile.sendProfile(
            com.tailormyresume.core.model.CandidateProfile(
                fullName = "Priya",
                email = "priya.deshmukh@gmail.com",
                phone = "+91 98000 00000",
                headline = "",
                skills = emptyList(),
                entries = emptyList(),
                city = "Pune",
            ),
        )
        val viewModel = viewModel()
        viewModel.onFieldChange(ManualField.City, "Local draft")
        profile.sendProfile(checkNotNull(profile.observeProfile().first()).copy(phone = "+91 99000 00000", city = "Mumbai"))

        viewModel.onRouteActive()

        assertThat(viewModel.uiState.value.phone).isEqualTo("+91 99000 00000")
        assertThat(viewModel.uiState.value.city).isEqualTo("Local draft")
    }
}

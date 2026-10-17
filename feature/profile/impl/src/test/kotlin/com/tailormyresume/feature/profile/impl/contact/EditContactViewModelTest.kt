package com.tailormyresume.feature.profile.impl.contact

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.profile.ProfileCompleteness
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditContactViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val profileRepository = TestProfileRepository()
    private val session = TestSessionRepository()

    private fun viewModel() = EditContactViewModel(profileRepository, session)

    private fun TestScope.collectUiState(viewModel: EditContactViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
    }

    private fun EditContactViewModel.editing(): EditContactUiState.Editing {
        val state = uiState.value
        assertThat(state).isInstanceOf(EditContactUiState.Editing::class.java)
        return state as EditContactUiState.Editing
    }

    private suspend fun savedProfile(): CandidateProfile = checkNotNull(profileRepository.observeProfile().first())

    private suspend fun EditContactViewModel.saveAndAwait() {
        events.test {
            save()
            assertThat(awaitItem()).isEqualTo(EditContactEvent.Saved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestCanCorrectImportedEmailWithoutChangingAuthentication() = runTest {
        session.sendAccount(SignInAccount("guest", "", "", com.tailormyresume.core.model.AccountMode.ANONYMOUS))
        profileRepository.sendProfile(PrototypeFixtures.returning().profile)
        val subject = viewModel()
        collectUiState(subject)
        assertThat(subject.editing().emailEditable).isTrue()
        subject.onEmailChange("not an email")
        assertThat(subject.editing().canSave).isFalse()
        subject.onEmailChange("candidate@example.com")
        subject.saveAndAwait()
        assertThat(savedProfile().email).isEqualTo("candidate@example.com")
        assertThat(session.observeAccount().first()?.email).isEmpty()
    }

    @Test
    fun googleAccountEmailCannotBeReplacedThroughTheGuestEditorHandler() = runTest {
        val profile = PrototypeFixtures.returning().profile
        session.sendAccount(SignInAccount("google", "Name", profile.email))
        profileRepository.sendProfile(profile)
        val subject = viewModel()
        collectUiState(subject)
        assertThat(subject.editing().emailEditable).isFalse()
        subject.onEmailChange("candidate@example.com")
        subject.saveAndAwait()
        assertThat(savedProfile().email).isEqualTo(profile.email)
    }

    @Test
    fun initialState_beforeProfileIsLoaded_isLoading() {
        assertThat(viewModel().uiState.value).isEqualTo(EditContactUiState.Loading)
    }

    @Test
    fun draftStartsFromTheProfileAndShowsTheEmailReadOnly() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)

        val editing = viewModel.editing()

        assertThat(editing.draft).isEqualTo(
            EditContactDraft(profile.fullName, profile.phone, profile.city, profile.linkedinUrl, profile.portfolioUrl, profile.email),
        )
        assertThat(editing.email).isEqualTo(profile.email)
        assertThat(editing.canSave).isTrue()
    }

    @Test
    fun saveStoresTrimmedFieldsAndLeavesEmailEntriesAndSkills() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onFullNameChange("  Priya D  ")
        viewModel.onPhoneChange(" +91 98200 41736 ")
        viewModel.onCityChange(" Mumbai ")
        viewModel.onLinkedinChange(" linkedin.com/in/priya ")
        viewModel.onPortfolioChange(" https://priya.dev ")

        viewModel.saveAndAwait()

        val saved = savedProfile()
        assertThat(saved.fullName).isEqualTo("Priya D")
        assertThat(saved.phone).isEqualTo("+91 98200 41736")
        assertThat(saved.city).isEqualTo("Mumbai")
        assertThat(saved.linkedinUrl).isEqualTo("linkedin.com/in/priya")
        assertThat(saved.portfolioUrl).isEqualTo("https://priya.dev")
        assertThat(saved.copy(fullName = profile.fullName, phone = profile.phone, city = profile.city, linkedinUrl = profile.linkedinUrl, portfolioUrl = profile.portfolioUrl, contactUserStated = profile.contactUserStated))
            .isEqualTo(profile)
    }

    @Test
    fun saveLeavesEntriesAndSkills() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onCityChange("Mumbai")

        viewModel.saveAndAwait()

        val saved = savedProfile()
        assertThat(saved.entries).isEqualTo(profile.entries)
        assertThat(saved.skills).isEqualTo(profile.skills)
        assertThat(saved.userStatedSkills).isEqualTo(profile.userStatedSkills)
        assertThat(saved.email).isEqualTo(profile.email)
        assertThat(saved.reviewedAt).isEqualTo(profile.reviewedAt)
        assertThat(saved.sourceFileName).isEqualTo(profile.sourceFileName)
        assertThat(saved.summary).isEqualTo(profile.summary)
    }

    @Test
    fun saveMarksTheContactUserStated() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        assertThat(profile.contactUserStated).isFalse()
        val viewModel = viewModel()
        collectUiState(viewModel)

        viewModel.saveAndAwait()

        assertThat(savedProfile().contactUserStated).isTrue()
    }

    @Test
    fun addingLinkedInRaisesCompletenessByFour() = runTest {
        val profile = PrototypeFixtures.returning().profile.copy(linkedinUrl = "")
        profileRepository.sendProfile(profile)
        val before = ProfileCompleteness.percent(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onLinkedinChange("linkedin.com/in/priya")

        viewModel.saveAndAwait()

        assertThat(ProfileCompleteness.percent(savedProfile()) - before).isEqualTo(4)
    }

    @Test
    fun laterProfileEmissionsDoNotOverwriteDraft() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onCityChange("Mumbai")

        profileRepository.sendProfile(profile.copy(city = "Delhi", headline = "Finance lead"))

        assertThat(viewModel.editing().draft.city).isEqualTo("Mumbai")
    }

    @Test
    fun saveWritesIntoTheLatestProfile() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onCityChange("Mumbai")
        profileRepository.sendProfile(profile.copy(headline = "Finance lead"))

        viewModel.saveAndAwait()

        assertThat(savedProfile().headline).isEqualTo("Finance lead")
        assertThat(savedProfile().city).isEqualTo("Mumbai")
    }

    @Test
    fun doubleSaveWritesOnce() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile)
        val viewModel = viewModel()
        collectUiState(viewModel)

        viewModel.events.test {
            viewModel.save()
            viewModel.save()
            assertThat(awaitItem()).isEqualTo(EditContactEvent.Saved)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankNameDisablesSave() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile)
        val viewModel = viewModel()
        collectUiState(viewModel)

        viewModel.onFullNameChange("   ")

        assertThat(viewModel.editing().canSave).isFalse()
    }

    @Test
    fun invalidPhoneDisablesSave_validOrBlankPhoneEnables() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile)
        val viewModel = viewModel()
        collectUiState(viewModel)

        viewModel.onPhoneChange("call me")
        assertThat(viewModel.editing().canSave).isFalse()

        viewModel.onPhoneChange("")
        assertThat(viewModel.editing().canSave).isTrue()

        viewModel.onPhoneChange("+91 98200 41736")
        assertThat(viewModel.editing().canSave).isTrue()
    }

    @Test
    fun overLongFieldDisablesSave() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile)
        val viewModel = viewModel()
        collectUiState(viewModel)

        viewModel.onCityChange("x".repeat(ProfileLimits.MAX_TEXT_LENGTH + 1))
        assertThat(viewModel.editing().canSave).isFalse()

        viewModel.onCityChange("x".repeat(ProfileLimits.MAX_TEXT_LENGTH))
        assertThat(viewModel.editing().canSave).isTrue()

        viewModel.onLinkedinChange("x".repeat(ProfileLimits.MAX_TEXT_LENGTH + 1))
        assertThat(viewModel.editing().canSave).isFalse()
    }

    @Test
    fun saveWhileInvalidWritesNothing() = runTest {
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        val viewModel = viewModel()
        collectUiState(viewModel)
        viewModel.onFullNameChange("")

        viewModel.events.test {
            viewModel.save()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(savedProfile()).isEqualTo(profile)
    }

    @Test
    fun emailFromTheResumeIsNotLabelledAsGoogle() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile.copy(email = "sam@resume.example"))
        session.sendAccount(SignInAccount("uid-1", "Google Name", "google.account@gmail.com"))
        val viewModel = viewModel()
        collectUiState(viewModel)

        assertThat(viewModel.editing().emailFromGoogle).isFalse()
    }

    @Test
    fun emailEqualToTheSignedInAccountIsLabelledGoogle() = runTest {
        profileRepository.sendProfile(PrototypeFixtures.returning().profile.copy(email = "Same@Gmail.com"))
        session.sendAccount(SignInAccount("uid-1", "Google Name", "same@gmail.com"))
        val viewModel = viewModel()
        collectUiState(viewModel)

        assertThat(viewModel.editing().emailFromGoogle).isTrue()
    }
}

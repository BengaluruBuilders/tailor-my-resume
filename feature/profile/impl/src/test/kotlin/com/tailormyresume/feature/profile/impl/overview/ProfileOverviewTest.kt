package com.tailormyresume.feature.profile.impl.overview

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.profile.ProfileCompleteness
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.MainDispatcherRule
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.ListEditNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.api.navigation.ProfileNavKey
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Instant

class YearsOfExperienceTest {

    private val clock = TestClock(Instant.parse("2026-09-30T00:00:00Z"))

    private fun entry(category: EntryCategory, start: String, end: String) = ProfileEntry(
        id = "e-$start-$end",
        category = category,
        title = "Role",
        organization = "Org",
        startDate = start,
        endDate = end,
        bullets = emptyList(),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )

    private fun experience(start: String, end: String) = entry(EntryCategory.EXPERIENCE, start, end)

    @Test
    fun disjointRoles_areSummed() {
        val entries = listOf(experience("Jun 2020", "May 2021"), experience("Jul 2022", "Present"))

        assertThat(yearsOfExperience(entries, clock)).isEqualTo(5)
    }

    @Test
    fun overlappingRoles_areNotDoubleCounted() {
        val entries = listOf(experience("Jan 2020", "Dec 2021"), experience("Jun 2021", "Present"))

        assertThat(yearsOfExperience(entries, clock)).isEqualTo(6)
    }

    @Test
    fun gapBetweenRoles_isNotCounted() {
        val entries = listOf(experience("Jan 2018", "Dec 2018"), experience("Jan 2023", "Dec 2023"))

        assertThat(yearsOfExperience(entries, clock)).isEqualTo(2)
    }

    @Test
    fun lessThanTwelveMonths_floorsToZero() {
        assertThat(yearsOfExperience(listOf(experience("Jun 2020", "Apr 2021")), clock)).isEqualTo(0)
    }

    @Test
    fun janToDecOfOneYear_isOneYear() {
        assertThat(yearsOfExperience(listOf(experience("Jan 2024", "Dec 2024")), clock)).isEqualTo(1)
    }

    @Test
    fun futureEndIsClampedToNow_andFutureStartAddsNothing() {
        assertThat(yearsOfExperience(listOf(experience("Jan 2026", "Dec 2026")), clock)).isEqualTo(0)
        val withNextYear = listOf(experience("Jan 2020", "Dec 2020"), experience("Jan 2027", "Dec 2027"))
        assertThat(yearsOfExperience(withNextYear, clock)).isEqualTo(1)
    }

    @Test
    fun twelveCalendarMonthsAcrossTwoYears_isOneYear() {
        assertThat(yearsOfExperience(listOf(experience("Jun 2020", "May 2021")), clock)).isEqualTo(1)
    }

    @Test
    fun noExperienceEntry_isNull() {
        val entries = listOf(entry(EntryCategory.EDUCATION, "2014", "2018"))

        assertThat(yearsOfExperience(entries, clock)).isNull()
    }

    @Test
    fun unparsableStartDate_isNullEvenWhenAnotherRoleParses() {
        val entries = listOf(experience("Jun 2020", "May 2021"), experience("sometime", "May 2022"))

        assertThat(yearsOfExperience(entries, clock)).isNull()
    }

    @Test
    fun blankStartDate_isNull() {
        val entries = listOf(experience("", "May 2022"))

        assertThat(yearsOfExperience(entries, clock)).isNull()
    }

    @Test
    fun blankEndDate_omitsYears() {
        val entries = listOf(experience("Jun 2020", ""), experience("Jan 2020", "Dec 2021"))

        assertThat(yearsOfExperience(entries, clock)).isNull()
    }

    @Test
    fun unparsableEndDate_omitsYears() {
        val entries = listOf(experience("Jun 2020", "whenever"))

        assertThat(yearsOfExperience(entries, clock)).isNull()
    }

    @Test
    fun present_isTheClockMonthInUtc() {
        val clockUtc = TestClock(Instant.parse("2026-01-01T00:00:00Z"))
        val clockUtcPlusOneDay = TestClock(Instant.parse("2026-01-01T23:59:59Z"))
        val entries = listOf(experience("Jan 2025", "Present"))

        assertThat(yearsOfExperience(entries, clockUtc)).isEqualTo(1)
        assertThat(yearsOfExperience(entries, clockUtcPlusOneDay)).isEqualTo(1)
    }

    @Test
    fun nonExperienceEntries_areIgnored() {
        val entries = listOf(
            entry(EntryCategory.EDUCATION, "Jan 2010", "Dec 2014"),
            experience("Jan 2024", "Dec 2024"),
            entry(EntryCategory.ACHIEVEMENT, "Jan 2000", "Dec 2009"),
        )

        assertThat(yearsOfExperience(entries, clock)).isEqualTo(1)
    }

    @Test
    fun bareYearDates_areParsed() {
        val entries = listOf(experience("2020", "2021"))

        assertThat(yearsOfExperience(entries, clock)).isEqualTo(2)
    }
}

class ProfileViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val profileRepository = TestProfileRepository()
    private val clock = TestClock(Instant.parse("2026-09-30T00:00:00Z"))

    private fun viewModel() = ProfileViewModel(profileRepository, clock)

    private fun TestScope.collectUiState(viewModel: ProfileViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect() }
    }

    private fun ProfileViewModel.content(): ProfileUiState.Content {
        val state = uiState.value
        assertThat(state).isInstanceOf(ProfileUiState.Content::class.java)
        return state as ProfileUiState.Content
    }

    @Test
    fun initialState_isLoadingThenEmptyWhenNoProfile() = runTest {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value).isEqualTo(ProfileUiState.Loading)

        collectUiState(viewModel)

        assertThat(viewModel.uiState.value).isEqualTo(ProfileUiState.Empty)
    }

    @Test
    fun returningProfile_mapsHeaderPercentAndCounts() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)

        val content = viewModel.content()
        assertThat(content.initials).isEqualTo("PD")
        assertThat(content.name).isEqualTo("Priya Deshmukh")
        assertThat(content.headline).isEqualTo("Finance analyst")
        assertThat(content.city).isEqualTo("Pune")
        assertThat(content.years).isEqualTo(yearsOfExperience(profile.entries, clock))
        assertThat(content.years).isNotNull()
        assertThat(content.percent).isEqualTo(ProfileCompleteness.percent(profile))
        assertThat(content.experienceCount).isEqualTo(3)
        assertThat(content.educationCount).isEqualTo(1)
        assertThat(content.skillsCount).isEqualTo(18)
        assertThat(content.achievementsCount).isEqualTo(6)
        assertThat(content.linkedinMissing).isTrue()
        assertThat(content.sourceFileName).isNull()
    }

    @Test
    fun blankLinkedin_showsAddTag() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        val profile = PrototypeFixtures.returning().profile
        profileRepository.sendProfile(profile)
        assertThat(viewModel.content().linkedinMissing).isTrue()

        profileRepository.sendProfile(profile.copy(linkedinUrl = "https://linkedin.com/in/priya"))

        assertThat(viewModel.content().linkedinMissing).isFalse()
    }

    @Test
    fun sourceFileName_isCarried() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        profileRepository.sendProfile(
            PrototypeFixtures.returning().profile.copy(sourceFileName = "Priya_Deshmukh_Resume.pdf"),
        )

        assertThat(viewModel.content().sourceFileName).isEqualTo("Priya_Deshmukh_Resume.pdf")
    }

    @Test
    fun noParsableStartDate_omitsYears() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        val base = PrototypeFixtures.returning().profile
        val profile = base.copy(
            entries = base.entries.map {
                if (it.category == EntryCategory.EXPERIENCE) it.copy(startDate = "") else it
            },
        )
        profileRepository.sendProfile(profile)

        val content = viewModel.content()
        assertThat(content.years).isNull()
        assertThat(content.name).isEqualTo("Priya Deshmukh")
        assertThat(content.headline).isEqualTo("Finance analyst")
        assertThat(content.city).isEqualTo("Pune")
        assertThat(content.experienceCount).isEqualTo(3)
        assertThat(content.educationCount).isEqualTo(1)
        assertThat(content.skillsCount).isEqualTo(18)
        assertThat(content.achievementsCount).isEqualTo(6)
        assertThat(content.percent).isEqualTo(ProfileCompleteness.percent(profile))
    }

    @Test
    fun percentDropsForMissingEndDate() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        val fresh = PrototypeFixtures.fresh().profile
        profileRepository.sendProfile(fresh)
        val withoutEndDate = viewModel.content().percent

        profileRepository.sendProfile(
            fresh.copy(
                entries = fresh.entries.map {
                    if (it.category == EntryCategory.EXPERIENCE && it.endDate.isBlank()) {
                        it.copy(endDate = "May 2022")
                    } else {
                        it
                    }
                },
            ),
        )
        val withEndDate = viewModel.content().percent

        assertThat(withEndDate - withoutEndDate).isEqualTo(6)
    }

    @Test
    fun initials_useFirstTwoWords() = runTest {
        val viewModel = viewModel()
        collectUiState(viewModel)
        val profile = PrototypeFixtures.returning().profile

        profileRepository.sendProfile(profile.copy(fullName = "Madonna"))
        assertThat(viewModel.content().initials).isEqualTo("M")

        profileRepository.sendProfile(profile.copy(fullName = "Priya Anand Deshmukh"))
        assertThat(viewModel.content().initials).isEqualTo("PA")

        profileRepository.sendProfile(profile.copy(fullName = " "))
        assertThat(viewModel.content().initials).isEqualTo("")
    }
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TmrTestDevices.BOARD_QUALIFIERS)
class ProfileDestinationsTest {

    @Test
    fun settingsTarget_mapsToSettingsNavKey() {
        assertThat(ProfileTarget.SETTINGS.navKey()).isEqualTo(SettingsNavKey())
    }

    @Test
    fun contactTarget_mapsToEditContactNavKey() {
        assertThat(ProfileTarget.CONTACT.navKey()).isEqualTo(EditContactNavKey())
    }

    @Test
    fun linkedinTarget_mapsToEditContactNavKey() {
        assertThat(ProfileTarget.LINKEDIN.navKey()).isEqualTo(EditContactNavKey())
    }

    @Test
    fun summaryTarget_mapsToSummaryListEditNavKey() {
        assertThat(ProfileTarget.SUMMARY.navKey()).isEqualTo(ListEditNavKey(ProfileListSection.SUMMARY))
    }

    @Test
    fun educationTarget_mapsToEducationListEditNavKey() {
        assertThat(ProfileTarget.EDUCATION.navKey()).isEqualTo(ListEditNavKey(ProfileListSection.EDUCATION))
    }

    @Test
    fun achievementsTarget_mapsToAchievementsListEditNavKey() {
        assertThat(ProfileTarget.ACHIEVEMENTS.navKey()).isEqualTo(ListEditNavKey(ProfileListSection.ACHIEVEMENTS))
    }

    @Test
    fun experienceTarget_mapsToExperienceNavKey() {
        assertThat(ProfileTarget.EXPERIENCE.navKey()).isEqualTo(ExperienceNavKey())
    }

    @Test
    fun skillsTarget_mapsToSkillsNavKey() {
        assertThat(ProfileTarget.SKILLS.navKey()).isEqualTo(SkillsNavKey())
    }

    @Test
    fun replaceTarget_mapsToUploadNavKey() {
        assertThat(ProfileTarget.REPLACE.navKey()).isEqualTo(UploadNavKey())
    }

    @Test
    fun everyTarget_opensItsKeyAndGoesBackToProfile() {
        ProfileTarget.entries.forEach { target ->
            val navigator = Navigator(NavigationState(NavBackStack<NavKey>(ProfileNavKey())))

            navigator.open(target)

            assertThat(navigator.state.currentKey).isEqualTo(target.navKey())
            assertThat(navigator.goBack()).isTrue()
            assertThat(navigator.state.currentKey).isEqualTo(ProfileNavKey())
        }
    }
}

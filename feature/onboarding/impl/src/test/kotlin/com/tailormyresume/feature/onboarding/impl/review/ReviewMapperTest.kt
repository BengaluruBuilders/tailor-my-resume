package com.tailormyresume.feature.onboarding.impl.review

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import org.junit.Test

class ReviewMapperTest {

    private fun skillsOf(count: Int) = freshReviewProfile().copy(skills = List(count) { "Skill $it" })

    @Test
    fun freshProfileMapsToBannerOptionalRowAndAmberDates() {
        val state = freshReviewProfile().toReviewUiState()

        assertThat(state.sourceFileName).isEqualTo(SAMPLE_SOURCE_FILE)
        assertThat(state.percent).isEqualTo(90)
        assertThat(state.gap).isEqualTo(ReviewGap("exp-tata", "Data Analyst Intern"))
        assertThat(state.canContinue).isFalse()
        assertThat(state.showLinkedinRow).isTrue()
        assertThat(state.contact).isEqualTo(ReviewContact("Priya Deshmukh", "priya.deshmukh@gmail.com", "+91 98200 41736 · Pune"))
        assertThat(state.roles.map { it.title }).containsExactly("Business Analyst", "Data Analyst Intern", "Finance Associate").inOrder()
        assertThat(state.roles.map { it.organization }).containsExactly("Infosys", "Tata Digital", "Bajaj Finserv").inOrder()
        assertThat(state.roles[1].dates).isEqualTo(ReviewDates.EndMissing("Jun 2021"))
        assertThat(state.roles[2].dates).isEqualTo(ReviewDates.Range("Jun 2020", "May 2021"))
        assertThat(state.education).containsExactly(ReviewEducation("B.Com, Finance", "Symbiosis College, Pune · 2020"))
        assertThat(state.skills.count).isEqualTo(18)
    }

    @Test
    fun fixedProfileHasNoGapAndCanContinue() {
        val state = fixedReviewProfile().toReviewUiState()

        assertThat(state.gap).isNull()
        assertThat(state.canContinue).isTrue()
        assertThat(state.percent).isEqualTo(96)
        assertThat(state.roles[1].dates).isEqualTo(ReviewDates.Range("Jun 2021", "May 2022"))
    }

    @Test
    fun linkedinHidesTheOptionalRowAndLiftsTheScoreTo100() {
        val state = fixedReviewProfile().copy(linkedinUrl = SAMPLE_LINKEDIN).toReviewUiState()

        assertThat(state.showLinkedinRow).isFalse()
        assertThat(state.percent).isEqualTo(100)
    }

    @Test
    fun noSourceFileNameLeavesItNull() {
        assertThat(freshReviewProfile(sourceFileName = null).toReviewUiState().sourceFileName).isNull()
        assertThat(freshReviewProfile(sourceFileName = "  ").toReviewUiState().sourceFileName).isNull()
    }

    @Test
    fun presentEndDateReadsNowAndIsNotAGap() {
        val state = fixedReviewProfile().toReviewUiState()

        assertThat(state.roles[0].dates).isEqualTo(ReviewDates.Current("Jul 2022"))
        assertThat(state.gap).isNull()

        val lowercase = fixedReviewProfile().withEntries(importedRole("a", endDate = "present")).toReviewUiState()
        assertThat(lowercase.roles.single().dates).isEqualTo(ReviewDates.Current("Jul 2022"))
    }

    @Test
    fun roleWithoutStartDateShowsNoDates() {
        val state = fixedReviewProfile().withEntries(importedRole("a", startDate = "", endDate = "")).toReviewUiState()

        assertThat(state.roles.single().dates).isEqualTo(ReviewDates.None)
        assertThat(state.gap).isNull()
    }

    @Test
    fun splitRoleShowsOneRowAndCountsOnce() {
        val profile = freshReviewProfile().withEntries(
            importedRole("a", title = "Data Analyst Intern", organization = "Tata Digital", startDate = "Jun 2021", endDate = ""),
            importedRole("a-2", title = "Data Analyst Intern", organization = "Tata Digital", startDate = "Jun 2021", endDate = ""),
            importedRole("b", title = "Finance Associate", organization = "Bajaj Finserv", startDate = "Jun 2020", endDate = "May 2021"),
        )

        val state = profile.toReviewUiState()

        assertThat(state.roles.map { it.title }).containsExactly("Data Analyst Intern", "Finance Associate").inOrder()
        assertThat(state.roles.first().dates).isEqualTo(ReviewDates.EndMissing("Jun 2021"))
        assertThat(state.gap?.entryId).isEqualTo("a")
    }

    @Test
    fun skillsOverFiveShowPlusN() {
        val skills = skillsOf(18).toReviewUiState().skills

        assertThat(skills.chips).containsExactly("Skill 0", "Skill 1", "Skill 2", "Skill 3", "Skill 4").inOrder()
        assertThat(skills.more).isEqualTo(13)
        assertThat(skills.count).isEqualTo(18)
    }

    @Test
    fun skillsAtOrUnderFiveShowNoPill() {
        assertThat(skillsOf(5).toReviewUiState().skills.more).isEqualTo(0)
        assertThat(skillsOf(5).toReviewUiState().skills.chips).hasSize(5)
        assertThat(skillsOf(2).toReviewUiState().skills.more).isEqualTo(0)
        assertThat(skillsOf(6).toReviewUiState().skills.more).isEqualTo(1)
    }

    @Test
    fun manualProfileMapsWithoutEmptyCards() {
        val manual = CandidateProfile(
            fullName = "Priya Deshmukh",
            email = "priya.deshmukh@gmail.com",
            phone = "",
            headline = "",
            skills = emptyList(),
            entries = listOf(
                ProfileEntry("W-1", EntryCategory.EXPERIENCE, "Business Analyst", "Infosys", "", "", emptyList(), FactSource.USER_STATED, true),
            ),
            city = "Pune",
        )

        val state = manual.toReviewUiState()

        assertThat(state.contact.phoneAndCity).isEqualTo("Pune")
        assertThat(state.roles.single().dates).isEqualTo(ReviewDates.None)
        assertThat(state.skills).isEqualTo(ReviewSkills(0, emptyList(), 0))
        assertThat(state.education).isEmpty()
        assertThat(state.gap).isNull()
        assertThat(state.sourceFileName).isNull()
        assertThat(state.canContinue).isTrue()
    }
}

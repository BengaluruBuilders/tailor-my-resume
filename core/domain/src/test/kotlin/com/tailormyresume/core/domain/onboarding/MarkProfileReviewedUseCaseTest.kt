package com.tailormyresume.core.domain.onboarding

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MarkProfileReviewedUseCaseTest {

    private val repository = TestProfileRepository()
    private val clock = TestClock()
    private val useCase = MarkProfileReviewedUseCase(repository, clock)

    private fun entry(
        id: String,
        category: EntryCategory,
        endDate: String = "May 2022",
        startDate: String = "Jun 2021",
        bullets: List<EvidenceBullet> = emptyList(),
    ) = ProfileEntry(id, category, "Title $id", "Org", startDate, endDate, bullets, FactSource.IMPORTED, isConfirmed = false)

    private fun profile(vararg entries: ProfileEntry) = CandidateProfile(
        fullName = "Priya Deshmukh",
        email = "priya@example.com",
        phone = "",
        headline = "",
        skills = listOf("SQL"),
        entries = entries.toList(),
    )

    private val allCategories = listOf(
        entry("exp", EntryCategory.EXPERIENCE, bullets = listOf(EvidenceBullet("exp-b1", "Built dashboards"))),
        entry("edu", EntryCategory.EDUCATION, startDate = "", endDate = "2020"),
        entry("ach", EntryCategory.ACHIEVEMENT, startDate = "", endDate = ""),
        entry("prj", EntryCategory.PROJECT, startDate = "", endDate = ""),
        entry("crt", EntryCategory.CERTIFICATION, startDate = "", endDate = ""),
    )

    private suspend fun saved() = checkNotNull(repository.observeProfile().first())

    @Test
    fun setsReviewedAtFromInjectedClock() = runTest {
        repository.sendProfile(profile(*allCategories.toTypedArray()))

        val result = useCase()

        assertThat(result).isInstanceOf(MarkReviewedResult.Reviewed::class.java)
        assertThat(saved().reviewedAt).isEqualTo(clock.instant)
        assertThat((result as MarkReviewedResult.Reviewed).profile).isEqualTo(saved())
    }

    @Test
    fun confirmsOnlyExperienceAndEducation() = runTest {
        repository.sendProfile(profile(*allCategories.toTypedArray()))

        useCase()

        val confirmed = saved().entries.filter { it.isConfirmed }.map { it.category }
        assertThat(confirmed).containsExactly(EntryCategory.EXPERIENCE, EntryCategory.EDUCATION)
    }

    @Test
    fun achievementsStayUnconfirmed() = runTest {
        repository.sendProfile(profile(*allCategories.toTypedArray()))

        useCase()

        val others = saved().entries.filter { it.category !in setOf(EntryCategory.EXPERIENCE, EntryCategory.EDUCATION) }
        assertThat(others.map { it.id }).containsExactly("ach", "prj", "crt")
        assertThat(others.none { it.isConfirmed }).isTrue()
    }

    @Test
    fun entryAndBulletIdsUnchanged() = runTest {
        val before = profile(*allCategories.toTypedArray())
        repository.sendProfile(before)

        useCase()

        val after = saved()
        assertThat(after.entries.map { it.id }).containsExactlyElementsIn(before.entries.map { it.id }).inOrder()
        assertThat(after.entries.flatMap { e -> e.bullets.map { it.id } }).containsExactly("exp-b1")
        assertThat(after.entries.map { it.copy(isConfirmed = false) }).isEqualTo(before.entries)
        assertThat(after.copy(entries = before.entries, reviewedAt = null)).isEqualTo(before)
    }

    @Test
    fun refusesWithRequiredGapAndLeavesProfileUntouched() = runTest {
        val gapped = profile(entry("exp", EntryCategory.EXPERIENCE, endDate = ""), allCategories[1])
        repository.sendProfile(gapped)

        val result = useCase()

        assertThat(result).isInstanceOf(MarkReviewedResult.RequiredInfoMissing::class.java)
        assertThat((result as MarkReviewedResult.RequiredInfoMissing).gaps.map { it.entryId }).containsExactly("exp")
        assertThat(saved()).isEqualTo(gapped)
    }

    @Test
    fun nothingToReviewWritesNothing() = runTest {
        assertThat(useCase()).isEqualTo(MarkReviewedResult.NothingToReview)
        assertThat(repository.observeProfile().first()).isNull()

        val empty = CandidateProfile("", "", "", "", emptyList(), emptyList())
        repository.sendProfile(empty)

        assertThat(useCase()).isEqualTo(MarkReviewedResult.NothingToReview)
        assertThat(saved()).isEqualTo(empty)
    }

    @Test
    fun staleSnapshotDoesNotConfirmOrOverwriteLaterEdits() = runTest {
        val displayed = profile(*allCategories.toTypedArray())
        val edited = displayed.copy(linkedinUrl = "linkedin.com/in/priya", skills = listOf("SQL", "Python"))
        repository.sendProfile(edited)

        assertThat(useCase(displayed)).isEqualTo(MarkReviewedResult.ProfileChanged)
        assertThat(saved()).isEqualTo(edited)
    }
}

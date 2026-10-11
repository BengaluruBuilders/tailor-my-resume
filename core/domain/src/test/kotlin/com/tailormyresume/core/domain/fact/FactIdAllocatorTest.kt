package com.tailormyresume.core.domain.fact

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import org.junit.Test

class FactIdAllocatorTest {
    private val allocator = FactIdAllocator()

    @Test
    fun everyCategoryHasItsOwnPrefix() {
        assertThat(nextId(EntryCategory.EDUCATION)).isEqualTo("E-01")
        assertThat(nextId(EntryCategory.EXPERIENCE)).isEqualTo("W-01")
        assertThat(nextId(EntryCategory.PROJECT)).isEqualTo("P-01")
        assertThat(nextId(EntryCategory.CERTIFICATION)).isEqualTo("CT-01")
        assertThat(nextId(EntryCategory.ACHIEVEMENT)).isEqualTo("X-01")
    }

    @Test
    fun anUncategorisedFactTakesTheFirstFreeUNumber() {
        assertThat(allocator.nextUncategorisedId(emptyList())).isEqualTo("U-01")
        assertThat(allocator.nextUncategorisedId(listOf(entry("U-01")))).isEqualTo("U-02")
    }

    @Test
    fun anExperienceWithInternInItsTitleIsAnInternship() {
        val id = allocator.nextId(EntryCategory.EXPERIENCE, emptyList(), "Android DEVELOPER Intern")

        assertThat(id).isEqualTo("I-01")
    }

    @Test
    fun anExperienceWithoutInternInItsTitleIsWork() {
        assertThat(allocator.nextId(EntryCategory.EXPERIENCE, emptyList(), "Data Operations Associate")).isEqualTo("W-01")
    }

    @Test
    fun anEmptyProfileStartsAtOne() {
        assertThat(allocator.nextId(EntryCategory.PROJECT, emptyList())).isEqualTo("P-01")
    }

    @Test
    fun theNumberIsZeroPaddedToTwoDigits() {
        val existing = (1..9).map { entry("P-${it.toString().padStart(2, '0')}") }

        assertThat(allocator.nextId(EntryCategory.PROJECT, existing)).isEqualTo("P-10")
    }

    @Test
    fun theFirstFreeNumberInTheRunIsUsed() {
        val existing = listOf(entry("P-01"), entry("P-02"), entry("P-03"))

        assertThat(allocator.nextId(EntryCategory.PROJECT, existing)).isEqualTo("P-04")
    }

    @Test
    fun aGapInTheRunIsFilledBeforeCountingUp() {
        val existing = listOf(entry("P-01"), entry("P-03"))

        assertThat(allocator.nextId(EntryCategory.PROJECT, existing)).isEqualTo("P-02")
    }

    @Test
    fun aTakenNumberIsNeverReused() {
        val existing = listOf(entry("W-01"), entry("W-02"), entry("W-03"))

        val id = allocator.nextId(EntryCategory.EXPERIENCE, existing)

        assertThat(id).isNotIn(existing.map { it.id })
    }

    @Test
    fun anotherCategoriesIdsDoNotBlockTheNumber() {
        val existing = listOf(entry("E-01"), entry("W-01"), entry("P-01"))

        assertThat(allocator.nextId(EntryCategory.PROJECT, existing)).isEqualTo("P-02")
    }

    @Test
    fun eachCategoryCountsItsOwnRun() {
        val existing = listOf(entry("E-01"), entry("E-02"), entry("W-01"))

        assertThat(allocator.nextId(EntryCategory.EDUCATION, existing)).isEqualTo("E-03")
        assertThat(allocator.nextId(EntryCategory.EXPERIENCE, existing)).isEqualTo("W-02")
    }

    @Test
    fun theCanonicalProfileLeavesRoomInEveryRun() {
        val existing = listOf(entry("E-01"), entry("E-02"), entry("W-01"), entry("W-02"))

        assertThat(allocator.nextId(EntryCategory.EDUCATION, existing)).isEqualTo("E-03")
        assertThat(allocator.nextId(EntryCategory.EXPERIENCE, existing)).isEqualTo("W-03")
    }

    @Test
    fun neverUsedIdSkipsTakenAndKeepsPrefix() {
        val taken = listOf(entry("E-01"), entry("X-0123abcd"))

        val education = allocator.neverUsedId(EntryCategory.EDUCATION, taken)
        val achievement = allocator.neverUsedId(EntryCategory.ACHIEVEMENT, taken)
        val intern = allocator.neverUsedId(EntryCategory.EXPERIENCE, taken, "Data Intern")

        assertThat(education).matches("E-[0-9a-f]{8}")
        assertThat(achievement).matches("X-[0-9a-f]{8}")
        assertThat(intern).matches("I-[0-9a-f]{8}")
        assertThat(setOf(education, achievement, intern) intersect taken.map { it.id }.toSet()).isEmpty()
    }

    @Test
    fun neverUsedIdIsDifferentEachCall() {
        val ids = (1..50).map { allocator.neverUsedId(EntryCategory.EDUCATION, emptyList()) }

        assertThat(ids.toSet()).hasSize(50)
    }

    private fun nextId(category: EntryCategory) = allocator.nextId(category, emptyList())

    private fun entry(id: String) = ProfileEntry(
        id = id,
        category = EntryCategory.PROJECT,
        title = "Fact $id",
        organization = "",
        startDate = "",
        endDate = "",
        bullets = listOf(EvidenceBullet("$id-b1", "Text for $id.")),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )
}

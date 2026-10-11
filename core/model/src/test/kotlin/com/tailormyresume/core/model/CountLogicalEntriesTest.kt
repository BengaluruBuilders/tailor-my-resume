package com.tailormyresume.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CountLogicalEntriesTest {
    private fun entry(
        id: String,
        category: EntryCategory = EntryCategory.EXPERIENCE,
        title: String = "Business Analyst",
        organization: String = "Infosys",
        startDate: String = "Jul 2022",
        endDate: String = "Present",
    ) = ProfileEntry(id, category, title, organization, startDate, endDate, emptyList(), FactSource.IMPORTED, isConfirmed = false)

    @Test
    fun continuationCountsOnce() {
        val entries = listOf(entry("a"), entry("a-2"), entry("b", title = "Data Analyst Intern", startDate = "Jun 2021"))

        assertEquals(2, entries.countLogicalEntries(EntryCategory.EXPERIENCE))
    }

    @Test
    fun onlyTheRequestedCategoryCounts() {
        val entries = listOf(entry("a"), entry("e", EntryCategory.EDUCATION, title = "B.Com"), entry("e-2", EntryCategory.EDUCATION, title = "M.Com"))

        assertEquals(1, entries.countLogicalEntries(EntryCategory.EXPERIENCE))
        assertEquals(2, entries.countLogicalEntries(EntryCategory.EDUCATION))
        assertEquals(0, entries.countLogicalEntries(EntryCategory.ACHIEVEMENT))
    }

    @Test
    fun identicalRolesSeparatedByAnotherEntryAreDistinct() {
        val entries = listOf(entry("a"), entry("e", EntryCategory.EDUCATION, title = "B.Com"), entry("a-again"))

        assertEquals(2, entries.countLogicalEntries(EntryCategory.EXPERIENCE))
    }

    @Test
    fun emptyListCountsZero() {
        assertEquals(0, emptyList<ProfileEntry>().countLogicalEntries(EntryCategory.EXPERIENCE))
    }
}

package com.tailormyresume.core.database.dao

import com.tailormyresume.core.database.json.EvidenceBulletDto
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.database.model.ProfileEntryEntity
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ProfileDaoTest : DatabaseTest() {

    @Test
    fun emptyDatabaseObservesNullProfile() = runTest {
        assertNull(profileDao.observePopulatedProfile().first())
    }

    @Test
    fun replaceProfile_savesProfileWithItsEntries() = runTest {
        profileDao.replaceProfile(testProfile(), listOf(testEntry("e1", 0), testEntry("e2", 1)))

        val populated = profileDao.observePopulatedProfile().first()

        assertEquals("Asha Rao", populated?.profile?.fullName)
        assertEquals(listOf("Kotlin", "Room"), populated?.profile?.skills)
        assertEquals(setOf("e1", "e2"), populated?.entries?.map { it.id }?.toSet())
    }

    @Test
    fun entryJsonColumnsSurviveStorage() = runTest {
        profileDao.replaceProfile(testProfile(), listOf(testEntry("e1", 0)))

        val entry = profileDao.observePopulatedProfile().first()?.entries?.single()

        assertEquals(listOf(EvidenceBulletDto("b-e1", "Built things")), entry?.bullets)
        assertEquals(EntryCategory.PROJECT, entry?.category)
        assertEquals(FactSource.IMPORTED, entry?.source)
    }

    @Test
    fun replaceProfile_removesEntriesThatAreNotInTheNewList() = runTest {
        profileDao.replaceProfile(testProfile(), listOf(testEntry("e1", 0), testEntry("e2", 1)))

        profileDao.replaceProfile(testProfile(), listOf(testEntry("e2", 0)))

        val entries = profileDao.observePopulatedProfile().first()?.entries
        assertEquals(listOf("e2"), entries?.map { it.id })
    }

    @Test
    fun upsertProfile_updatesTheSingleRow() = runTest {
        profileDao.upsertProfile(testProfile())
        profileDao.upsertProfile(testProfile().copy(headline = "Staff engineer"))

        val populated = profileDao.observePopulatedProfile().first()

        assertEquals("Staff engineer", populated?.profile?.headline)
    }

    @Test
    fun deleteProfile_cascadesToEntries() = runTest {
        profileDao.replaceProfile(testProfile(), listOf(testEntry("e1", 0)))

        profileDao.deleteProfile()

        assertNull(profileDao.observePopulatedProfile().first())
        profileDao.upsertProfile(testProfile())
        assertEquals(emptyList(), profileDao.observePopulatedProfile().first()?.entries)
    }

    @Test
    fun matchingReviewSnapshotCanConfirmEntries() = runTest {
        val profile = testProfile()
        val entries = listOf(testEntry("e1", 0).copy(isConfirmed = false))
        val confirmed = entries.map { it.copy(isConfirmed = true) }
        profileDao.replaceProfile(profile, entries)

        assertTrue(profileDao.replaceProfileIfUnchanged(profile, entries, profile, confirmed))
        assertEquals(confirmed, profileDao.readEntries())
    }

    @Test
    fun changedProfileOrEntryRejectsTheReviewSnapshot() = runTest {
        val profile = testProfile()
        val entries = listOf(testEntry("e1", 0).copy(isConfirmed = false))
        val confirmed = entries.map { it.copy(isConfirmed = true) }
        val latestProfile = profile.copy(email = "updated@example.test")
        profileDao.replaceProfile(latestProfile, entries)

        assertFalse(profileDao.replaceProfileIfUnchanged(profile, entries, profile, confirmed))
        assertEquals(latestProfile, profileDao.readProfile())
        assertEquals(entries, profileDao.readEntries())

        val latestEntries = entries.map { it.copy(bullets = listOf(EvidenceBulletDto("b-e1", "An unseen edit"))) }
        profileDao.replaceProfile(profile, latestEntries)

        assertFalse(profileDao.replaceProfileIfUnchanged(profile, entries, profile, confirmed))
        assertEquals(latestEntries, profileDao.readEntries())
    }

    private fun testProfile() = ProfileEntity(
        fullName = "Asha Rao",
        email = "asha@example.com",
        phone = "123",
        headline = "Android developer",
        skills = listOf("Kotlin", "Room"),
    )

    private fun testEntry(id: String, position: Int) = ProfileEntryEntity(
        id = id,
        position = position,
        category = EntryCategory.PROJECT,
        title = "Title $id",
        organization = "Org",
        startDate = "2023",
        endDate = "2024",
        bullets = listOf(EvidenceBulletDto("b-$id", "Built things")),
        source = FactSource.IMPORTED,
        isConfirmed = true,
    )
}

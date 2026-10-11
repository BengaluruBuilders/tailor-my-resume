package com.tailormyresume.core.testing.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.canonicalProfileWithoutEntries
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

abstract class ProfileRepositoryContractTest {

    protected abstract fun createProfileRepository(): ProfileRepository

    @Test
    fun anAccountWithNoStoredProfileHasNoProfile() = runTest {
        val repository = createProfileRepository()

        assertThat(repository.observeProfile().first()).isNull()
    }

    @Test
    fun aSavedProfileComesBackUnchanged() = runTest {
        val repository = createProfileRepository()
        val profile: CandidateProfile = canonicalCandidateProfile

        repository.saveProfile(profile)

        assertThat(repository.observeProfile().first()).isEqualTo(profile)
    }

    @Test
    fun savingASecondProfileReplacesTheFirstOne() = runTest {
        val repository = createProfileRepository()
        repository.saveProfile(canonicalCandidateProfile)

        repository.saveProfile(canonicalProfileWithoutEntries)

        assertThat(repository.observeProfile().first()).isEqualTo(canonicalProfileWithoutEntries)
    }

    @Test
    fun savingAProfileTwiceDoesNotDuplicateItsEntries() = runTest {
        val repository = createProfileRepository()
        val profile: CandidateProfile = canonicalCandidateProfile

        repository.saveProfile(profile)
        repository.saveProfile(profile)

        val stored = repository.observeProfile().first()
        assertThat(stored?.entries?.map { entry -> entry.id }).hasSize(profile.entries.size)
    }

    @Test
    fun everyEntryKeepsItsFactsAndItsProvenance() = runTest {
        val repository = createProfileRepository()
        val profile: CandidateProfile = canonicalCandidateProfile

        repository.saveProfile(profile)

        val stored = repository.observeProfile().first()
        assertThat(stored?.entries?.map { entry -> entry.id })
            .containsExactlyElementsIn(profile.entries.map { entry -> entry.id })
        assertThat(stored?.entries?.filter { entry -> entry.isConfirmed }?.size)
            .isEqualTo(profile.entries.count { entry -> entry.isConfirmed })
    }

    @Test
    fun conditionalSaveAcceptsOnlyTheCurrentSnapshot() = runTest {
        val repository = createProfileRepository()
        val original = canonicalCandidateProfile
        val reviewed = original.copy(reviewedAt = Instant.fromEpochSeconds(1_700_000_000))
        repository.saveProfile(original)

        assertThat(repository.saveProfileIfUnchanged(original, reviewed)).isTrue()
        assertThat(repository.saveProfileIfUnchanged(original, original)).isFalse()
        assertThat(repository.observeProfile().first()).isEqualTo(reviewed)
    }

    @Test
    fun conditionalSaveKeepsChangedContactAndBullets() = runTest {
        val repository = createProfileRepository()
        val original = canonicalCandidateProfile
        val edited = original.copy(
            email = "edited@example.test",
            entries = original.entries.map { entry ->
                entry.copy(bullets = entry.bullets.map { it.copy(text = "Edited evidence") })
            },
        )
        repository.saveProfile(edited)

        assertThat(repository.saveProfileIfUnchanged(original, original.copy(reviewedAt = Instant.fromEpochSeconds(1)))).isFalse()
        assertThat(repository.observeProfile().first()).isEqualTo(edited)
    }

    @Test
    fun clearingRemovesTheStoredProfile() = runTest {
        val repository = createProfileRepository()
        repository.saveProfile(canonicalCandidateProfile)

        repository.clearProfile()

        assertThat(repository.observeProfile().first()).isNull()
    }

    @Test
    fun clearingAnEmptyAccountIsSafe() = runTest {
        val repository = createProfileRepository()

        repository.clearProfile()

        assertThat(repository.observeProfile().first()).isNull()
    }

    @Test
    fun aProfileCanBeSavedAgainAfterItWasCleared() = runTest {
        val repository = createProfileRepository()
        repository.saveProfile(canonicalCandidateProfile)
        repository.clearProfile()

        repository.saveProfile(canonicalProfileWithoutEntries)

        assertThat(repository.observeProfile().first()).isEqualTo(canonicalProfileWithoutEntries)
    }

    @Test
    fun roundTripsEveryNewProfileField() = runTest {
        val repository = createProfileRepository()
        val profile = canonicalCandidateProfile.copy(
            city = "Pune",
            linkedinUrl = "https://www.linkedin.com/in/priya",
            portfolioUrl = "https://priya.example.com",
            summary = "Finance analyst with four years of SQL work.",
            sourceFileName = "Priya_Resume.pdf",
            reviewedAt = Instant.parse("2026-10-02T09:30:00Z"),
            entries = canonicalCandidateProfile.entries.map { it.copy(source = FactSource.USER_ANSWER) },
        )

        repository.saveProfile(profile)

        assertThat(repository.observeProfile().first()).isEqualTo(profile)
    }
}

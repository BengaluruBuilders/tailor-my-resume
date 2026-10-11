package com.tailormyresume.core.domain.onboarding

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.repository.TestProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveManualProfileUseCaseTest {

    private val repository = TestProfileRepository()
    private val useCase = SaveManualProfileUseCase(repository, FactIdAllocator())

    private val contact = ManualContact(
        fullName = "  Priya Deshmukh ",
        email = "priya@example.com",
        phone = " +91 98000 00000",
        city = "Pune ",
        jobTitle = " Business Analyst",
        company = "Infosys ",
    )

    @Test
    fun savesContactFieldsAndOneUserStatedEntryWithAllocatedId() = runTest {
        val returned = useCase(contact)

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(returned).isEqualTo(saved)
        assertThat(saved.fullName).isEqualTo("Priya Deshmukh")
        assertThat(saved.email).isEqualTo("priya@example.com")
        assertThat(saved.phone).isEqualTo("+91 98000 00000")
        assertThat(saved.city).isEqualTo("Pune")
        val entry = saved.entries.single()
        assertThat(entry.id).matches("W-[0-9a-f]{8}")
        assertThat(entry.category).isEqualTo(EntryCategory.EXPERIENCE)
        assertThat(entry.title).isEqualTo("Business Analyst")
        assertThat(entry.organization).isEqualTo("Infosys")
        assertThat(entry.source).isEqualTo(FactSource.USER_STATED)
        assertThat(saved.reviewedAt).isNull()
    }

    @Test
    fun blankRoleSavesNoEntry() = runTest {
        useCase(contact.copy(jobTitle = "  ", company = ""))

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries).isEmpty()
        assertThat(saved.fullName).isEqualTo("Priya Deshmukh")
    }

    @Test
    fun keepsExistingUserFacts() = runTest {
        val kept = ProfileEntry(
            "W-01", EntryCategory.EXPERIENCE, "Kept role", "Acme", "2022", "2024", emptyList(),
            FactSource.USER_STATED, isConfirmed = true,
        )
        repository.sendProfile(
            CandidateProfile("Old", "", "", "", listOf("Kotlin"), listOf(kept), userStatedSkills = listOf("Kotlin")),
        )

        useCase(contact)

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries.first().id).isEqualTo("W-01")
        assertThat(saved.entries.last().id).isNotEqualTo("W-01")
        assertThat(saved.entries).hasSize(2)
        assertThat(saved.entries.first()).isEqualTo(kept)
        assertThat(saved.skills).containsExactly("Kotlin")
        assertThat(saved.userStatedSkills).containsExactly("Kotlin")
    }

    @Test
    fun deletedRoleIdIsNeverGivenToANewRole() = runTest {
        val first = useCase(contact).entries.single()
        repository.sendProfile(checkNotNull(repository.observeProfile().first()).copy(entries = emptyList()))

        val second = useCase(contact).entries.single()

        assertThat(second.id).isNotEqualTo(first.id)
    }

    @Test
    fun importedRoleWithTheSameShapeIsNeverReplaced() = runTest {
        val imported = ProfileEntry(
            "E-01", EntryCategory.EXPERIENCE, "Imported role", "Acme", "", "", emptyList(),
            FactSource.IMPORTED, isConfirmed = true,
        )
        repository.sendProfile(CandidateProfile("Old", "", "", "", emptyList(), listOf(imported)))

        useCase(contact, replacingEntryId = imported.id)

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries).hasSize(2)
        assertThat(saved.entries.first()).isEqualTo(imported)
    }
}

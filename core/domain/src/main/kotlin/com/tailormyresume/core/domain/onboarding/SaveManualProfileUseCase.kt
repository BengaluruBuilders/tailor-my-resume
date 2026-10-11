package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ManualContact(
    val fullName: String,
    val email: String,
    val phone: String,
    val city: String,
    val jobTitle: String,
    val company: String,
)

class SaveManualProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val factIdAllocator: FactIdAllocator,
) {
    suspend operator fun invoke(contact: ManualContact, replacingEntryId: String? = null): CandidateProfile {
        val existing = profileRepository.observeProfile().first()
        val all = existing?.entries.orEmpty()
        val replaced = all.firstOrNull { it.id == replacingEntryId && it.isOwnedManualRole() }
        val kept = all
        val title = contact.jobTitle.trim()
        val company = contact.company.trim()
        val added = if (title.isEmpty() && company.isEmpty()) {
            emptyList()
        } else {
            listOf(
                replaced?.copy(title = title, organization = company)
                    ?: ProfileEntry(
                        id = factIdAllocator.neverUsedId(EntryCategory.EXPERIENCE, kept, title),
                        category = EntryCategory.EXPERIENCE,
                        title = title,
                        organization = company,
                        startDate = "",
                        endDate = "",
                        bullets = emptyList(),
                        source = FactSource.USER_STATED,
                        isConfirmed = true,
                    ),
            )
        }
        val saved = (existing ?: EMPTY_PROFILE).copy(
            fullName = contact.fullName.trim(),
            email = contact.email.trim(),
            phone = contact.phone.trim(),
            city = contact.city.trim(),
            contactUserStated = true,
            entries = if (replaced == null) kept + added else kept.flatMap { if (it.id == replaced.id) added else listOf(it) },
            reviewedAt = null,
        )
        profileRepository.saveProfile(saved)
        return saved
    }

    private fun ProfileEntry.isOwnedManualRole(): Boolean =
        category == EntryCategory.EXPERIENCE &&
            (source == FactSource.USER_STATED || source == FactSource.USER_EDITED)

    private companion object {
        val EMPTY_PROFILE = CandidateProfile(
            fullName = "",
            email = "",
            phone = "",
            headline = "",
            skills = emptyList(),
            entries = emptyList(),
        )
    }
}

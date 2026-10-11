package com.tailormyresume.core.domain.onboarding

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private data class Identity(
    val fullName: String,
    val email: String,
    val phone: String,
    val city: String,
    val linkedinUrl: String,
    val portfolioUrl: String,
    val headline: String,
    val summary: String,
) {
    fun isBlank() = listOf(fullName, email, phone, city, linkedinUrl, portfolioUrl, headline, summary).all { it.isBlank() }
}

class SaveImportedProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val factIdAllocator: FactIdAllocator,
) {
    suspend operator fun invoke(parsed: CandidateProfile, sourceFileName: String?): CandidateProfile {
        val existing = profileRepository.observeProfile().first() ?: EMPTY_PROFILE
        val importedName = personNameOrNull(parsed.fullName).orEmpty()
        val identity = chooseIdentity(existing, parsed, importedName)
        val kept = existing.entries.filter { it.source != FactSource.IMPORTED }
        val allocated = existing.entries.toMutableList()
        val imported = parsed.entries.map { entry ->
            reidentify(entry, allocated).also { allocated += it }
        }
        val saved = existing.copy(
            fullName = identity.fullName,
            email = identity.email,
            phone = identity.phone,
            headline = identity.headline,
            city = identity.city,
            linkedinUrl = identity.linkedinUrl,
            portfolioUrl = identity.portfolioUrl,
            summary = identity.summary,
            skills = (existing.userStatedSkills + parsed.skills)
                .distinctBy { it.lowercase() }
                .take(ProfileLimits.MAX_SKILLS),
            entries = limitEntries(kept + imported),
            userStatedSkills = existing.userStatedSkills,
            sourceFileName = sourceFileName,
            reviewedAt = null,
        )
        profileRepository.saveProfile(saved)
        return saved
    }

    private fun chooseIdentity(existing: CandidateProfile, parsed: CandidateProfile, importedName: String): Identity {
        val existingIdentity = existing.identity(existing.fullName)
        val importedIdentity = parsed.identity(importedName)
        return when {
            existingIdentity.isBlank() -> importedIdentity
            existing.contactIsUserStated() -> existingIdentity
            importedName.isNotEmpty() -> importedIdentity
            else -> existingIdentity
        }
    }

    private fun CandidateProfile.identity(name: String) =
        Identity(name, email, phone, city, linkedinUrl, portfolioUrl, headline, summary)

    private fun CandidateProfile.contactIsUserStated(): Boolean {
        val hadImport = sourceFileName != null || entries.isNotEmpty()
        return entries.any { it.source != FactSource.IMPORTED } || !hadImport
    }

    private fun reidentify(entry: ProfileEntry, allocated: List<ProfileEntry>): ProfileEntry {
        val newId = factIdAllocator.neverUsedId(entry.category, allocated, entry.title)
        val oldPrefix = entry.id + "-"
        return entry.copy(
            id = newId,
            bullets = entry.bullets.map { bullet ->
                if (bullet.id.startsWith(oldPrefix)) bullet.copy(id = newId + "-" + bullet.id.removePrefix(oldPrefix)) else bullet
            },
            source = FactSource.IMPORTED,
            isConfirmed = false,
        )
    }

    private fun limitEntries(entries: List<ProfileEntry>): List<ProfileEntry> {
        var bulletsLeft = ProfileLimits.MAX_BULLETS_IN_TOTAL
        return entries.take(ProfileLimits.MAX_ENTRIES).map { entry ->
            val keptBullets = entry.bullets.take(minOf(ProfileLimits.MAX_BULLETS_PER_ENTRY, bulletsLeft))
            bulletsLeft -= keptBullets.size
            entry.copy(bullets = keptBullets)
        }
    }

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

package com.tailormyresume.core.model

import kotlin.time.Instant

data class CandidateProfile(
    val fullName: String,
    val email: String,
    val phone: String,
    val headline: String,
    val skills: List<String>,
    val entries: List<ProfileEntry>,
    val userStatedSkills: List<String> = emptyList(),
    val city: String = "",
    val linkedinUrl: String = "",
    val portfolioUrl: String = "",
    val summary: String = "",
    val sourceFileName: String? = null,
    val reviewedAt: Instant? = null,
)

object ProfileLimits {
    const val MAX_ENTRIES = 40
    const val MAX_BULLETS_PER_ENTRY = 15
    const val MAX_BULLETS_IN_TOTAL = 200
    const val MAX_BULLET_LENGTH = 400
    const val MAX_SKILLS = 100
    const val MAX_SKILL_LENGTH = 60
    const val MAX_TEXT_LENGTH = 200
    const val SKILL_ID_PREFIX = "skill:"
}

fun CandidateProfile.confirmedWithinLimits(): CandidateProfile {
    var bulletsLeft = ProfileLimits.MAX_BULLETS_IN_TOTAL
    val limited = entries.filter { it.isConfirmed }.take(ProfileLimits.MAX_ENTRIES).map { entry ->
        val bullets = entry.bullets
            .filter { it.text.isNotBlank() && !it.isTooLong }
            .take(minOf(ProfileLimits.MAX_BULLETS_PER_ENTRY, bulletsLeft))
        bulletsLeft -= bullets.size
        entry.copy(
            title = entry.title.take(ProfileLimits.MAX_TEXT_LENGTH),
            organization = entry.organization.take(ProfileLimits.MAX_TEXT_LENGTH),
            startDate = entry.startDate.take(ProfileLimits.MAX_TEXT_LENGTH),
            endDate = entry.endDate.take(ProfileLimits.MAX_TEXT_LENGTH),
            bullets = bullets,
        )
    }
    val limitedSkills = skills.filter { it.isNotBlank() && it.length <= ProfileLimits.MAX_SKILL_LENGTH }
        .take(ProfileLimits.MAX_SKILLS)
    return copy(skills = limitedSkills, entries = limited)
}

fun CandidateProfile.sendableFacts(): CandidateProfile =
    copy(entries = entries.filter { it.source != FactSource.USER_ANSWER }).confirmedWithinLimits()

fun CandidateProfile.isSkillUserStated(skill: String): Boolean =
    userStatedSkills.any { it.equals(skill, ignoreCase = true) }

fun CandidateProfile.evidenceIds(): Set<String> =
    entries.flatMap { entry -> listOf(entry.id) + entry.bullets.map { it.id } }.toSet() +
        skills.map { ProfileLimits.SKILL_ID_PREFIX + it }

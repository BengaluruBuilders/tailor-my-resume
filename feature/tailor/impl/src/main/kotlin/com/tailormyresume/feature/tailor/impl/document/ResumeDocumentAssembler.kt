package com.tailormyresume.feature.tailor.impl.document

import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.continues
import javax.inject.Inject

internal class ResumeDocumentAssembler @Inject constructor(
    private val headings: ResumeHeadings,
) {

    fun assemble(profile: CandidateProfile, resume: TailoredResume): ResumeDocument {
        val reviewed = reviewedEntries(profile, resume)
        val recordedIds = resume.entryIds?.toSet()
        return ResumeDocument(
            name = profile.fullName.trim(),
            contactLine = listOf(profile.email, profile.phone).cleaned().joinToString(CONTACT_SEPARATOR),
            headline = profile.headline.trim(),
            skills = acceptedSkills(profile, resume).cleaned().distinctBy { it.lowercase() },
            sections = EntryCategory.entries.mapNotNull { category ->
                reviewed
                    .filter { it.category == category }
                    .filter { entry -> entry.bullets.isNotEmpty() || recordedIds == null || entry.id in recordedIds }
                    .mergingContinuations(resume)
                    .takeIf { it.isNotEmpty() }
                    ?.let { ResumeSection(category, headings.forCategory(category), it) }
            },
            skillsHeading = headings.skills,
            summary = resume.summary?.takeIf { it.decision == BulletDecision.ACCEPTED }?.text?.trim().orEmpty(),
        )
    }

    private fun acceptedSkills(profile: CandidateProfile, resume: TailoredResume): List<String> =
        resume.skills?.takeIf { it.decision == BulletDecision.ACCEPTED }?.skills ?: profile.skills

    private fun List<ProfileEntry>.mergingContinuations(resume: TailoredResume): List<ResumeEntry> {
        val merged = mutableListOf<Pair<ProfileEntry, ResumeEntry>>()
        forEach { entry ->
            val resumeEntry = entry.toResumeEntry(resume)
            val head = merged.lastOrNull()
            if (head != null && entry.continues(head.first)) {
                merged[merged.lastIndex] = head.first to head.second.copy(bullets = head.second.bullets + resumeEntry.bullets)
            } else {
                merged += entry to resumeEntry
            }
        }
        return merged.map { it.second }
    }

    private fun ProfileEntry.toResumeEntry(resume: TailoredResume): ResumeEntry = ResumeEntry(
        title = title.trim(),
        organization = organization.trim(),
        dateRange = dateRange(startDate, endDate),
        bullets = BulletReconciler.reconcile(this, resume).cleaned(),
    )

    private fun dateRange(start: String, end: String): String =
        listOf(start, end).cleaned().joinToString(DATE_SEPARATOR)

    private fun List<String>.cleaned(): List<String> = map { it.trim() }.filter { it.isNotEmpty() }

    private companion object {
        const val CONTACT_SEPARATOR = " | "
        const val DATE_SEPARATOR = " - "
    }
}

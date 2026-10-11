package com.tailormyresume.feature.tailor.impl

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Clock

internal class HandEditSummaryAndSkillsUseCase @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val clock: Clock,
) {
    suspend fun summary(applicationId: String, text: String, profileSummary: String): Boolean {
        val edited = text.trim()
        if (edited.isEmpty()) return false
        return update(applicationId) { resume ->
            val original = resume.summary?.original ?: profileSummary.trim()
            resume.copy(summary = TailoredText(text = edited, original = original, decision = BulletDecision.ACCEPTED))
        }
    }

    suspend fun skills(applicationId: String, text: String, profileSkills: List<String>): Boolean {
        val edited = parseSkillsLine(text)
        if (edited.isEmpty()) return false
        return update(applicationId) { resume ->
            val original = resume.skills?.original ?: profileSkills
            resume.copy(skills = TailoredSkills(skills = edited, original = original, decision = BulletDecision.ACCEPTED))
        }
    }

    private suspend fun update(applicationId: String, change: (TailoredResume) -> TailoredResume): Boolean {
        val application = applicationRepository.observeApplication(applicationId).first() ?: return false
        val resume = application.tailoredResume ?: return false
        applicationRepository.upsertApplication(
            application.copy(tailoredResume = change(resume), updatedAt = clock.now()),
        )
        return true
    }
}

internal fun parseSkillsLine(text: String): List<String> =
    text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

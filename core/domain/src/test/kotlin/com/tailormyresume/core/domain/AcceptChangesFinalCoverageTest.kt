package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.entry
import com.tailormyresume.core.domain.offline.profileOf
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementMatch
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AcceptChangesFinalCoverageTest {
    private val repository = FakeApplicationRepository()
    private val profile = profileOf(
        emptyList(),
        entry("exp-1", EntryCategory.EXPERIENCE, "Analyst", "Built dashboards in SQL for the finance team"),
    )

    private fun match(id: String, keyword: String, status: MatchStatus, evidenceIds: List<String>) = RequirementMatch(
        requirement = JobRequirement(id, keyword, RequirementType.SKILL, RequirementPriority.MUST_HAVE, listOf(keyword)),
        status = status,
        evidenceIds = evidenceIds,
    )

    @Test
    fun finalExcludesAKeywordTheUserUndid() = runTest {
        val addsPython = TailoredBullet(
            id = "b1",
            entryId = "exp-1",
            originalText = "Built dashboards in SQL for the finance team",
            proposedText = "Built dashboards in SQL and Python for the finance team",
            sourceIds = listOf("exp-1-b1"),
            editTypes = listOf(EditType.REWORD),
            keywordsUsed = emptyList(),
            violations = emptyList(),
            decision = BulletDecision.REJECTED,
        )
        val at = Instant.fromEpochSeconds(1_000)
        repository.upsertApplication(
            JobApplication(
                id = "app-1",
                job = JobDescription("t", "c", "raw", emptyList()),
                status = ApplicationStatus.SAVED,
                gapAnalysis = GapAnalysis(
                    listOf(
                        match("r1", "sql", MatchStatus.MET, listOf("exp-1-b1")),
                        match("r2", "python", MatchStatus.GAP, emptyList()),
                    ),
                    KeywordCoverage(0, 0),
                ),
                tailoredResume = TailoredResume(listOf(addsPython), entryIds = listOf("exp-1")),
                keywordCoverage = ApplicationKeywordCoverage(50, 100, 100),
                createdAt = at,
                updatedAt = at,
            ),
        )

        AcceptChangesUseCase(repository, FakeProfileRepository(profile), FixedClock())("app-1")

        assertThat(checkNotNull(repository.current("app-1")).keywordCoverage?.final).isEqualTo(50)
    }
}

package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class NoChangesTest {
    private val repository = FakeApplicationRepository()
    private val created = Instant.fromEpochSeconds(1_000)

    private val unchangedResume = TailoredResume(
        listOf(
            TailoredBullet(
                id = "b1",
                entryId = "e1",
                originalText = "Built reports",
                proposedText = "Built reports",
                sourceIds = listOf("s1"),
                editTypes = emptyList(),
                keywordsUsed = emptyList(),
                violations = emptyList(),
                decision = BulletDecision.PENDING,
            ),
        ),
    )

    private suspend fun saved(): JobApplication {
        repository.upsertApplication(
            JobApplication(
                id = "app-1",
                job = JobDescription("t", "c", "raw", emptyList()),
                status = ApplicationStatus.SAVED,
                gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(0, 0)),
                tailoredResume = unchangedResume,
                createdAt = created,
                updatedAt = created,
            ),
        )
        return checkNotNull(repository.current("app-1"))
    }

    @Test
    fun aRunWithNoChangesHasNothingToAcceptSoExportIsAllowed() = runTest {
        assertThat(ExportReadiness.check(saved())).isEqualTo(ExportCheck.ALLOWED)
    }
}

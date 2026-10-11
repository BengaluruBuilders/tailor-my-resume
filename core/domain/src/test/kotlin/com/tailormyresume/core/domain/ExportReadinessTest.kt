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
import com.tailormyresume.core.model.TailoredText
import org.junit.Test
import kotlin.time.Instant

class ExportReadinessTest {
    private fun bullet(decision: BulletDecision) = TailoredBullet(
        id = "b1",
        entryId = "e1",
        originalText = "original",
        proposedText = "proposed",
        sourceIds = listOf("s1"),
        editTypes = emptyList(),
        keywordsUsed = emptyList(),
        violations = emptyList(),
        decision = decision,
    )

    private fun application(resume: TailoredResume?, acceptedAt: Instant?) = JobApplication(
        id = "app-1",
        job = JobDescription("t", "c", "raw", emptyList()),
        status = ApplicationStatus.SAVED,
        gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(0, 0)),
        tailoredResume = resume,
        createdAt = Instant.fromEpochSeconds(1),
        updatedAt = Instant.fromEpochSeconds(1),
        changesAcceptedAt = acceptedAt,
    )

    private val accepted = Instant.fromEpochSeconds(2)

    @Test
    fun refusedWhenNotAccepted() {
        val decided = TailoredResume(listOf(bullet(BulletDecision.ACCEPTED)))

        assertThat(ExportReadiness.check(application(decided, acceptedAt = null))).isEqualTo(ExportCheck.NOT_ACCEPTED)
        assertThat(ExportReadiness.check(application(null, acceptedAt = accepted))).isEqualTo(ExportCheck.NOT_ACCEPTED)
    }

    @Test
    fun refusedWhenPendingAfterRetailor() {
        val pendingBullet = TailoredResume(listOf(bullet(BulletDecision.PENDING)))
        val pendingSummary = TailoredResume(emptyList(), summary = TailoredText("a", "b"))

        assertThat(ExportReadiness.check(application(pendingBullet, accepted))).isEqualTo(ExportCheck.PENDING_CHANGES)
        assertThat(ExportReadiness.check(application(pendingSummary, accepted))).isEqualTo(ExportCheck.PENDING_CHANGES)
    }

    @Test
    fun allowedWhenAllDecidedAndAccepted() {
        val decided = TailoredResume(
            listOf(bullet(BulletDecision.ACCEPTED), bullet(BulletDecision.REJECTED)),
            summary = TailoredText("a", "b", decision = BulletDecision.REJECTED),
        )

        assertThat(ExportReadiness.check(application(decided, accepted))).isEqualTo(ExportCheck.ALLOWED)
    }
}

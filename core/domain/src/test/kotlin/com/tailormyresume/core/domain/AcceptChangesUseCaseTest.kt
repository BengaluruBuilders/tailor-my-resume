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
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class AcceptChangesUseCaseTest {
    private val repository = FakeApplicationRepository()
    private val clock = FixedClock(Instant.fromEpochSeconds(2_000_000_000))
    private val useCase = AcceptChangesUseCase(repository, clock)
    private val created = Instant.fromEpochSeconds(1_000)

    private fun bullet(id: String, decision: BulletDecision) = TailoredBullet(
        id = id,
        entryId = "e1",
        originalText = "original $id",
        proposedText = "proposed $id",
        sourceIds = listOf("s-$id"),
        editTypes = emptyList(),
        keywordsUsed = emptyList(),
        violations = emptyList(),
        decision = decision,
    )

    private val mixedResume = TailoredResume(
        bullets = listOf(bullet("b1", BulletDecision.PENDING), bullet("b2", BulletDecision.REJECTED)),
        summary = TailoredText("new", "old"),
        skills = TailoredSkills(listOf("SQL"), listOf("SQL", "Excel")),
    )

    private suspend fun saved(resume: TailoredResume?, acceptedAt: Instant? = null): JobApplication {
        repository.upsertApplication(
            JobApplication(
                id = "app-1",
                job = JobDescription("t", "c", "raw", emptyList()),
                status = ApplicationStatus.SAVED,
                gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(0, 0)),
                tailoredResume = resume,
                createdAt = created,
                updatedAt = created,
                changesAcceptedAt = acceptedAt,
            ),
        )
        return checkNotNull(repository.current("app-1"))
    }

    @Test
    fun pendingBecomeAcceptedRejectedStays() = runTest {
        saved(mixedResume)

        useCase("app-1")

        val resume = checkNotNull(checkNotNull(repository.current("app-1")).tailoredResume)
        assertThat(resume.bullets.map { it.decision }).containsExactly(BulletDecision.ACCEPTED, BulletDecision.REJECTED).inOrder()
        assertThat(checkNotNull(resume.summary).decision).isEqualTo(BulletDecision.ACCEPTED)
        assertThat(checkNotNull(resume.skills).decision).isEqualTo(BulletDecision.ACCEPTED)
    }

    @Test
    fun setsChangesAcceptedAtFromClock() = runTest {
        saved(mixedResume)

        useCase("app-1")

        assertThat(checkNotNull(repository.current("app-1")).changesAcceptedAt).isEqualTo(clock.instant)
    }

    @Test
    fun secondRunChangesNothingElse() = runTest {
        saved(mixedResume)
        useCase("app-1")
        val afterFirst = checkNotNull(repository.current("app-1"))
        val writes = repository.upsertCount
        clock.instant = Instant.fromEpochSeconds(2_000_000_500)

        useCase("app-1")

        assertThat(checkNotNull(repository.current("app-1"))).isEqualTo(afterFirst)
        assertThat(repository.upsertCount).isEqualTo(writes)
    }

    @Test
    fun acceptedAtStaysTheFirstTimeWhenALaterTailoringAddsPendingChanges() = runTest {
        val first = Instant.fromEpochSeconds(1_500)
        saved(mixedResume, acceptedAt = first)

        useCase("app-1")

        val application = checkNotNull(repository.current("app-1"))
        assertThat(application.changesAcceptedAt).isEqualTo(first)
        assertThat(checkNotNull(application.tailoredResume).decisions).doesNotContain(BulletDecision.PENDING)
    }

    @Test
    fun applicationWithoutChangesIsStillMarkedAccepted() = runTest {
        saved(TailoredResume(emptyList()))

        useCase("app-1")

        assertThat(checkNotNull(repository.current("app-1")).changesAcceptedAt).isEqualTo(clock.instant)
    }

    @Test
    fun unknownApplicationOrMissingResumeWritesNothing() = runTest {
        useCase("missing")
        saved(null)
        val writes = repository.upsertCount

        useCase("app-1")

        assertThat(repository.upsertCount).isEqualTo(writes)
    }
}

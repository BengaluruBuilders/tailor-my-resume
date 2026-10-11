package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.ApplicationRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class UpdateBulletDecisionUseCaseTest {
    private val repository = FakeApplicationRepository()
    private val clock = FixedClock(Instant.fromEpochSeconds(2_000_000_000))
    private val useCase = UpdateBulletDecisionUseCase(repository, clock)
    private val created = Instant.fromEpochSeconds(1_000)

    private fun bullet(id: String) = TailoredBullet(
        id = id,
        entryId = "e1",
        originalText = "original $id",
        proposedText = "proposed $id",
        sourceIds = listOf("s-$id"),
        editTypes = emptyList(),
        keywordsUsed = emptyList(),
        violations = emptyList(),
        decision = BulletDecision.PENDING,
    )

    private fun application(resume: TailoredResume?) = JobApplication(
        id = "app-1",
        job = JobDescription("t", "c", "raw", emptyList()),
        status = ApplicationStatus.SAVED,
        gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(0, 0)),
        tailoredResume = resume,
        createdAt = created,
        updatedAt = created,
    )

    @Test
    fun updatesOnlyTheTargetBulletAndTimestamp() = runTest {
        repository.upsertApplication(application(TailoredResume(listOf(bullet("b1"), bullet("b2")))))

        useCase("app-1", "b2", BulletDecision.ACCEPTED)

        val saved = checkNotNull(repository.current("app-1"))
        assertThat(checkNotNull(saved.tailoredResume).bullets.map { it.decision })
            .containsExactly(BulletDecision.PENDING, BulletDecision.ACCEPTED).inOrder()
        assertThat(saved.updatedAt).isEqualTo(Instant.fromEpochSeconds(2_000_000_000))
        assertThat(saved.createdAt).isEqualTo(created)
    }

    @Test
    fun keepsTheLegacyNotesAndStatusOfTheApplication() = runTest {
        repository.upsertApplication(
            application(TailoredResume(listOf(bullet("b1")))).copy(legacyNotes = "no reply yet", legacyStatus = "NO_RESPONSE"),
        )

        useCase("app-1", "b1", BulletDecision.ACCEPTED)

        val saved = checkNotNull(repository.current("app-1"))
        assertThat(saved.legacyNotes).isEqualTo("no reply yet")
        assertThat(saved.legacyStatus).isEqualTo("NO_RESPONSE")
    }

    @Test
    fun rejectedDecisionIsStoredAndCanBeChangedBack() = runTest {
        repository.upsertApplication(application(TailoredResume(listOf(bullet("b1")))))

        useCase("app-1", "b1", BulletDecision.REJECTED)
        useCase("app-1", "b1", BulletDecision.PENDING)

        val saved = checkNotNull(repository.current("app-1"))
        assertThat(checkNotNull(saved.tailoredResume).bullets.single().decision).isEqualTo(BulletDecision.PENDING)
    }

    @Test
    fun unknownApplicationDoesNothing() = runTest {
        useCase("missing", "b1", BulletDecision.ACCEPTED)

        assertThat(repository.upsertCount).isEqualTo(0)
    }

    @Test
    fun applicationWithoutTailoredResumeIsUnchanged() = runTest {
        repository.upsertApplication(application(null))
        val before = repository.upsertCount

        useCase("app-1", "b1", BulletDecision.ACCEPTED)

        assertThat(repository.upsertCount).isEqualTo(before)
    }

    @Test
    fun unknownBulletLeavesAllDecisionsUntouched() = runTest {
        repository.upsertApplication(application(TailoredResume(listOf(bullet("b1")))))

        useCase("app-1", "nope", BulletDecision.ACCEPTED)

        val saved = checkNotNull(repository.current("app-1"))
        assertThat(checkNotNull(saved.tailoredResume).bullets.single().decision).isEqualTo(BulletDecision.PENDING)
    }

    @Test
    fun undoSetsRejectedAndOriginalTextIsUsed() = runTest {
        repository.upsertApplication(application(TailoredResume(listOf(bullet("b1")))))
        useCase("app-1", "b1", BulletDecision.ACCEPTED)

        useCase("app-1", "b1", BulletDecision.REJECTED)

        val undone = checkNotNull(checkNotNull(repository.current("app-1")).tailoredResume).bullets.single()
        assertThat(undone.decision).isEqualTo(BulletDecision.REJECTED)
        assertThat(undone.originalText).isEqualTo("original b1")
        assertThat(undone.proposedText).isEqualTo("proposed b1")
    }

    @Test
    fun summaryAndSkillsCanBeUndoneAndAreNotAcceptedByAcceptChanges() = runTest {
        val resume = TailoredResume(
            listOf(bullet("b1")),
            summary = TailoredText("new", "old"),
            skills = TailoredSkills(listOf("SQL"), listOf("SQL", "Excel")),
        )
        repository.upsertApplication(application(resume))

        useCase("app-1", UpdateBulletDecisionUseCase.SUMMARY_CHANGE_ID, BulletDecision.REJECTED)
        useCase("app-1", UpdateBulletDecisionUseCase.SKILLS_CHANGE_ID, BulletDecision.REJECTED)
        AcceptChangesUseCase(repository, clock)("app-1")

        val saved = checkNotNull(checkNotNull(repository.current("app-1")).tailoredResume)
        assertThat(saved.summary?.decision).isEqualTo(BulletDecision.REJECTED)
        assertThat(saved.skills?.decision).isEqualTo(BulletDecision.REJECTED)
        assertThat(saved.bullets.single().decision).isEqualTo(BulletDecision.ACCEPTED)
        assertThat(ExportReadiness.check(checkNotNull(repository.current("app-1")))).isEqualTo(ExportCheck.ALLOWED)
    }

    @Test
    fun undoThenAcceptInQuickSuccessionNeverSavesTheUndoneChangeAsAccepted() = runTest {
        repository.upsertApplication(application(TailoredResume(listOf(bullet("b1"), bullet("b2")))))
        val slowReads = object : ApplicationRepository by repository {
            override fun observeApplication(id: String): Flow<JobApplication?> =
                repository.observeApplication(id).onEach { delay(10) }
        }
        val undo = UpdateBulletDecisionUseCase(slowReads, clock)
        val accept = AcceptChangesUseCase(slowReads, clock)

        launch { undo("app-1", "b1", BulletDecision.REJECTED) }
        launch { accept("app-1") }
        testScheduler.advanceUntilIdle()

        val saved = checkNotNull(checkNotNull(repository.current("app-1")).tailoredResume)
        assertThat(saved.bullets.map { it.decision })
            .containsExactly(BulletDecision.REJECTED, BulletDecision.ACCEPTED).inOrder()
    }
}

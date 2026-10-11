package com.tailormyresume.feature.tailor.impl

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class HandEditBulletLegacyFieldsTest {

    @Test
    fun aHandEditKeepsTheLegacyNotesAndStatus() = runTest {
        val repository = TestApplicationRepository()
        val created = Instant.fromEpochSeconds(1_000)
        repository.upsertApplication(
            JobApplication(
                id = "app-1",
                job = JobDescription("t", "c", "raw", emptyList()),
                status = ApplicationStatus.SAVED,
                gapAnalysis = GapAnalysis(emptyList(), KeywordCoverage(0, 0)),
                tailoredResume = TailoredResume(listOf(testBullet("b1"))),
                createdAt = created,
                updatedAt = created,
                legacyNotes = "no reply yet",
                legacyStatus = "NO_RESPONSE",
            ),
        )

        val edited = HandEditBulletUseCase(repository, TestTailoringReviewStateRepository(), TestClock())("app-1", "b1", "Built a tool")

        val saved = checkNotNull(repository.observeApplication("app-1").first())
        assertThat(edited).isTrue()
        assertThat(saved.legacyNotes).isEqualTo("no reply yet")
        assertThat(saved.legacyStatus).isEqualTo("NO_RESPONSE")
    }
}

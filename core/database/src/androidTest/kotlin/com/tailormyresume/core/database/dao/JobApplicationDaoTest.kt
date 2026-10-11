package com.tailormyresume.core.database.dao

import com.tailormyresume.core.database.json.GapAnalysisDto
import com.tailormyresume.core.database.json.GuardrailViolationDto
import com.tailormyresume.core.database.json.JobRequirementDto
import com.tailormyresume.core.database.json.KeywordCoverageDto
import com.tailormyresume.core.database.json.RequirementMatchDto
import com.tailormyresume.core.database.json.TailoredBulletDto
import com.tailormyresume.core.database.json.TailoredResumeDto
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

internal class JobApplicationDaoTest : DatabaseTest() {

    @Test
    fun upsertApplication_roundTripsEveryColumn() = runTest {
        val application = testApplication("a1", updatedAtMillis = 10)

        jobApplicationDao.upsertApplication(application)

        assertEquals(application, jobApplicationDao.observeApplication("a1").first())
    }

    @Test
    fun nullJsonColumnsRoundTripAsNull() = runTest {
        val application = testApplication("a1", updatedAtMillis = 10)
            .copy(gapAnalysis = null, tailoredResume = null)

        jobApplicationDao.upsertApplication(application)

        val saved = jobApplicationDao.observeApplication("a1").first()
        assertNull(saved?.gapAnalysis)
        assertNull(saved?.tailoredResume)
    }

    @Test
    fun observeApplications_ordersByMostRecentUpdate() = runTest {
        jobApplicationDao.upsertApplication(testApplication("old", updatedAtMillis = 1))
        jobApplicationDao.upsertApplication(testApplication("new", updatedAtMillis = 2))

        val saved = jobApplicationDao.observeApplications().first()

        assertEquals(listOf("new", "old"), saved.map { it.id })
    }

    @Test
    fun observeApplications_breaksTiesById() = runTest {
        jobApplicationDao.upsertApplication(testApplication("b", updatedAtMillis = 5))
        jobApplicationDao.upsertApplication(testApplication("c", updatedAtMillis = 5))
        jobApplicationDao.upsertApplication(testApplication("a", updatedAtMillis = 5))

        val saved = jobApplicationDao.observeApplications().first()

        assertEquals(listOf("a", "b", "c"), saved.map { it.id })
    }

    @Test
    fun upsertApplication_replacesExistingRow() = runTest {
        jobApplicationDao.upsertApplication(testApplication("a1", updatedAtMillis = 1))
        jobApplicationDao.upsertApplication(
            testApplication("a1", updatedAtMillis = 1).copy(company = "Initech"),
        )

        val saved = jobApplicationDao.observeApplications().first()

        assertEquals(listOf("Initech"), saved.map { it.company })
    }

    @Test
    fun updateStatus_changesStatusAndUpdatedAt() = runTest {
        jobApplicationDao.upsertApplication(testApplication("a1", updatedAtMillis = 1))

        jobApplicationDao.updateStatus("a1", ApplicationStatus.OFFER, Instant.fromEpochMilliseconds(99))

        val saved = jobApplicationDao.observeApplication("a1").first()
        assertEquals(ApplicationStatus.OFFER, saved?.status)
        assertEquals(Instant.fromEpochMilliseconds(99), saved?.updatedAt)
    }

    @Test
    fun deleteApplication_removesOnlyThatRow() = runTest {
        jobApplicationDao.upsertApplication(testApplication("a1", updatedAtMillis = 1))
        jobApplicationDao.upsertApplication(testApplication("a2", updatedAtMillis = 2))

        jobApplicationDao.deleteApplication("a1")

        assertEquals(listOf("a2"), jobApplicationDao.observeApplications().first().map { it.id })
    }

    private fun testApplication(id: String, updatedAtMillis: Long): JobApplicationEntity {
        val requirement = JobRequirementDto(
            id = "r-$id",
            text = "Kotlin",
            type = RequirementType.SKILL,
            priority = RequirementPriority.MUST_HAVE,
            keywords = listOf("kotlin"),
        )
        return JobApplicationEntity(
            id = id,
            jobTitle = "Android Engineer",
            company = "Globex",
            rawText = "raw job text",
            requirements = listOf(requirement),
            status = ApplicationStatus.SAVED,
            notes = "",
            gapAnalysis = GapAnalysisDto(
                matches = listOf(RequirementMatchDto(requirement, MatchStatus.MET, listOf("b-1"))),
                keywordCoverage = KeywordCoverageDto(covered = 1, total = 1),
            ),
            tailoredResume = TailoredResumeDto(
                bullets = listOf(
                    TailoredBulletDto(
                        id = "t-$id",
                        entryId = "e-1",
                        originalText = "before",
                        proposedText = "after",
                        sourceIds = listOf("b-1"),
                        editTypes = listOf(EditType.REWORD),
                        keywordsUsed = listOf("kotlin"),
                        violations = listOf(GuardrailViolationDto.UnsupportedNumber("40%")),
                        decision = BulletDecision.PENDING,
                    ),
                ),
            ),
            createdAt = Instant.fromEpochMilliseconds(1),
            updatedAt = Instant.fromEpochMilliseconds(updatedAtMillis),
        )
    }
}

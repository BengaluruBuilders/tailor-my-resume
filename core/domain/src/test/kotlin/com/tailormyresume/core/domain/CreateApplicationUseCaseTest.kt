package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.OfflineFabricationGuard
import com.tailormyresume.core.domain.offline.OfflineGapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.domain.offline.resourceText
import com.tailormyresume.core.domain.offline.sampleProfile
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.TailoredResume
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

class CreateApplicationUseCaseTest {
    private val repository = FakeApplicationRepository()
    private val clock = FixedClock(Instant.fromEpochSeconds(1_800_000_000))
    private val useCase = CreateApplicationUseCase(
        applicationRepository = repository,
        tailorResume = TailorResumeUseCase(OfflineResumeTailor(), OfflineFabricationGuard()),
        clock = clock,
        idGenerator = SequentialIdGenerator("app"),
    )
    private val analysis = runBlocking {
        AnalyzeJobUseCase(OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), OfflineGapMatcher()))(
            sampleProfile,
            resourceText("jd_android.txt"),
        )
    }

    @Test
    fun savesApplicationWithSavedStatusAndReturnsItsId() = runTest {
        val id = useCase(sampleProfile, analysis)

        assertThat(id).isEqualTo("app-1")
        val saved = checkNotNull(repository.current(id))
        assertThat(saved.status).isEqualTo(ApplicationStatus.SAVED)
        assertThat(saved.job).isEqualTo(analysis.job)
        assertThat(saved.gapAnalysis).isEqualTo(analysis.gap)
    }

    @Test
    fun storesTailoredResumeAndTimestamps() = runTest {
        val saved = checkNotNull(repository.current(useCase(sampleProfile, analysis)))

        assertThat(checkNotNull(saved.tailoredResume).bullets).isNotEmpty()
        assertThat(saved.createdAt).isEqualTo(Instant.fromEpochSeconds(1_800_000_000))
        assertThat(saved.updatedAt).isEqualTo(saved.createdAt)
    }

    @Test
    fun whenTheTailorFails_savesNothingAndRethrowsTheTypedFailure() = runTest {
        val failing = object : ResumeTailor {
            override suspend fun tailor(profile: CandidateProfile, job: JobDescription, gap: GapAnalysis, applicationId: String, answer: QuickAnswer?, runId: String): TailoredResume =
                throw AiException(AiFailure.NoCredit)
        }
        val failingUseCase = CreateApplicationUseCase(
            applicationRepository = repository,
            tailorResume = TailorResumeUseCase(failing, OfflineFabricationGuard()),
            clock = clock,
            idGenerator = SequentialIdGenerator("app"),
        )

        val failure = runCatching { failingUseCase(sampleProfile, analysis) }.exceptionOrNull()

        assertThat(failure?.isAiFailure(AiFailure.NoCredit)).isTrue()
        assertThat(repository.upsertCount).isEqualTo(0)
    }

    @Test
    fun eachCallCreatesADistinctApplication() = runTest {
        val first = useCase(sampleProfile, analysis)
        val second = useCase(sampleProfile, analysis)

        assertThat(first).isNotEqualTo(second)
        assertThat(repository.upsertCount).isEqualTo(2)
    }

    @Test
    fun aCallWithTheSameApplicationIdReplacesInsteadOfCreatingASecondApplication() = runTest {
        val first = useCase(sampleProfile, analysis, applicationId = "draft-1")
        val second = useCase(sampleProfile, analysis, applicationId = "draft-1")

        assertThat(first).isEqualTo("draft-1")
        assertThat(second).isEqualTo(first)
    }

    @Test
    fun keptCompanyAndRoleOverrideWhatTheAnalyzerFound() = runTest {
        val id = useCase(sampleProfile, analysis, KeptJobDescription("text", company = "Kestrel Labs", role = "Android Developer"))

        val job = checkNotNull(repository.current(id)).job
        assertThat(job.company).isEqualTo("Kestrel Labs")
        assertThat(job.title).isEqualTo("Android Developer")
    }

    @Test
    fun blankKeptValuesKeepWhatTheAnalyzerFound() = runTest {
        val id = useCase(sampleProfile, analysis, KeptJobDescription("text", company = "  ", role = ""))

        assertThat(checkNotNull(repository.current(id)).job).isEqualTo(analysis.job)
    }
}

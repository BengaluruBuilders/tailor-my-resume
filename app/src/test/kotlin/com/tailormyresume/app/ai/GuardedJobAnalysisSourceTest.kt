package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class GuardedJobAnalysisSourceTest {
    private val backend = FakeBackend()
    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(emptyList(), KeywordCoverage(0, 0))
    }
    private val previewMode = DebugPreviewMode()
    private val guarded = GuardedJobAnalysisSource(
        remote = RemoteJobAnalysisSource(backend.api, matcher),
        offline = OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), matcher),
        previewMode = previewMode,
    )

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun whileAPreviewIsOpenTheBackendIsNeverCalled() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)
        previewMode.active = true

        val result = guarded.analyse(candidate, "Associate Analyst at Northwind\nSQL reports")

        assertThat(backend.server.requestCount).isEqualTo(0)
        assertThat(result.job.title).isNotEmpty()
    }

    @Test
    fun outsideAPreviewTheBackendAnswers() = runBlocking<Unit> {
        backend.reply(200, ANALYSIS_RESPONSE)

        guarded.analyse(candidate, "the raw job text")

        assertThat(backend.server.requestCount).isEqualTo(1)
    }
}

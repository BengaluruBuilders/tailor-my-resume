package com.tailormyresume.app.di

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.ai.DebugPreviewMode
import com.tailormyresume.app.ai.FakeBackend
import com.tailormyresume.app.ai.FixedIds
import com.tailormyresume.app.ai.GuardedJobAnalysisSource
import com.tailormyresume.app.ai.GuardedResumeTailor
import com.tailormyresume.app.ai.PendingTailoringIds
import com.tailormyresume.app.ai.RemoteJobAnalysisSource
import com.tailormyresume.app.ai.RemoteResumeTailor
import com.tailormyresume.app.ai.candidate
import com.tailormyresume.app.ai.job
import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.domain.offline.OfflineJobAnalysisSource
import com.tailormyresume.core.domain.offline.OfflineJobDescriptionAnalyzer
import com.tailormyresume.core.domain.offline.OfflineResumeTailor
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class AiBindingsTest {
    private val backend = FakeBackend()
    private val previewMode = DebugPreviewMode()
    private val matcher = object : GapMatcher {
        override fun match(profile: CandidateProfile, job: JobDescription) =
            GapAnalysis(emptyList(), KeywordCoverage(0, 0))
    }
    private val gap = matcher.match(candidate, job)

    private val analysisSource = GuardedJobAnalysisSource(
        RemoteJobAnalysisSource(backend.api, matcher),
        OfflineJobAnalysisSource(OfflineJobDescriptionAnalyzer(), matcher),
        previewMode,
    )
    private val tailor = GuardedResumeTailor(
        RemoteResumeTailor(backend.api, PendingTailoringIds(TestMockStateStore(), FixedIds)),
        OfflineResumeTailor(),
        previewMode,
    )

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun everyAiBindingResolvesToItsGuardedSource() {
        val parameterTypes = AiBindings::class.java.methods.associate { it.name to it.parameterTypes.single() }

        assertThat(parameterTypes["bindJobAnalysisSource"]).isEqualTo(GuardedJobAnalysisSource::class.java)
        assertThat(parameterTypes["bindResumeTailor"]).isEqualTo(GuardedResumeTailor::class.java)
    }

    @Test
    fun whileAPreviewIsOpenNoAiSourceCallsTheBackend() = runBlocking<Unit> {
        previewMode.active = true

        analysisSource.analyse(candidate, "Associate Analyst at Northwind\nSQL reports")
        tailor.tailor(candidate, job, gap, "app-1", null)

        assertThat(backend.server.requestCount).isEqualTo(0)
    }

    @Test
    fun outsideAPreviewEachAiSourceCallsTheBackend() = runBlocking<Unit> {
        backend.reply(500, """{"error":{"code":"AI_PROVIDER_ERROR","message":"x"}}""")
        backend.reply(500, """{"error":{"code":"AI_PROVIDER_ERROR","message":"x"}}""")

        runCatching { analysisSource.analyse(candidate, "text") }
        runCatching { tailor.tailor(candidate, job, gap, "app-1", null) }

        assertThat(backend.server.requestCount).isEqualTo(2)
    }
}

package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineRobustnessTest {
    private val odd = listOf(
        "",
        "   ",
        "\n\n\n",
        "•",
        "- ",
        ":",
        "Requirements:",
        "Requirements:\n",
        "Skills\nEducation\nProjects\nExperience",
        "• • • •\n- - -\n1. 2. 3.",
        "हिन्दी तेलुगु emoji 😀 Kotlin",
        "a".repeat(5_000),
        "Java, ".repeat(500),
        "C++ C# C R Go Rust\n".repeat(50),
        "Skills: ((((Python, SQL",
        "2021 - 2025 | 2019-21 | Jan 2020 to present",
        "Objective\nDeclaration\nPersonal Details\nHobbies",
    )

    @Test
    fun analyzerNeverThrowsOnOddInput() = runTest {
        val analyzer = OfflineJobDescriptionAnalyzer()
        odd.forEach { assertThat(analyzer.analyze(it).rawText).isEqualTo(it) }
    }

    @Test
    fun parserNeverThrowsOnOddInput() = runTest {
        val parser = OfflineResumeTextParser()
        odd.forEach { assertThat(parser.parse(it)).isNotNull() }
    }

    @Test
    fun matcherAndTailorNeverThrowOnOddJobs() = runTest {
        val analyzer = OfflineJobDescriptionAnalyzer()
        val matcher = OfflineGapMatcher()
        val tailor = OfflineResumeTailor()
        odd.forEach {
            val job = analyzer.analyze(it)
            val gap = matcher.match(sampleProfile, job)
            assertThat(gap.matches).hasSize(job.requirements.size)
            assertThat(tailor.tailor(sampleProfile, job, gap, "app-1", null, "run-1").bullets).isNotEmpty()
        }
    }

    @Test
    fun guardNeverThrowsOnOddText() = runTest {
        val guard = OfflineFabricationGuard()
        val sources = sampleProfile.entries.flatMap { it.bullets }
        odd.forEach { assertThat(guard.check(it, sources, sampleProfile)).isNotNull() }
    }

    @Test
    fun parsedResumeCanBeConfirmedAndUsedAsEvidence() = runTest {
        val parsed = OfflineResumeTextParser().parse(resourceText("resume_1.txt"))
        val confirmed = parsed.copy(entries = parsed.entries.map { it.copy(isConfirmed = true) })
        val job = OfflineJobDescriptionAnalyzer().analyze(resourceText("jd_android.txt"))
        val gap = OfflineGapMatcher().match(confirmed, job)
        assertThat(gap.keywordCoverage.covered).isAtLeast(8)
    }
}

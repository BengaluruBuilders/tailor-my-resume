package com.tailormyresume.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SimplifiedFlowModelTest {

    private val instant = Instant.parse("2026-10-10T09:00:00Z")

    @Test
    fun modelsExposeTheSimplifiedFlowFields() {
        val profile = CandidateProfile(
            fullName = "Priya",
            email = "p@example.com",
            phone = "1",
            headline = "h",
            skills = emptyList(),
            entries = emptyList(),
            city = "Pune",
            linkedinUrl = "https://linkedin.com/in/priya",
            portfolioUrl = "https://priya.dev",
            summary = "Analyst",
            sourceFileName = "resume.pdf",
            reviewedAt = instant,
        )
        assertEquals("Pune", profile.city)
        assertEquals("resume.pdf", profile.sourceFileName)
        assertEquals(instant, profile.reviewedAt)

        assertEquals(
            listOf("SAVED", "APPLIED", "INTERVIEW", "OFFER", "REJECTED"),
            ApplicationStatus.entries.map { it.name },
        )

        val application = JobApplication(
            id = "a",
            job = JobDescription("t", "c", "raw", emptyList()),
            status = ApplicationStatus.APPLIED,
            gapAnalysis = null,
            tailoredResume = null,
            createdAt = instant,
            updatedAt = instant,
            location = "Pune",
            appliedOn = instant,
            keywordCoverage = ApplicationKeywordCoverage(now = 61, upTo = 92, final = 92),
            exportFileName = "Priya_Northwind_Analyst.pdf",
            quickAnswer = QuickAnswer(requirementId = "r1", choice = "yes", detail = "Quarterly"),
            changesAcceptedAt = instant,
        )
        assertEquals(92, application.keywordCoverage?.upTo)
        assertEquals("Quarterly", application.quickAnswer?.detail)

        assertEquals(FactSource.USER_ANSWER, FactSource.valueOf("USER_ANSWER"))

        val entry = CreditLedgerEntry(
            kind = CreditLedgerKind.PURCHASE,
            amount = 5,
            applicationId = null,
            productId = "application_pack_5",
            createdAt = instant,
        )
        assertEquals(5, entry.amount)
        assertNull(entry.applicationId)

        val settings = ResumeSettings()
        assertEquals(PageSize.A4, settings.pageSize)
        assertEquals(FileNameFormat.NAME_COMPANY_ROLE, settings.fileNameFormat)
        assertEquals(false, settings.productUpdates)
        assertEquals(listOf("A4", "LETTER"), PageSize.entries.map { it.name })
        assertEquals(
            listOf("NAME_COMPANY_ROLE", "NAME_ROLE", "NAME_RESUME"),
            FileNameFormat.entries.map { it.name },
        )
    }
}

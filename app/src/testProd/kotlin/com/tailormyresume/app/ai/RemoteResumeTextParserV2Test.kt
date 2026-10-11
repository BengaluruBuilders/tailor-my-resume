package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.FactSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

class RemoteResumeTextParserV2Test {
    private val backend = FakeBackend()
    private val parser = RemoteResumeTextParser(backend.api, FactIdAllocator())

    @After
    fun tearDown() = backend.shutdown()

    @Test
    fun parseMapsSummaryLocationLinksAndCurrent() = runBlocking<Unit> {
        backend.reply(
            200,
            """{"generationId":"g","profile":{"fullName":"Priya","email":null,"phone":null,"headline":null,
"summary":"Data analyst with 2 years in retail.","location":"Pune",
"links":[{"kind":"OTHER","url":"https://other.example.com"},{"kind":"LINKEDIN","url":"https://www.linkedin.com/in/priya"},
{"kind":"PORTFOLIO","url":"https://priya.example.com"},{"kind":"LINKEDIN","url":"https://www.linkedin.com/in/second"}],
"skills":["SQL"],"entries":[
{"ref":"e1","category":"EXPERIENCE","title":"Data Operations Associate","organization":"Saffron Retail","startDate":"Jul 2024",
"endDate":null,"current":true,"bullets":[{"ref":"e1b1","text":"Cleaned weekly sales data."}]},
{"ref":"e2","category":"EXPERIENCE","title":"Intern","organization":"Acme","startDate":"Jan 2023",
"endDate":null,"current":false,"bullets":[{"ref":"e2b1","text":"Built a dashboard."}]}]},"droppedSensitive":["DATE_OF_BIRTH"]}""",
        )

        val profile = parser.parse("resume text")

        assertThat(profile.summary).isEqualTo("Data analyst with 2 years in retail.")
        assertThat(profile.city).isEqualTo("Pune")
        assertThat(profile.linkedinUrl).isEqualTo("https://www.linkedin.com/in/priya")
        assertThat(profile.portfolioUrl).isEqualTo("https://priya.example.com")
        assertThat(profile.entries.map { it.endDate }).containsExactly("Present", "").inOrder()
        assertThat(profile.entries.map { it.source }).containsExactly(FactSource.IMPORTED, FactSource.IMPORTED)
        assertThat(profile.entries.map { it.isConfirmed }).containsExactly(false, false)
        assertThat(profile.entries.map { it.id }.distinct()).hasSize(2)
    }

    @Test
    fun nullSummaryLocationAndMissingLinksMapToEmpty() = runBlocking<Unit> {
        backend.reply(
            200,
            """{"generationId":"g","profile":{"fullName":null,"email":null,"phone":null,"headline":null,
"summary":null,"location":null,"links":[{"kind":"SOMETHING_NEW","url":"https://x.example.com"}],"skills":[],"entries":[]},
"droppedSensitive":[]}""",
        )

        val profile = parser.parse("resume text")

        assertThat(profile.summary).isEmpty()
        assertThat(profile.city).isEmpty()
        assertThat(profile.linkedinUrl).isEmpty()
        assertThat(profile.portfolioUrl).isEmpty()
    }
}

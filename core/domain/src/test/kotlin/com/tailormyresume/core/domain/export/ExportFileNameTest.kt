package com.tailormyresume.core.domain.export

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobDescription
import org.junit.Test

class ExportFileNameTest {
    private fun profile(name: String) = CandidateProfile(
        fullName = name,
        email = "",
        phone = "",
        headline = "",
        skills = emptyList(),
        entries = emptyList(),
    )

    private fun job(company: String, title: String) =
        JobDescription(title = title, company = company, rawText = "", requirements = emptyList())

    private fun build(name: String, company: String, title: String, format: ExportFileNameFormat) =
        ExportFileName.build(profile(name), job(company, title), format)

    @Test
    fun nameCompanyRoleNameRoleNameResumeForPriyaDeshmukh() {
        val priya = "Priya Deshmukh"
        val company = "Northwind GCC"
        val role = "Associate Analyst"

        assertThat(build(priya, company, role, ExportFileNameFormat.NAME_COMPANY_ROLE))
            .isEqualTo("Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf")
        assertThat(build(priya, company, role, ExportFileNameFormat.NAME_ROLE))
            .isEqualTo("Priya-Deshmukh_Associate-Analyst.pdf")
        assertThat(build(priya, company, role, ExportFileNameFormat.NAME_RESUME))
            .isEqualTo("Priya-Deshmukh_Resume.pdf")
    }

    @Test
    fun unsafeCharactersAreRemoved() {
        assertThat(
            build("Priya   Deshmukh", "North/wind: GCC", "Analyst <Sr> | \"x\" * ?", ExportFileNameFormat.NAME_COMPANY_ROLE),
        ).isEqualTo("Priya-Deshmukh_North-wind-GCC_Analyst-Sr-x.pdf")
        assertThat(build("Priya", "..hidden\\path", "Analyst", ExportFileNameFormat.NAME_COMPANY_ROLE))
            .isEqualTo("Priya_hidden-path_Analyst.pdf")
        assertThat(build("A\u0000B\tC", "..", "Analyst \uD83D\uDE80", ExportFileNameFormat.NAME_COMPANY_ROLE))
            .isEqualTo("A-B-C_Analyst.pdf")
        val hostile = listOf("../../etc/passwd", "C:\\temp\\x", "a*b?c\"d<e>f|g", "...", "\u0007\u001B")
        hostile.forEach { name ->
            val result = build(name, name, name, ExportFileNameFormat.NAME_COMPANY_ROLE)
            assertThat(result).doesNotContainMatch("[/\\\\:*?\"<>|]")
            assertThat(result).doesNotContain("..")
        }
    }

    @Test
    fun fallbackBaseWhenAllPartsEmpty() {
        assertThat(build(" ", "", "  ", ExportFileNameFormat.NAME_COMPANY_ROLE)).isEqualTo("Resume.pdf")
        assertThat(build(" ", "", "  ", ExportFileNameFormat.NAME_ROLE)).isEqualTo("Resume.pdf")
        assertThat(build(" ", "", "  ", ExportFileNameFormat.NAME_RESUME)).isEqualTo("Resume.pdf")
    }

    @Test
    fun lengthIsCapped() {
        val long = build("a".repeat(150), "", "", ExportFileNameFormat.NAME_COMPANY_ROLE)
        assertThat(long).hasLength(104)
        assertThat(long).endsWith(".pdf")

        val trimmed = build("a".repeat(99) + " " + "b".repeat(50), "", "", ExportFileNameFormat.NAME_COMPANY_ROLE)
        assertThat(trimmed).isEqualTo("a".repeat(99) + ".pdf")
    }

    @Test
    fun endsWithPdf() {
        ExportFileNameFormat.entries.forEach { format ->
            assertThat(build("Priya Deshmukh", "Northwind GCC", "Analyst", format)).endsWith(".pdf")
        }
    }

    @Test
    fun combiningMarksSurviveInIndicAndLatinNames() {
        assertThat(build("प्रिया देशमुख", "", "", ExportFileNameFormat.NAME_RESUME))
            .isEqualTo("प्रिया-देशमुख_Resume.pdf")
        assertThat(build("Zoe\u0308 Núñez", "", "", ExportFileNameFormat.NAME_RESUME))
            .isEqualTo("Zoe\u0308-Núñez_Resume.pdf")
    }
}

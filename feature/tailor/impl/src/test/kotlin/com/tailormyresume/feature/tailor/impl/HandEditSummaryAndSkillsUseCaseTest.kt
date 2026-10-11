package com.tailormyresume.feature.tailor.impl

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.ExportCheck
import com.tailormyresume.core.domain.ExportReadiness
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.TailoredSkills
import com.tailormyresume.core.model.TailoredText
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HandEditSummaryAndSkillsUseCaseTest {
    private val applicationRepository = TestApplicationRepository()
    private val useCase = HandEditSummaryAndSkillsUseCase(applicationRepository, TestClock())
    private val bullet = acceptedBullet("b1", original = "Built a tool", proposed = "Developed a tool")

    private suspend fun stored() = checkNotNull(applicationRepository.observeApplication("app-1").first()?.tailoredResume)

    @Test
    fun summaryEditIsStoredAsTheUsersAcceptedTextKeepingTheOriginal() = runTest {
        val tailored = TailoredText(
            text = "Tailored",
            original = "Old summary",
            sourceIds = listOf("b1"),
            violations = listOf(GuardrailViolation.MissingSource),
            decision = BulletDecision.PENDING,
        )
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet), summary = tailored)))

        val saved = useCase.summary("app-1", "  My own words.  ", profileSummary = "Profile summary")

        assertThat(saved).isTrue()
        assertThat(stored().summary).isEqualTo(
            TailoredText(text = "My own words.", original = "Old summary", decision = BulletDecision.ACCEPTED),
        )
    }

    @Test
    fun summaryEditWithoutATailoredSummaryOriginatesFromTheProfileSummary() = runTest {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet))))

        useCase.summary("app-1", "Brand new", profileSummary = "Profile summary")

        assertThat(stored().summary)
            .isEqualTo(TailoredText(text = "Brand new", original = "Profile summary", decision = BulletDecision.ACCEPTED))
    }

    @Test
    fun blankSummaryIsIgnored() = runTest {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet))))

        assertThat(useCase.summary("app-1", "   ", profileSummary = "x")).isFalse()
        assertThat(stored().summary).isNull()
    }

    @Test
    fun skillsEditSplitsOnCommasDropsBlanksAndDuplicates() = runTest {
        val tailored = TailoredSkills(
            skills = listOf("Kotlin", "SQL"),
            original = listOf("Kotlin"),
            violations = listOf(GuardrailViolation.UnsupportedTerm("Rust")),
            decision = BulletDecision.PENDING,
        )
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet), skills = tailored)))

        val saved = useCase.skills("app-1", " Python , SQL,, sql ,Excel ", profileSkills = listOf("Kotlin"))

        assertThat(saved).isTrue()
        assertThat(stored().skills).isEqualTo(
            TailoredSkills(
                skills = listOf("Python", "SQL", "Excel"),
                original = listOf("Kotlin"),
                decision = BulletDecision.ACCEPTED,
            ),
        )
    }

    @Test
    fun skillsEditWithoutATailoredSkillsLineOriginatesFromTheProfileSkills() = runTest {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet))))

        useCase.skills("app-1", "Go, SQL", profileSkills = listOf("Kotlin", "SQL"))

        assertThat(stored().skills?.original).containsExactly("Kotlin", "SQL").inOrder()
        assertThat(stored().skills?.skills).containsExactly("Go", "SQL").inOrder()
    }

    @Test
    fun blankSkillsAreIgnored() = runTest {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet))))

        assertThat(useCase.skills("app-1", " , ,", profileSkills = listOf("Kotlin"))).isFalse()
        assertThat(stored().skills).isNull()
    }

    @Test
    fun missingTailoredResumeIsNotEdited() = runTest {
        assertThat(useCase.summary("nope", "x", profileSummary = "")).isFalse()
        assertThat(useCase.skills("nope", "x", profileSkills = emptyList())).isFalse()
    }

    @Test
    fun editsKeepExportAllowedAfterAccept() = runTest {
        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(bullet))))

        useCase.summary("app-1", "Mine", profileSummary = "p")
        useCase.skills("app-1", "Go", profileSkills = listOf("Kotlin"))

        val application = checkNotNull(applicationRepository.observeApplication("app-1").first())
        assertThat(ExportReadiness.check(application)).isEqualTo(ExportCheck.ALLOWED)
        assertThat(application.changesAcceptedAt).isEqualTo(EXPORT_ACCEPTED_AT)
    }
}

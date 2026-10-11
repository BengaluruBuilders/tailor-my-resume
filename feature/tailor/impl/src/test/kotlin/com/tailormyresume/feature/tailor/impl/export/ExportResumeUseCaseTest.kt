package com.tailormyresume.feature.tailor.impl.export

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.acceptedApplication
import com.tailormyresume.feature.tailor.impl.acceptedBullet
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExportResumeUseCaseTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()
    private val settingsRepository = TestResumeSettingsRepository()
    private val exportHistory = TestExportHistoryRepository()
    private val clock = TestClock()

    private lateinit var renderer: FakeResumePdfRenderer
    private lateinit var useCase: ExportResumeUseCase

    private val b1 = acceptedBullet(
        id = "b1",
        original = "Built an internal tool",
        proposed = "Developed an internal tool",
    )

    @Before
    fun setUp() {
        renderer = FakeResumePdfRenderer(folder.newFolder())
        useCase = ExportResumeUseCase(
            applicationRepository,
            profileRepository,
            settingsRepository,
            exportHistory,
            ResumeDocumentAssembler(TestResumeHeadings),
            renderer,
            clock,
        )
    }

    private fun profile(fullName: String = "Priya Deshmukh", bullet: com.tailormyresume.core.model.TailoredBullet = b1): CandidateProfile =
        testProfile(listOf(entryFor("exp-1", bullet))).copy(fullName = fullName)

    private suspend fun seed(application: JobApplication, candidate: CandidateProfile = profile()) {
        applicationRepository.sendApplications(listOf(application))
        profileRepository.sendProfile(candidate)
    }

    private fun ExportOutcome.exported(): ExportOutcome.Exported =
        this as? ExportOutcome.Exported ?: error("expected ExportOutcome.Exported but was $this")

    @Test
    fun writesFileNamedByCoreFormatAtSettingsPageSize() = runTest {
        seed(acceptedApplication(listOf(b1)))

        val outcome = useCase("app-1")

        assertThat(outcome.exported().fileName).isEqualTo("Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf")
        assertThat(renderer.lastPageSize).isEqualTo(PageSize.A4)

        settingsRepository.update { it.copy(pageSize = PageSize.LETTER, fileNameFormat = FileNameFormat.NAME_ROLE) }

        val renamed = useCase("app-1").exported()

        assertThat(renamed.fileName).isEqualTo("Priya-Deshmukh_Associate-Analyst.pdf")
        assertThat(renderer.lastPageSize).isEqualTo(PageSize.LETTER)
    }

    @Test
    fun indicNameKeepsCombiningMarks() = runTest {
        seed(acceptedApplication(listOf(b1)), profile("प्रिया देशमुख"))
        settingsRepository.update { it.copy(fileNameFormat = FileNameFormat.NAME_RESUME) }

        val outcome = useCase("app-1")

        assertThat(outcome.exported().fileName).isEqualTo("प्रिया-देशमुख_Resume.pdf")
    }

    @Test
    fun storesExportFileNameOnApplication() = runTest {
        seed(acceptedApplication(listOf(b1)))

        val fileName = useCase("app-1").exported().fileName
        val stored = applicationRepository.observeApplication("app-1").first()
        val records = exportHistory.observeExports("app-1").first()

        assertThat(stored?.exportFileName).isEqualTo(fileName)
        assertThat(records).hasSize(1)
        assertThat(records.single().format).isEqualTo(ExportFormat.PDF)
        assertThat(records.single().fileName).isEqualTo(fileName)
        assertThat(records.single().pageCount).isEqualTo(1)
        assertThat(records.single().exportedAt).isEqualTo(clock.now())
    }

    @Test
    fun refusesWhenNotAcceptedOrPending() = runTest {
        seed(acceptedApplication(listOf(b1), accepted = false))

        val refused = useCase("app-1")

        assertThat(refused).isEqualTo(ExportOutcome.Refused)
        assertThat(renderer.renderCalls).isEqualTo(0)
        assertThat(exportHistory.observeExports("app-1").first()).isEmpty()
        assertThat(applicationRepository.observeApplication("app-1").first()?.exportFileName).isNull()

        val pending = acceptedBullet(
            id = "b1",
            original = "Built an internal tool",
            proposed = "Developed an internal tool",
            decision = BulletDecision.PENDING,
        )
        seed(acceptedApplication(listOf(pending)), profile(bullet = pending))

        val stillRefused = useCase("app-1")

        assertThat(stillRefused).isEqualTo(ExportOutcome.Refused)
        assertThat(renderer.renderCalls).isEqualTo(0)
    }

    @Test
    fun missingApplicationOrProfileIsRefused() = runTest {
        assertThat(useCase("app-1")).isEqualTo(ExportOutcome.Refused)

        applicationRepository.sendApplications(listOf(acceptedApplication(listOf(b1))))

        assertThat(useCase("app-1")).isEqualTo(ExportOutcome.Refused)
        assertThat(renderer.renderCalls).isEqualTo(0)
    }

    @Test
    fun exportedOutcomeCarriesFileAndSize() = runTest {
        seed(acceptedApplication(listOf(b1)))

        val exported = useCase("app-1").exported()

        assertThat(exported.file.exists()).isTrue()
        assertThat(exported.file.name).isEqualTo(exported.fileName)
        assertThat(exported.pageCount).isEqualTo(renderer.pageCount)
        assertThat(exported.sizeBytes).isEqualTo(exported.file.length())
    }

    @Test
    fun reexportPassesThePreviousFileNameToTheRenderer() = runTest {
        seed(acceptedApplication(listOf(b1)))
        val first = useCase("app-1").exported().fileName
        assertThat(renderer.lastPreviousFileName).isNull()

        settingsRepository.update { it.copy(fileNameFormat = FileNameFormat.NAME_ROLE) }
        useCase("app-1").exported()

        assertThat(renderer.lastPreviousFileName).isEqualTo(first)
    }
}

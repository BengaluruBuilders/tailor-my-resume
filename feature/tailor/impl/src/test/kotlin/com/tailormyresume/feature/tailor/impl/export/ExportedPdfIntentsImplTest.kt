package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.util.TestClock
import com.tailormyresume.feature.tailor.impl.acceptedApplication
import com.tailormyresume.feature.tailor.impl.acceptedBullet
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import com.tailormyresume.feature.tailor.impl.document.ResumeDocumentAssembler
import com.tailormyresume.feature.tailor.impl.document.TestResumeHeadings
import com.tailormyresume.feature.tailor.impl.entryFor
import com.tailormyresume.feature.tailor.impl.exported.CacheExportedFileStore
import com.tailormyresume.feature.tailor.impl.testProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportedPdfIntentsImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val exportsDirectory = File(context.cacheDir, "exports")
    private val applicationRepository = TestApplicationRepository()
    private val profileRepository = TestProfileRepository()

    private val renderer = DirectoryRenderer(ExportDirectory(exportsDirectory))

    private val exportResume = ExportResumeUseCase(
        applicationRepository,
        profileRepository,
        TestResumeSettingsRepository(),
        TestExportHistoryRepository(),
        ResumeDocumentAssembler(TestResumeHeadings),
        renderer,
        TestClock(),
    )

    private val intents = ExportedPdfIntentsImpl(
        context,
        applicationRepository,
        exportResume,
        CacheExportedFileStore(context),
        Dispatchers.Unconfined,
    )

    private val bullet = acceptedBullet(id = "b1", original = "Built an internal tool", proposed = "Developed an internal tool")

    @Before
    fun setUp() {
        exportsDirectory.deleteRecursively()
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    private fun application(id: String, company: String, accepted: Boolean = true): JobApplication =
        acceptedApplication(listOf(bullet), company = company, accepted = accepted).copy(id = id)

    private suspend fun seed(vararg applications: JobApplication) {
        profileRepository.sendProfile(testProfile(listOf(entryFor("exp-1", bullet))).copy(fullName = "Priya Deshmukh"))
        applicationRepository.sendApplications(applications.toList())
    }

    private suspend fun exportedFileName(id: String): String =
        checkNotNull(applicationRepository.observeApplication(id).first()?.exportFileName)

    private fun Intent.streamedFileName(): String? {
        val send = getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        return send?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.lastPathSegment
    }

    @Test
    fun shareAndOpenForApplicationAAfterApplicationBWasExported() = runTest {
        seed(application("a", "Northwind GCC"), application("b", "Kestrel Bank"))
        exportResume("a")
        exportResume("b")
        val rendersBefore = renderer.renderCalls

        val share = checkNotNull(intents.shareIntent("a"))
        val view = checkNotNull(intents.viewIntent("a"))

        assertThat(share.streamedFileName()).isEqualTo(exportedFileName("a"))
        assertThat(view.data?.lastPathSegment).isEqualTo(exportedFileName("a"))
        assertThat(view.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(renderer.renderCalls).isEqualTo(rendersBefore)
    }

    @Test
    fun missingFileIsRerenderedForThatApplicationOnly() = runTest {
        seed(application("a", "Northwind GCC"), application("b", "Kestrel Bank"))
        exportResume("a")
        exportResume("b")
        val aFile = File(File(exportsDirectory, "a"), exportedFileName("a"))
        val bFile = File(File(exportsDirectory, "b"), exportedFileName("b"))
        check(aFile.delete())
        val rendersBefore = renderer.renderCalls

        val share = checkNotNull(intents.shareIntent("a"))

        assertThat(share.streamedFileName()).isEqualTo(aFile.name)
        assertThat(aFile.isFile).isTrue()
        assertThat(bFile.isFile).isTrue()
        assertThat(renderer.renderCalls).isEqualTo(rendersBefore + 1)
        assertThat(renderer.renderedFileNames.last()).isEqualTo(aFile.name)
    }

    @Test
    fun unacceptedApplicationWithoutFileReturnsNull() = runTest {
        seed(application("a", "Northwind GCC", accepted = false))

        assertThat(intents.shareIntent("a")).isNull()
        assertThat(intents.viewIntent("a")).isNull()
        assertThat(intents.shareIntent("missing")).isNull()
        assertThat(renderer.renderCalls).isEqualTo(0)
    }

    private class DirectoryRenderer(private val directory: ExportDirectory) : ResumePdfRenderer {
        val renderedFileNames = mutableListOf<String>()
        val renderCalls: Int get() = renderedFileNames.size

        override suspend fun render(
            applicationId: String,
            document: ResumeDocument,
            fileName: String,
            pageSize: PageSize,
            previousFileName: String?,
        ): RenderedResume {
            renderedFileNames += fileName
            val file = directory.write(applicationId, fileName, previousFileName) { it.write("%PDF-1.4".toByteArray()) }
            return RenderedResume(file = file, pageCount = 1, lines = emptyList())
        }

        override suspend fun pageCount(document: ResumeDocument, pageSize: PageSize): Int = 1
    }
}

package com.tailormyresume.feature.onboarding.impl.importresume

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.OfflineResumeTextParser
import com.tailormyresume.core.model.EntryCategory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
class PdfResumeTextDeviceTest {
    @Test
    fun platformReaderIdentifiesScannedPdfWithoutInventingText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bytes = instrumentation.context.assets.open("pdf/scanned.pdf").use { it.readBytes() }
        assertThat(readPdfResumeText(instrumentation.targetContext, bytes, "scanned.pdf"))
            .isEqualTo(ResumeRead.NoTextLayer("scanned.pdf"))
    }

    @Test
    fun platformReaderRecoversAllSyntheticResumeLayouts() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for (name in listOf("uncompressed.pdf", "compressed.pdf", "font-subset.pdf", "latex.pdf")) {
            val bytes = instrumentation.context.assets.open("pdf/$name").use { it.readBytes() }
            val read = readPdfResumeText(instrumentation.targetContext, bytes, name)
            assertThat(read).isInstanceOf(ResumeRead.Text::class.java)
            val text = (read as ResumeRead.Text).text
            assertThat(text.lines().map(String::trim)).containsAtLeast(
                "Sample Candidate",
                "sample@example.test",
                "Experience",
                "Data Analyst | Example Company",
                "Jan 2020 - Dec 2022",
                "Built SQL reports for the finance team.",
                "Skills",
                "SQL, Excel, Python",
            ).inOrder()
            val profile = OfflineResumeTextParser().parse(text)
            assertThat(profile.skills).containsAtLeast("SQL", "Excel", "Python")
            assertThat(profile.entries.any { it.category == EntryCategory.EXPERIENCE }).isTrue()
        }
        assertThat(instrumentation.targetContext.cacheDir.listFiles().orEmpty().none { it.name.startsWith("resume-read-") }).isTrue()
    }
}

package com.tailormyresume.feature.tailor.impl.export

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import com.tailormyresume.feature.tailor.impl.document.ResumeEntry
import com.tailormyresume.feature.tailor.impl.document.ResumeSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
class AndroidPdfResumeRendererDeviceTest {
    @Test
    fun renderedResumeKeepsSearchableTextOnA4AndLetter() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val renderer = AndroidPdfResumeRenderer(context, Dispatchers.Default, Dispatchers.IO)
        val files = mutableListOf<File>()

        try {
            val a4 = renderer.render("pdf-text-a4", document, "resume-a4.pdf", PageSize.A4, null)
            files += a4.file
            val letter = renderer.render("pdf-text-letter", document, "resume-letter.pdf", PageSize.LETTER, null)
            files += letter.file

            assertThat(a4.pageCount).isEqualTo(1)
            assertThat(letter.pageCount).isEqualTo(1)

            val text = extractText(a4.file).replace(Regex("\\s+"), " ").lowercase()
            assertThat(text).contains("find")
            assertThat(text).contains("flow")
            assertThat(text).contains("office")
            assertThat(text).contains("i find workflow improvements in office finance teams")
            assertThat(text).contains("workflow design")
            assertThat(text).contains("microsoft")
            assertThat(text).contains("financial analysis")
            assertThat(text).contains("edited bullet")
            assertThat(text).contains("finds and fixes flow blockers")
        } finally {
            files.forEach { it.delete() }
        }
    }

    private fun extractText(file: File): String =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                buildString {
                    repeat(renderer.pageCount) { index ->
                        renderer.openPage(index).use { page ->
                            page.textContents.forEach { append(it.text).append('\n') }
                        }
                    }
                }
            }
        }

    private val document = ResumeDocument(
        name = "Priya Deshmukh",
        contactLine = "priya@example.com · Pune",
        headline = "Finance Analyst",
        summary = "I find workflow improvements in office finance teams.",
        skills = listOf("Workflow design", "Microsoft Office", "Financial analysis"),
        skillsHeading = "Skills",
        sections = listOf(
            ResumeSection(
                category = EntryCategory.EXPERIENCE,
                heading = "Experience",
                entries = listOf(
                    ResumeEntry(
                        title = "Finance Analyst",
                        organization = "Northwind",
                        dateRange = "2024 – Present",
                        bullets = listOf("Edited bullet finds and fixes flow blockers."),
                    ),
                ),
            ),
        ),
    )
}

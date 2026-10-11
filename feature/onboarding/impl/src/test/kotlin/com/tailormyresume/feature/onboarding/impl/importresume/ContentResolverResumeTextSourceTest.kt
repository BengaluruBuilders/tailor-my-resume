package com.tailormyresume.feature.onboarding.impl.importresume

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ContentResolverResumeTextSourceTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val uri = Uri.parse("content://test.resume/big.pdf")

    @Test
    @Config(sdk = [28])
    fun uncompressedPdfPreservesResumeLines() = assertResumeLines("uncompressed.pdf")

    @Test
    @Config(sdk = [28])
    fun compressedPdfPreservesResumeLines() = assertResumeLines("compressed.pdf")

    @Test
    @Config(sdk = [28])
    fun subsetFontPdfDoesNotClaimImageOnly() {
        val read = readCorpus("font-subset.pdf")
        assertThat(read).isNotInstanceOf(ResumeRead.NoTextLayer::class.java)
    }

    @Test
    @Config(sdk = [28])
    fun latexSubsetFontUsesNeutralFailure() {
        assertThat(readCorpus("latex.pdf")).isEqualTo(ResumeRead.Unreadable("latex.pdf"))
    }

    @Test
    @Config(sdk = [28])
    fun olderReaderUsesNeutralFailureWhenItCannotEstablishTextLayer() {
        assertThat(readCorpus("scanned.pdf")).isInstanceOf(ResumeRead.Unreadable::class.java)
    }

    private fun assertResumeLines(name: String) {
        val read = readCorpus(name)
        assertThat(read).isInstanceOf(ResumeRead.Text::class.java)
        val text = (read as ResumeRead.Text).text
        assertThat(text.lines()).containsAtLeast(
            "Sample Candidate",
            "sample@example.test",
            "Experience",
            "Data Analyst | Example Company",
            "Jan 2020 - Dec 2022",
            "Built SQL reports for the finance team.",
            "Skills",
            "SQL, Excel, Python",
        ).inOrder()
    }

    private fun readCorpus(name: String): ResumeRead {
        val bytes = requireNotNull(javaClass.getResourceAsStream("/pdf/$name")).use { it.readBytes() }
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        val file = ResumeFile(name, RESUME_PDF_MIME, bytes.size.toLong(), uri.toString())
        return runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }
    }

    @Test
    fun overLimitFileWithUnknownSizeIsTooLargeNotParsedTruncated() {
        val oversize = RESUME_READ_LIMIT_BYTES + 1
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArray(oversize.toInt()).inputStream())
        val file = ResumeFile("big.pdf", RESUME_PDF_MIME, 0L, uri.toString())

        val read = runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }

        assertThat(read).isEqualTo(ResumeRead.TooLarge(oversize, RESUME_READ_LIMIT_BYTES))
    }

    @Test
    fun compressedDocxWithOversizeDocumentIsUnreadable() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(ByteArray((RESUME_READ_LIMIT_BYTES + 1L).toInt()) { 'a'.code.toByte() })
            zip.closeEntry()
        }
        val bytes = output.toByteArray()
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        val file = ResumeFile("large.docx", RESUME_DOCX_MIME, bytes.size.toLong(), uri.toString())

        val read = runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }

        assertThat(read).isEqualTo(ResumeRead.Unreadable("large.docx"))
    }

    @Test
    fun compressedDocxWithOversizeEntryBeforeDocumentIsUnreadable() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("word/media/large.bin"))
            zip.write(ByteArray((RESUME_READ_LIMIT_BYTES + 1L).toInt()) { 'a'.code.toByte() })
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write("<w:p><w:t>Sample Candidate worked at Example Company building finance reports.</w:t></w:p>".toByteArray())
            zip.closeEntry()
        }
        val bytes = output.toByteArray()
        shadowOf(context.contentResolver).registerInputStream(uri, bytes.inputStream())
        val file = ResumeFile("large.docx", RESUME_DOCX_MIME, bytes.size.toLong(), uri.toString())

        val read = runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }

        assertThat(read).isEqualTo(ResumeRead.Unreadable("large.docx"))
    }
}

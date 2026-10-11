package com.tailormyresume.feature.onboarding.impl.importresume

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.OfflineResumeTextParser
import com.tailormyresume.core.model.EntryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

@RunWith(AndroidJUnit4::class)
class ContentResolverPdfTextTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val resumeLines = listOf(
        "Priya Deshmukh",
        "priya.deshmukh@example.com | +91 98765 43210 | Pune, India",
        "",
        "EXPERIENCE",
        "Senior Backend Engineer, Acme Payments",
        "Jan 2021 - Present",
        "- Built a payments ledger service handling 2 million transactions daily.",
        "",
        "Backend Engineer, Bluebird Logistics",
        "Jun 2018 - Dec 2020",
        "- Developed routing APIs used by 300 delivery partners.",
        "",
        "EDUCATION",
        "B.E. Computer Engineering, Pune University, 2018",
        "",
        "SKILLS",
        "Kotlin, Java, PostgreSQL, Kafka, Docker",
    )

    private fun read(pdf: ByteArray): ResumeRead {
        val uri = Uri.parse("content://test.resume/${pdf.contentHashCode()}.pdf")
        shadowOf(context.contentResolver).registerInputStream(uri, pdf.inputStream())
        val file = ResumeFile("resume.pdf", RESUME_PDF_MIME, pdf.size.toLong(), uri.toString())
        return runBlocking { ContentResolverResumeTextSource(context, Dispatchers.IO).read(file) }
    }

    private fun text(pdf: ByteArray): List<String> = (read(pdf) as ResumeRead.Text).text.lines()

    private fun deflate(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun pdf(streamBody: ByteArray, filter: String = ""): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("%PDF-1.4\n1 0 obj\n<</Type/Catalog>>\nendobj\n4 0 obj\n<</Length ${streamBody.size}$filter>>\nstream\n".toByteArray(Charsets.ISO_8859_1))
        out.write(streamBody)
        out.write("\nendstream\nendobj\ntrailer\n<</Root 1 0 R>>\n%%EOF\n".toByteArray(Charsets.ISO_8859_1))
        return out.toByteArray()
    }

    private fun content(operators: String) = operators.toByteArray(Charsets.ISO_8859_1)

    private fun tmContent(lines: List<String>): String = buildString {
        append("BT /F1 11 Tf\n")
        lines.forEachIndexed { index, line -> append("1 0 0 1 50 ${790 - index * 14} Tm ($line) Tj\n") }
        append("ET")
    }

    private fun tStarContent(lines: List<String>): String = buildString {
        append("BT /F1 11 Tf 14 TL 50 790 Td\n")
        lines.forEach { append("($it) Tj T*\n") }
        append("ET")
    }

    @Test
    fun uncompressedTmPdfKeepsEveryLine() {
        assertThat(text(pdf(content(tmContent(resumeLines))))).containsAtLeastElementsIn(resumeLines.filter { it.isNotEmpty() }).inOrder()
    }

    @Test
    fun flateTmPdfKeepsEveryLine() {
        val pdf = pdf(deflate(content(tmContent(resumeLines))), "/Filter/FlateDecode")
        assertThat(text(pdf)).containsAtLeastElementsIn(resumeLines.filter { it.isNotEmpty() }).inOrder()
    }

    @Test
    fun tStarBreaksLines() {
        val lines = text(pdf(content(tStarContent(resumeLines))))
        assertThat(lines).containsAtLeastElementsIn(resumeLines.filter { it.isNotEmpty() }).inOrder()
    }

    @Test
    fun tdWithVerticalMoveBreaksLinesAndHorizontalMoveDoesNot() {
        val body = "BT 50 790 Td (Jan 2021) Tj 80 0 Td (Present) Tj 0 -14 Td (Next line here) Tj -14 TD (Third line) Tj ET"
        assertThat(text(pdf(content(body)))).containsExactly(
            "Jan 2021 Present",
            "Next line here",
            "Third line",
        ).inOrder()
    }

    @Test
    fun quoteOperatorsBreakLines() {
        val body = "BT 14 TL 50 790 Td (First line of the resume) Tj (Second line of the resume) ' 1 2 (Third line of the resume) \" ET"
        assertThat(text(pdf(content(body)))).containsExactly(
            "First line of the resume",
            "Second line of the resume",
            "Third line of the resume",
        ).inOrder()
    }

    @Test
    fun tjArrayJoinsFragmentsWithoutBreakingTheLine() {
        val body = "BT 50 790 Td [(Kot) -20 (lin, Java) -400 (and SQL skills here)] TJ ET"
        assertThat(text(pdf(content(body)))).containsExactly("Kotlin, Java and SQL skills here")
    }

    @Test
    fun escapedParenthesesAndHexStringsAreDecoded() {
        val hex = "Hex line of text".toByteArray().joinToString("") { "%02X".format(it) }
        val body = "BT 50 790 Td (Escaped \\(paren\\) text line) Tj 0 -14 Td <$hex> Tj ET"
        assertThat(text(pdf(content(body)))).containsExactly("Escaped (paren) text line", "Hex line of text").inOrder()
    }

    @Test
    fun imageOnlyPdfIsNoTextLayer() {
        val image = pdf(content("q 200 0 0 200 10 10 cm /Im0 Do Q"))
        val withDct = image + "5 0 obj\n<</Subtype/Image/Filter/DCTDecode/Length 12>>\nstream\n".toByteArray() +
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02, 0x28, 0x29) + "\nendstream\nendobj\n".toByteArray()
        assertThat(read(withDct)).isEqualTo(ResumeRead.NoTextLayer("resume.pdf"))
    }

    @Test
    fun headerOnlyGarbageIsUnreadableNotImageOnly() {
        val garbage = "%PDF-1.4\n".toByteArray() + ByteArray(2048) { (it % 256).toByte() }
        assertThat(read(garbage)).isEqualTo(ResumeRead.Unreadable("resume.pdf"))
    }

    @Test
    fun brokenFlateStreamIsUnreadableNotImageOnly() {
        val broken = pdf(ByteArray(300) { (it * 7 % 251).toByte() }, "/Filter/FlateDecode")
        assertThat(read(broken)).isEqualTo(ResumeRead.Unreadable("resume.pdf"))
    }

    @Test
    fun textPdfParsesToRolesAndEducationOffline() = runBlocking {
        val extracted = (read(pdf(deflate(content(tmContent(resumeLines))), "/Filter/FlateDecode")) as ResumeRead.Text).text
        val profile = OfflineResumeTextParser().parse(extracted)
        assertThat(profile.fullName).isEqualTo("Priya Deshmukh")
        assertThat(profile.entries.count { it.category == EntryCategory.EXPERIENCE }).isEqualTo(2)
        assertThat(profile.entries.count { it.category == EntryCategory.EDUCATION }).isEqualTo(1)
        assertThat(profile.skills).contains("Kotlin")
    }
}

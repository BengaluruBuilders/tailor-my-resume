package com.tailormyresume.feature.onboarding.impl.importresume

import android.content.Context
import android.database.Cursor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ContentResolverResumeTextSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(TmrDispatchers.IO) private val dispatcher: CoroutineDispatcher,
) : ResumeTextSource {

    override suspend fun read(file: ResumeFile): ResumeRead = withContext(dispatcher) {
        if (!isSupported(file)) {
            return@withContext ResumeRead.Unsupported(file.displayName)
        }
        val measured = measure(file)
        if (measured.byteSize > RESUME_READ_LIMIT_BYTES) {
            return@withContext ResumeRead.TooLarge(measured.byteSize, RESUME_READ_LIMIT_BYTES)
        }
        val bytes = readBytes(file.uri) ?: return@withContext ResumeRead.Unreadable(measured.displayName)
        if (bytes.size > RESUME_READ_LIMIT_BYTES) {
            return@withContext ResumeRead.TooLarge(bytes.size.toLong(), RESUME_READ_LIMIT_BYTES)
        }
        if (bytes.isEmpty()) {
            return@withContext ResumeRead.Empty(measured.byteSize)
        }
        val text = when {
            isZipContainer(bytes) -> textFromZip(bytes)
            isPdfContainer(bytes) -> textFromPdf(bytes)
            else -> textFromPlainBytes(bytes)
        } ?: return@withContext ResumeRead.NoTextLayer(measured.displayName)
        ResumeRead.Text(text)
    }

    private fun isSupported(file: ResumeFile): Boolean {
        val mime = file.mimeType.lowercase()
        if (mime == RESUME_PDF_MIME || mime == RESUME_DOCX_MIME) return true
        val name = file.displayName.lowercase()
        return name.endsWith(".pdf") || name.endsWith(".docx")
    }

    private fun measure(file: ResumeFile): MeasuredFile {
        if (file.byteSize > 0L) return MeasuredFile(file.displayName, file.byteSize)
        val uri = file.uri.toUri()
        var size = 0L
        var name = file.displayName
        runCatching {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.SIZE, OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        size = cursor.longAtOrZero(0)
                        name = cursor.textAt(1).ifBlank { file.displayName }
                    }
                }
        }
        return MeasuredFile(name, size)
    }

    private fun readBytes(uriText: String): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uriText.toUri())?.use { stream ->
            stream.toByteArrayLimited(RESUME_READ_LIMIT_BYTES + 1L)
        }
    }.getOrNull()

    private fun InputStream.toByteArrayLimited(limit: Long): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_CHUNK_BYTES)
        var total = 0L
        while (total < limit) {
            val read = read(chunk)
            if (read <= 0) break
            buffer.write(chunk, 0, read)
            total += read
        }
        return buffer.toByteArray()
    }

    private fun Cursor.longAtOrZero(index: Int): Long = if (isNull(index)) 0L else getLong(index)

    private fun Cursor.textAt(index: Int): String = if (isNull(index)) "" else getString(index).orEmpty()
}

private const val DEFAULT_CHUNK_BYTES = 8 * 1024
private const val PDF_STREAM_MARKER = "stream"
private const val PDF_END_STREAM_MARKER = "endstream"
private const val PDF_HEADER = "%PDF-"
private const val ZIP_MAGIC_FIRST = 0x50
private const val ZIP_MAGIC_SECOND = 0x4B
private const val ZIP_ENTRY_MARKER = "word/document.xml"
private val XML_TAG = Regex("<[^>]*>")
private const val XML_PARAGRAPH_END = "</w:p>"
private val PDF_LITERAL = Regex("\\((?:\\\\.|[^\\\\()])*\\)")
private val PDF_HEX = Regex("<([0-9A-Fa-f\\s]+)>")
private val WORD = Regex("[A-Za-z][A-Za-z'\\-]+")
private const val TEXT_CHARACTERS = " .,;:()'&/\\-–—%+#@"
private const val PRINTABLE_MIN_RATIO = 0.9
private const val MIN_TEXT_CHARACTERS = 24
private const val MIN_TEXT_WORDS = 5

private data class MeasuredFile(val displayName: String, val byteSize: Long)

private fun isPdfContainer(bytes: ByteArray): Boolean =
    bytes.size >= PDF_HEADER.length &&
        String(bytes, 0, PDF_HEADER.length, Charsets.ISO_8859_1) == PDF_HEADER

private fun isZipContainer(bytes: ByteArray): Boolean =
    bytes.size >= 2 &&
        bytes[0].toInt() == ZIP_MAGIC_FIRST &&
        bytes[1].toInt() == ZIP_MAGIC_SECOND

private fun textFromZip(bytes: ByteArray): String? {
    val xml = runCatching {
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            var document = ""
            while (entry != null) {
                if (entry.name == ZIP_ENTRY_MARKER) {
                    document = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
                entry = zip.nextEntry
            }
            stripXml(document)
        }
    }.getOrNull().orEmpty()
    return xml.takeIf(::looksLikeText)
}

private fun stripXml(xml: String): String =
    xml.replace(XML_PARAGRAPH_END, "\n").replace(XML_TAG, "").replace("&amp;", "&")

private fun textFromPdf(bytes: ByteArray): String? {
    val tokens = pdfStreams(bytes)
        .asSequence()
        .mapNotNull(::inflateOrNull)
        .map(::pdfTokens)
        .joinToString(separator = " ")
    return tokens.takeIf(::looksLikeText)
}

private fun pdfStreams(bytes: ByteArray): List<String> {
    val raw = String(bytes, Charsets.ISO_8859_1)
    val streams = mutableListOf<String>()
    var cursor = raw.indexOf(PDF_STREAM_MARKER)
    while (cursor >= 0) {
        val bodyStart = raw.indexOf('\n', cursor)
        if (bodyStart < 0) break
        val bodyEnd = raw.indexOf(PDF_END_STREAM_MARKER, bodyStart)
        if (bodyEnd < 0) break
        streams += raw.substring(bodyStart + 1, bodyEnd)
        cursor = raw.indexOf(PDF_STREAM_MARKER, bodyEnd)
    }
    return streams
}

private fun inflateOrNull(fragment: String): String? {
    val data = fragment.trim('\r', '\n', ' ').toByteArray(Charsets.ISO_8859_1)
    return listOf(false, true).firstNotNullOfOrNull { nowrap ->
        val inflater = Inflater(nowrap)
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(4 * 1024)
        val result = runCatching {
            inflater.setInput(data)
            while (!inflater.finished()) {
                val produced = inflater.inflate(chunk)
                if (produced == 0) break
                output.write(chunk, 0, produced)
            }
            output.toByteArray().toString(Charsets.ISO_8859_1)
        }.getOrNull()
        inflater.end()
        result?.takeIf { it.isNotBlank() }
    }
}

private fun pdfTokens(inflated: String): String {
    val literals = PDF_LITERAL.findAll(inflated)
        .map { it.value.removePrefix("(").removeSuffix(")").unescapePdf() }
    val hex = PDF_HEX.findAll(inflated).map { it.decodeHex() }
    return (literals + hex)
        .filter { token -> token.isNotEmpty() && token.all { character -> character.isReadable() } }
        .joinToString(separator = " ")
}

private fun String.unescapePdf(): String =
    replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\")

private fun Char.isReadable(): Boolean = isLetterOrDigit() || this in TEXT_CHARACTERS

private fun MatchResult.decodeHex(): String {
    val digits = groupValues[1].filter { character ->
        character.isDigit() || character in 'a'..'f' || character in 'A'..'F'
    }
    if (digits.isEmpty() || digits.length % 2 != 0) return ""
    val decoded = StringBuilder()
    var index = 0
    while (index < digits.length) {
        val code = digits.substring(index, index + 2).toIntOrNull(16) ?: return ""
        decoded.append(code.toChar())
        index += 2
    }
    return decoded.toString()
}

private fun textFromPlainBytes(bytes: ByteArray): String? =
    String(bytes, Charsets.UTF_8).takeIf(::looksLikeText)

private fun looksLikeText(candidate: String): Boolean {
    if (candidate.length < MIN_TEXT_CHARACTERS) return false
    val printable = candidate.count { character -> character == '\n' || !character.isISOControl() }
    if (printable.toDouble() / candidate.length < PRINTABLE_MIN_RATIO) return false
    return WORD.findAll(candidate).count() >= MIN_TEXT_WORDS
}

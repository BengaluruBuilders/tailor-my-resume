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
                ?: return@withContext ResumeRead.Unreadable(measured.displayName)
            isPdfContainer(bytes) -> return@withContext readPdfResumeText(context, bytes, measured.displayName)
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
            stream.readLimitedBytes(RESUME_READ_LIMIT_BYTES + 1L)
        }
    }.getOrNull()

    private fun Cursor.longAtOrZero(index: Int): Long = if (isNull(index)) 0L else getLong(index)

    private fun Cursor.textAt(index: Int): String = if (isNull(index)) "" else getString(index).orEmpty()
}

private const val DEFAULT_CHUNK_BYTES = 8 * 1024
private const val PDF_HEADER = "%PDF-"
private const val ZIP_MAGIC_FIRST = 0x50
private const val ZIP_MAGIC_SECOND = 0x4B
private const val ZIP_ENTRY_MARKER = "word/document.xml"
private val XML_TAG = Regex("<[^>]*>")
private const val XML_PARAGRAPH_END = "</w:p>"
private val WORD = Regex("[A-Za-z][A-Za-z'\\-]+")
private const val PRINTABLE_MIN_RATIO = 0.9
private const val MIN_TEXT_CHARACTERS = 24
private const val MIN_TEXT_WORDS = 5

private data class MeasuredFile(val displayName: String, val byteSize: Long)

private fun InputStream.readLimitedBytes(limit: Long): ByteArray {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_CHUNK_BYTES)
    var total = 0L
    while (total < limit) {
        val read = read(chunk, 0, minOf(chunk.size.toLong(), limit - total).toInt())
        if (read <= 0) break
        buffer.write(chunk, 0, read)
        total += read
    }
    return buffer.toByteArray()
}

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
            var expanded = 0L
            while (entry != null) {
                val content = zip.readLimitedBytes(RESUME_READ_LIMIT_BYTES - expanded + 1L)
                expanded += content.size
                if (expanded > RESUME_READ_LIMIT_BYTES) return null
                if (entry.name == ZIP_ENTRY_MARKER) {
                    document = content.toString(Charsets.UTF_8)
                    break
                }
                entry = zip.nextEntry
            }
            stripXml(document)
        }
    }.getOrNull().orEmpty()
    return xml.takeIf(::looksLikeResumeText)
}

private fun stripXml(xml: String): String =
    xml.replace(XML_PARAGRAPH_END, "\n").replace(XML_TAG, "").replace("&amp;", "&")

private fun textFromPlainBytes(bytes: ByteArray): String? =
    String(bytes, Charsets.UTF_8).takeIf(::looksLikeResumeText)

internal fun looksLikeResumeText(candidate: String): Boolean {
    if (candidate.length < MIN_TEXT_CHARACTERS) return false
    val printable = candidate.count { character -> character == '\n' || !character.isISOControl() }
    if (printable.toDouble() / candidate.length < PRINTABLE_MIN_RATIO) return false
    return WORD.findAll(candidate).count() >= MIN_TEXT_WORDS
}

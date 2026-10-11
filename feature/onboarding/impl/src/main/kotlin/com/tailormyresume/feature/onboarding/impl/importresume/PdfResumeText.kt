package com.tailormyresume.feature.onboarding.impl.importresume

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.Normalizer
import java.util.zip.InflaterInputStream

private val PDF_STREAM = Regex("<<([^<>]*)>>\\s*stream(?:\\r\\n|\\n|\\r)")
private val STREAM_LENGTH = Regex("/Length\\s+(\\d+)\\b(?!\\s+\\d+\\s+R)")
private val PDF_TOKEN = Regex("\\((?:\\\\.|[^\\\\()])*\\)|<[A-Fa-f0-9\\s]+>|-?\\d+(?:\\.\\d+)?|BT|ET|Tm|Td|TD|T\\*|Tj|TJ")
private const val MAX_EXTRACTED_BYTES = 5 * 1024 * 1024

internal fun readPdfResumeText(context: Context, bytes: ByteArray, name: String): ResumeRead {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        val text = runCatching { nativePdfText(context, bytes) }.getOrNull()
        if (text != null) {
            return if (text.isBlank()) ResumeRead.NoTextLayer(name) else ResumeRead.Text(normalizePdfText(text))
        }
    }
    val text = legacyPdfText(bytes)
    return if (looksLikeResumeText(text)) ResumeRead.Text(normalizePdfText(text)) else ResumeRead.Unreadable(name)
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun nativePdfText(context: Context, bytes: ByteArray): String {
    val file = File.createTempFile("resume-read-", ".pdf", context.cacheDir)
    return try {
        file.writeBytes(bytes)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val text = StringBuilder()
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { page ->
                        page.textContents.forEach { content ->
                            check(text.length + content.text.length < MAX_EXTRACTED_BYTES)
                            text.append(content.text).append('\n')
                        }
                    }
                }
                text.toString()
            }
        }
    } finally {
        file.delete()
    }
}

private fun normalizePdfText(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC).trim()

private fun legacyPdfText(bytes: ByteArray): String = runCatching {
    val raw = bytes.toString(Charsets.ISO_8859_1)
    val text = StringBuilder()
    PDF_STREAM.findAll(raw).forEach { match ->
        val dictionary = match.groupValues[1]
        val length = STREAM_LENGTH.find(dictionary)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
        val start = match.range.last + 1
        if (length < 0 || length > bytes.size - start) return@forEach
        val stream = bytes.copyOfRange(start, start + length)
        val decoded = when {
            "/FlateDecode" in dictionary -> inflate(stream)
            "/Filter" in dictionary -> null
            else -> stream.toString(Charsets.ISO_8859_1)
        } ?: return@forEach
        val extracted = textOperations(decoded)
        check(text.length + extracted.length < MAX_EXTRACTED_BYTES)
        if (extracted.isNotBlank()) text.append(extracted).append('\n')
    }
    text.toString()
}.getOrDefault("")

private fun inflate(bytes: ByteArray): String? = runCatching {
    InflaterInputStream(bytes.inputStream()).use { stream ->
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        while (true) {
            val count = stream.read(chunk)
            if (count < 0) break
            check(output.size() + count <= MAX_EXTRACTED_BYTES)
            output.write(chunk, 0, count)
        }
        output.toString(Charsets.ISO_8859_1.name())
    }
}.getOrNull()

private fun textOperations(stream: String): String {
    val text = StringBuilder()
    val operands = mutableListOf<Double>()
    var inText = false
    var lastY: Double? = null
    PDF_TOKEN.findAll(stream).forEach { match ->
        val token = match.value
        when {
            token == "BT" -> {
                inText = true
                operands.clear()
            }
            token == "ET" -> {
                inText = false
                text.append('\n')
            }
            !inText -> Unit
            token.toDoubleOrNull() != null -> operands.add(token.toDouble())
            token == "Tm" -> {
                val y = operands.lastOrNull()
                if (lastY != null && y != lastY) text.append('\n')
                lastY = y
                operands.clear()
            }
            token == "Td" || token == "TD" -> {
                if (operands.lastOrNull() != 0.0) text.append('\n')
                operands.clear()
            }
            token == "T*" -> text.append('\n')
            token.startsWith("(") -> text.append(decodeLiteral(token))
            token.startsWith("<") -> text.append(decodeHex(token))
            token == "Tj" || token == "TJ" -> operands.clear()
        }
    }
    return text.toString().trim()
}

private fun decodeHex(token: String): String {
    val hex = token.drop(1).dropLast(1).filterNot(Char::isWhitespace)
    if (hex.length % 2 != 0) return ""
    val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return bytes.toString(Charsets.ISO_8859_1).takeIf { text -> text.none { it.isISOControl() && !it.isWhitespace() } }.orEmpty()
}

private fun decodeLiteral(token: String): String = token.drop(1).dropLast(1)
    .replace(Regex("\\\\([0-7]{1,3})")) { it.groupValues[1].toInt(8).toChar().toString() }
    .replace("\\n", "\n").replace("\\r", "\n").replace("\\t", "\t")
    .replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\")

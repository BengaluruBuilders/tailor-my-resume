package com.tailormyresume.feature.onboarding.impl.importresume

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

internal sealed interface PdfText {
    data class Found(val text: String) : PdfText
    data object ImageOnly : PdfText
    data object Corrupt : PdfText
}

private const val STREAM = "stream"
private const val END_STREAM = "endstream"
private const val FLATE = "FlateDecode"
private const val TEXT_OBJECT_BEGIN = "BT"
private const val KERNING_SPACE_THRESHOLD = -200.0
private const val MIN_TEXT_CHARACTERS = 24
private const val MIN_TEXT_WORDS = 5
private val WORD = Regex("[A-Za-z][A-Za-z'\\-]+")

internal fun extractPdfText(bytes: ByteArray): PdfText {
    val raw = String(bytes, Charsets.ISO_8859_1)
    val contents = mutableListOf<String>()
    var streamCount = 0
    var brokenFlate = 0
    var cursor = raw.indexOf(STREAM)
    while (cursor >= 0) {
        val bodyStart = raw.indexOf('\n', cursor)
        if (bodyStart < 0) break
        val bodyEnd = raw.indexOf(END_STREAM, bodyStart)
        if (bodyEnd < 0) break
        streamCount++
        val dictionary = raw.substring(raw.lastIndexOf("obj", cursor).coerceAtLeast(0), cursor)
        val body = raw.substring(bodyStart + 1, bodyEnd).removeSuffix("\n").removeSuffix("\r")
        if (FLATE in dictionary) {
            val inflated = inflateOrNull(body)
            if (inflated == null) brokenFlate++ else contents += inflated
        } else if (!dictionary.contains("/Filter")) {
            contents += body
        }
        cursor = raw.indexOf(STREAM, bodyEnd + END_STREAM.length)
    }
    val textual = contents.filter { it.contains(TEXT_OBJECT_BEGIN) }
    return when {
        streamCount == 0 -> PdfText.Corrupt
        textual.isEmpty() -> if (brokenFlate > 0) PdfText.Corrupt else PdfText.ImageOnly
        else -> textual.joinToString("\n") { contentStreamText(it) }
            .takeIf(::looksLikeText)
            ?.let(PdfText::Found)
            ?: PdfText.ImageOnly
    }
}

private fun inflateOrNull(body: String): String? {
    val data = body.toByteArray(Charsets.ISO_8859_1)
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

private fun contentStreamText(content: String): String {
    val text = StringBuilder()
    val operands = mutableListOf<Any>()
    var lastY: Double? = null

    fun breakLine() {
        if (text.isNotEmpty() && text.last() != '\n') text.append('\n')
    }

    fun joinWord() {
        if (text.isNotEmpty() && !text.last().isWhitespace()) text.append(' ')
    }

    fun number(index: Int): Double? = operands.getOrNull(index) as? Double

    var index = 0
    while (index < content.length) {
        val character = content[index]
        when {
            character == '(' -> {
                val (value, next) = readLiteral(content, index)
                operands += value
                index = next
            }
            character == '<' && content.getOrNull(index + 1) != '<' -> {
                val end = content.indexOf('>', index)
                if (end < 0) break
                operands += decodeHex(content.substring(index + 1, end))
                index = end + 1
            }
            character == '[' -> {
                val (items, next) = readArray(content, index)
                operands.add(items)
                index = next
            }
            character.isWhitespace() || character in "<>]{})" -> index++
            character == '/' -> {
                index++
                while (index < content.length && !content[index].isWhitespace() && content[index] !in "/[(<") index++
                operands += NAME
            }
            else -> {
                val start = index
                while (index < content.length && !content[index].isWhitespace() && content[index] !in "/[(<>") index++
                val token = content.substring(start, index)
                val value = token.toDoubleOrNull()
                if (value != null) {
                    operands += value
                } else {
                    when (token) {
                        "Tj" -> (operands.lastOrNull() as? String)?.let(text::append)
                        "'" -> {
                            breakLine()
                            (operands.lastOrNull() as? String)?.let(text::append)
                        }
                        "\"" -> {
                            breakLine()
                            (operands.lastOrNull() as? String)?.let(text::append)
                        }
                        "TJ" -> (operands.lastOrNull() as? List<*>)?.forEach { item ->
                            when {
                                item is String -> text.append(item)
                                item is Double && item < KERNING_SPACE_THRESHOLD -> joinWord()
                            }
                        }
                        "T*" -> breakLine()
                        "Td", "TD" -> {
                            val vertical = number(operands.size - 1)
                            if (vertical != null && vertical != 0.0) breakLine() else joinWord()
                        }
                        "Tm" -> {
                            val y = number(operands.size - 1)
                            if (lastY != null && y != lastY) breakLine() else joinWord()
                            lastY = y
                        }
                        "ET" -> breakLine()
                    }
                    operands.clear()
                }
            }
        }
    }
    return text.lines().joinToString("\n") { it.trim() }.trim()
}

private val NAME = Any()

private fun readLiteral(content: String, start: Int): Pair<String, Int> {
    val value = StringBuilder()
    var depth = 0
    var index = start
    while (index < content.length) {
        val character = content[index]
        when {
            character == '\\' && index + 1 < content.length -> {
                index++
                value.append(
                    when (val escaped = content[index]) {
                        'n', 'r' -> ' '
                        't' -> ' '
                        else -> escaped
                    },
                )
            }
            character == '(' -> {
                if (depth > 0) value.append(character)
                depth++
            }
            character == ')' -> {
                depth--
                if (depth == 0) return value.toString().filterNot { it.isISOControl() } to index + 1
                value.append(character)
            }
            else -> value.append(character)
        }
        index++
    }
    return value.toString() to content.length
}

private fun readArray(content: String, start: Int): Pair<List<Any>, Int> {
    val items = mutableListOf<Any>()
    var index = start + 1
    while (index < content.length && content[index] != ']') {
        val character = content[index]
        when {
            character == '(' -> {
                val (value, next) = readLiteral(content, index)
                items += value
                index = next
            }
            character == '<' -> {
                val end = content.indexOf('>', index).takeIf { it >= 0 } ?: return items to content.length
                items += decodeHex(content.substring(index + 1, end))
                index = end + 1
            }
            character.isWhitespace() -> index++
            else -> {
                val numberStart = index
                while (index < content.length && !content[index].isWhitespace() && content[index] !in "([<]") index++
                content.substring(numberStart, index).toDoubleOrNull()?.let { items += it }
            }
        }
    }
    return items to minOf(index + 1, content.length)
}

private fun decodeHex(hex: String): String {
    val digits = hex.filter { it.isLetterOrDigit() }
    if (digits.isEmpty() || digits.length % 2 != 0 || digits.any { it !in "0123456789abcdefABCDEF" }) return ""
    return digits.chunked(2).map { it.toInt(16).toChar() }.joinToString("").filterNot { it.isISOControl() }
}

private fun looksLikeText(candidate: String): Boolean =
    candidate.length >= MIN_TEXT_CHARACTERS && WORD.findAll(candidate).count() >= MIN_TEXT_WORDS

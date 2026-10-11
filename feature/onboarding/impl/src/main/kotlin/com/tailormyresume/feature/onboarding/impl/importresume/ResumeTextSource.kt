package com.tailormyresume.feature.onboarding.impl.importresume

data class ResumeFile(
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val uri: String,
)

sealed interface ResumeRead {
    data class Text(val text: String) : ResumeRead
    data class Empty(val byteSize: Long) : ResumeRead
    data class NoTextLayer(val displayName: String) : ResumeRead
    data class Unsupported(val displayName: String) : ResumeRead
    data class TooLarge(val byteSize: Long, val limitBytes: Long) : ResumeRead
    data class Unreadable(val displayName: String) : ResumeRead
}

interface ResumeTextSource {
    suspend fun read(file: ResumeFile): ResumeRead
}

const val RESUME_READ_LIMIT_BYTES: Long = 5L * 1024L * 1024L

const val MIN_PASTED_RESUME_CHARS: Int = 50

const val MAX_RESUME_CHARS: Int = 30_000

const val RESUME_PDF_MIME: String = "application/pdf"

const val RESUME_DOCX_MIME: String =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

fun resumeMimeTypes(): Array<String> = arrayOf(RESUME_PDF_MIME, RESUME_DOCX_MIME)

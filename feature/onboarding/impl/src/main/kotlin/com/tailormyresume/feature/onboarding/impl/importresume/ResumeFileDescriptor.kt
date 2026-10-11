package com.tailormyresume.feature.onboarding.impl.importresume

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

internal enum class ResumeFileKind { Pdf, Docx }

internal fun resumeFileKind(mimeType: String?, fileName: String?): ResumeFileKind? {
    val mime = mimeType.orEmpty().lowercase()
    val name = fileName.orEmpty().lowercase()
    return when {
        mime == RESUME_PDF_MIME || name.endsWith(".pdf") -> ResumeFileKind.Pdf
        mime == RESUME_DOCX_MIME || name.endsWith(".docx") -> ResumeFileKind.Docx
        else -> null
    }
}

internal fun Context.resumeFileFor(uri: Uri): ResumeFile {
    var name = uri.lastPathSegment.orEmpty()
    var size = 0L
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    if (!cursor.isNull(0)) name = cursor.getString(0).orEmpty().ifBlank { name }
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
    }
    return ResumeFile(displayName = name, mimeType = contentResolver.getType(uri).orEmpty(), byteSize = size, uri = uri.toString())
}

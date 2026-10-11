package com.tailormyresume.feature.onboarding.impl.importresume

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tailormyresume.feature.onboarding.impl.R
import java.util.Locale

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L

@Composable
internal fun resumeFileTypeLabel(mimeType: String?, fileName: String?): String =
    when (resumeFileKind(mimeType, fileName)) {
        ResumeFileKind.Pdf -> stringResource(R.string.feature_onboarding_impl_reading_type_pdf)
        ResumeFileKind.Docx -> stringResource(R.string.feature_onboarding_impl_reading_type_docx)
        null -> ""
    }

@Composable
internal fun resumeFileSizeText(bytes: Long): String =
    if (bytes >= BYTES_PER_MB) {
        stringResource(R.string.feature_onboarding_impl_file_size_mb, String.format(Locale.getDefault(), "%.1f", bytes / BYTES_PER_MB.toDouble()))
    } else {
        stringResource(R.string.feature_onboarding_impl_file_size_kb, maxOf(1L, bytes / BYTES_PER_KB).toInt())
    }

private const val META_SEPARATOR = " · "

@Composable
internal fun resumeFileMeta(
    mimeType: String?,
    fileName: String?,
    byteSize: Long,
    imageOnly: Boolean = false,
): String {
    val type = resumeFileTypeLabel(mimeType, fileName)
    val detail = when {
        imageOnly -> stringResource(R.string.feature_onboarding_impl_file_meta_image_only)
        byteSize > 0L -> resumeFileSizeText(byteSize)
        else -> ""
    }
    return listOf(type, detail).filter { it.isNotBlank() }.joinToString(META_SEPARATOR)
}

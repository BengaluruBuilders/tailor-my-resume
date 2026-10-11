package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

internal class BitmapPdfResumeRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ResumePdfRenderer {

    override suspend fun render(
        document: ResumeDocument,
        fileName: String,
        pageSize: PageSize,
        previousFileName: String?,
    ): RenderedResume {
        val fitted = ResumePdfFit.compose(document, pageSize, ::BitmapPdfPages)
        val file = ExportDirectory(File(context.cacheDir, "exports")).write(fileName, previousFileName) { it.write("%PDF-1.4".toByteArray()) }
        return RenderedResume(file = file, pageCount = fitted.pageCount, lines = fitted.lines)
    }

    override suspend fun pageCount(document: ResumeDocument, pageSize: PageSize): Int =
        ResumePdfFit.compose(document, pageSize, ::BitmapPdfPages).pageCount
}

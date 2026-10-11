package com.tailormyresume.feature.tailor.impl.export

import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument

internal class FittedResume<P : PdfPages>(val pages: P, val pageCount: Int, val lines: List<String>)

internal object ResumePdfFit {

    fun <P : PdfPages> compose(
        document: ResumeDocument,
        pageSize: PageSize,
        newPages: () -> P,
        discard: (P) -> Unit = {},
    ): FittedResume<P> {
        var fitted: FittedResume<P>? = null
        for (pass in FitPass.entries) {
            fitted?.let { discard(it.pages) }
            fitted = composeOnce(document, pageSize, pass, newPages())
            if (fitted.pageCount == 1) break
        }
        return checkNotNull(fitted)
    }

    private fun <P : PdfPages> composeOnce(
        document: ResumeDocument,
        pageSize: PageSize,
        pass: FitPass,
        pages: P,
    ): FittedResume<P> {
        val writer = PdfPageWriter(pages, pageSize)
        val composer = ResumePdfComposer(writer = writer, style = PdfResumeStyle(pass))
        composer.compose(document)
        return FittedResume(pages, writer.pageCount, composer.lines)
    }
}

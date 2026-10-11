package com.tailormyresume.feature.tailor.impl.export

import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument

internal interface ResumePdfRenderer {
    suspend fun render(
        applicationId: String,
        document: ResumeDocument,
        fileName: String,
        pageSize: PageSize,
        previousFileName: String? = null,
    ): RenderedResume

    suspend fun pageCount(document: ResumeDocument, pageSize: PageSize): Int
}

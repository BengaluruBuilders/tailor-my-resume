package com.tailormyresume.feature.tailor.impl.export

import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import java.io.File

internal class FakeResumePdfRenderer(
    private val directory: File,
    var pageCount: Int = 1,
) : ResumePdfRenderer {

    var renderCalls = 0
        private set
    var pageCountCalls = 0
        private set
    var lastFileName: String? = null
        private set
    var lastPreviousFileName: String? = null
        private set
    var lastPageSize: PageSize? = null
        private set
    var lastDocument: ResumeDocument? = null
        private set

    override suspend fun render(
        document: ResumeDocument,
        fileName: String,
        pageSize: PageSize,
        previousFileName: String?,
    ): RenderedResume {
        renderCalls += 1
        lastFileName = fileName
        lastPreviousFileName = previousFileName
        lastPageSize = pageSize
        lastDocument = document
        directory.mkdirs()
        val file = File(directory, fileName).apply { writeText("%PDF-1.4") }
        return RenderedResume(file = file, pageCount = pageCount, lines = emptyList())
    }

    override suspend fun pageCount(document: ResumeDocument, pageSize: PageSize): Int {
        pageCountCalls += 1
        lastPageSize = pageSize
        lastDocument = document
        return pageCount
    }
}

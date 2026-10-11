package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.feature.tailor.impl.document.ResumeDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class AndroidPdfResumeRenderer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(TmrDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
    @param:Dispatcher(TmrDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ResumePdfRenderer {

    override suspend fun render(
        document: ResumeDocument,
        fileName: String,
        pageSize: PageSize,
        previousFileName: String?,
    ): RenderedResume {
        val fitted = withContext(defaultDispatcher) { fit(document, pageSize) }
        try {
            return withContext(ioDispatcher) {
                val directory = ExportDirectory(File(context.cacheDir, EXPORT_DIRECTORY))
                val file = directory.write(fileName, previousFileName) { fitted.pages.writeTo(it) }
                RenderedResume(file = file, pageCount = fitted.pageCount, lines = fitted.lines)
            }
        } finally {
            fitted.pages.close()
        }
    }

    override suspend fun pageCount(document: ResumeDocument, pageSize: PageSize): Int =
        withContext(defaultDispatcher) {
            val fitted = fit(document, pageSize)
            fitted.pages.close()
            fitted.pageCount
        }

    private fun fit(document: ResumeDocument, pageSize: PageSize): FittedResume<AndroidPdfPages> =
        ResumePdfFit.compose(document, pageSize, newPages = ::AndroidPdfPages, discard = AndroidPdfPages::close)

    private companion object {
        const val EXPORT_DIRECTORY = "exports"
    }
}

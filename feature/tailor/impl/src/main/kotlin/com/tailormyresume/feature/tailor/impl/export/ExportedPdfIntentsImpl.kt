package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import android.content.Intent
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.feature.tailor.impl.exported.ExportedFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

internal class ExportedPdfIntentsImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val applicationRepository: ApplicationRepository,
    private val exportResume: ExportResumeUseCase,
    private val fileStore: ExportedFileStore,
    @param:Dispatcher(TmrDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ExportedPdfIntents {

    override suspend fun shareIntent(applicationId: String): Intent? =
        pdfFile(applicationId)?.let { file ->
            withContext(ioDispatcher) {
                createPdfShareIntent(
                    context,
                    file,
                    subject = context.getString(R.string.feature_tailor_impl_exported_share_subject),
                    chooserTitle = context.getString(R.string.feature_tailor_impl_exported_share_title),
                )
            }
        }

    override suspend fun viewIntent(applicationId: String): Intent? =
        pdfFile(applicationId)?.let { file -> withContext(ioDispatcher) { createPdfViewIntent(context, file) } }

    private suspend fun pdfFile(applicationId: String): File? {
        val application = applicationRepository.observeApplication(applicationId).first() ?: return null
        val stored = application.exportFileName?.let { name -> withContext(ioDispatcher) { fileStore.fileFor(applicationId, name) } }
        return if (application.exportFileName == null) rerender(applicationId) else stored
    }

    private suspend fun rerender(applicationId: String): File? =
        try {
            (exportResume(applicationId) as? ExportOutcome.Exported)?.file
        } catch (_: IOException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
}

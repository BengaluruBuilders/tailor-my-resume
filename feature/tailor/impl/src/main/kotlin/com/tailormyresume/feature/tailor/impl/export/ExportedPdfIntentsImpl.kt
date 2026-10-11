package com.tailormyresume.feature.tailor.impl.export

import android.content.Context
import android.content.Intent
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.feature.tailor.api.ExportedPdfIntents
import com.tailormyresume.feature.tailor.impl.exported.ExportedFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

internal class ExportedPdfIntentsImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val applicationRepository: ApplicationRepository,
    private val exportResume: ExportResumeUseCase,
    private val fileStore: ExportedFileStore,
    @param:Dispatcher(TmrDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ExportedPdfIntents {
    override suspend fun shareIntent(applicationId: String): Intent? = null

    override suspend fun viewIntent(applicationId: String): Intent? = null
}

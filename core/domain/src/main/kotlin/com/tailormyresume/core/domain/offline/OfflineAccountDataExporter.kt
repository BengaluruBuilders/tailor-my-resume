package com.tailormyresume.core.domain.offline

import android.content.Context
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockOperation
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataArchive
import com.tailormyresume.core.domain.account.AccountDataArchiveWriter
import com.tailormyresume.core.domain.account.AccountDataExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class OfflineAccountDataExporter internal constructor(
    private val directory: () -> File,
    private val latency: MockLatency,
    private val ioDispatcher: CoroutineDispatcher,
) : AccountDataExporter {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        latency: MockLatency,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    ) : this({ File(context.cacheDir, EXPORT_DIRECTORY) }, latency, ioDispatcher)

    override suspend fun export(data: AccountData): AccountDataArchive {
        latency.await(MockOperation.EXPORT_DATA)
        val target = File(directory(), FILE_NAME)
        target.parentFile?.listFiles()?.filter { it != target }?.forEach(File::deleteRecursively)
        withContext(ioDispatcher) { AccountDataArchiveWriter().write(data, target) }
        return AccountDataArchive(fileName = FILE_NAME, file = target)
    }

    private companion object {
        const val EXPORT_DIRECTORY = "data-exports"
        const val FILE_NAME = "tailormyresume-my-data.zip"
    }
}

package com.tailormyresume.app.account

import android.content.Context
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataArchive
import com.tailormyresume.core.domain.account.AccountDataArchiveWriter
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.ServerExportResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class RemoteAccountDataExporter internal constructor(
    private val directory: () -> File,
    private val api: TailorMyResumeApi,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) : AccountDataExporter {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        api: TailorMyResumeApi,
        json: Json,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    ) : this({ File(context.cacheDir, EXPORT_DIRECTORY) }, api, json, ioDispatcher)

    override suspend fun export(data: AccountData): AccountDataArchive {
        val serverPart = apiResult { api.exportMe() }.fold(
            onSuccess = { json.encodeToString(ServerExportResponse.serializer(), it) },
            onFailure = { UNAVAILABLE_PART },
        )
        val target = File(directory(), FILE_NAME)
        target.parentFile?.listFiles()?.filter { it != target }?.forEach(File::deleteRecursively)
        withContext(ioDispatcher) { AccountDataArchiveWriter().write(data, target, mapOf(SERVER_ENTRY to serverPart)) }
        return AccountDataArchive(fileName = FILE_NAME, file = target)
    }

    private companion object {
        const val EXPORT_DIRECTORY = "data-exports"
        const val FILE_NAME = "tailormyresume-my-data.zip"
        const val SERVER_ENTRY = "server.json"
        const val UNAVAILABLE_PART =
            """{"unavailable":"The server part of this export could not be fetched. Export again when you are online."}"""
    }
}

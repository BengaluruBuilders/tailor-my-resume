package com.tailormyresume.app.account

import android.content.Context
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.domain.account.ExportedFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheExportedFiles internal constructor(
    private val cacheDirectory: () -> File,
    private val filesDirectory: () -> File,
    private val ioDispatcher: CoroutineDispatcher,
) : ExportedFiles {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    ) : this({ context.cacheDir }, { context.filesDir }, ioDispatcher)

    override suspend fun deleteAll() {
        withContext(ioDispatcher) {
            val cache = cacheDirectory()
            EXPORT_DIRECTORIES.forEach { name -> File(cache, name).deleteRecursively() }
            File(filesDirectory(), "exports").deleteRecursively()
        }
    }

    private companion object {
        val EXPORT_DIRECTORIES = listOf("exports", "data-exports")
    }
}

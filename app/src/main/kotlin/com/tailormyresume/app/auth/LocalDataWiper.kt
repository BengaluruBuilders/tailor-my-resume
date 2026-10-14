package com.tailormyresume.app.auth

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.domain.account.ExportedFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface LocalDataWiper {
    suspend fun wipeAll()

    companion object {
        val None = LocalDataWiper { }
    }
}

@Singleton
class RoomLocalDataWiper @Inject constructor(
    private val database: TmrDatabase,
    private val store: MockStateStore,
    private val exportedFiles: ExportedFiles,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : LocalDataWiper {
    override suspend fun wipeAll() {
        withContext(NonCancellable) {
            withContext(ioDispatcher) { database.clearAllTables() }
            exportedFiles.deleteAll()
            store.clear()
        }
    }
}

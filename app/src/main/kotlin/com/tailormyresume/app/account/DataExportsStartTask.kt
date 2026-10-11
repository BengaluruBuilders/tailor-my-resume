package com.tailormyresume.app.account

import android.content.Context
import com.tailormyresume.app.AppStartTask
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.common.network.di.ApplicationScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

class DataExportsStartTask internal constructor(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long,
) : AppStartTask {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ) : this(File(context.cacheDir, "data-exports"), ioDispatcher, scope, System::currentTimeMillis)

    override fun start() {
        scope.launch {
            withContext(ioDispatcher) {
                val cutoff = nowMillis() - MAX_ARCHIVE_AGE_MILLIS
                directory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach(File::deleteRecursively)
            }
        }
    }
}

private val MAX_ARCHIVE_AGE_MILLIS = 24.hours.inWholeMilliseconds

@Module
@InstallIn(SingletonComponent::class)
interface DataExportsStartTaskBindings {
    @Binds
    @IntoSet
    fun bindDataExportsStartTask(impl: DataExportsStartTask): AppStartTask
}

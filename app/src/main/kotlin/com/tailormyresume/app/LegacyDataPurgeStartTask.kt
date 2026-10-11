package com.tailormyresume.app

import com.tailormyresume.core.common.network.di.ApplicationScope
import com.tailormyresume.core.data.mock.LegacyDataPurge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LegacyDataPurgeStartTask @Inject constructor(
    private val purge: LegacyDataPurge,
    @ApplicationScope private val scope: CoroutineScope,
) : AppStartTask {
    override fun start() {
        scope.launch { runPurge() }
    }

    private suspend fun runPurge() {
        try {
            purge()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LegacyDataPurgeBindings {
    @Binds
    @IntoSet
    fun bindLegacyDataPurgeStartTask(impl: LegacyDataPurgeStartTask): AppStartTask
}

package com.tailormyresume.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.data.mock.DataStoreMockStateStore
import com.tailormyresume.core.data.mock.DelayMockLatency
import com.tailormyresume.core.data.mock.MockLatency
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.ApplicationCleanup
import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.OfflineFirstApplicationRepository
import com.tailormyresume.core.data.repository.OfflineFirstCreditsRepository
import com.tailormyresume.core.data.repository.OfflineFirstProfileRepository
import com.tailormyresume.core.data.repository.PendingAccountWipe
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.data.repository.ResumeSettingsRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.data.repository.StoredApplicationCleanup
import com.tailormyresume.core.data.repository.StoredExportHistoryRepository
import com.tailormyresume.core.data.repository.StoredPendingAccountWipe
import com.tailormyresume.core.data.repository.StoredResumeSettingsRepository
import com.tailormyresume.core.data.repository.StoredSessionRepository
import com.tailormyresume.core.data.repository.StoredTailoringReviewStateRepository
import com.tailormyresume.core.data.repository.TailoringReviewStateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsProfileRepository(
        profileRepository: OfflineFirstProfileRepository,
    ): ProfileRepository

    @Binds
    internal abstract fun bindsApplicationRepository(
        applicationRepository: OfflineFirstApplicationRepository,
    ): ApplicationRepository

    @Binds
    internal abstract fun bindsCreditsRepository(repository: OfflineFirstCreditsRepository): CreditsRepository

    @Binds
    internal abstract fun bindsResumeSettingsRepository(
        repository: StoredResumeSettingsRepository,
    ): ResumeSettingsRepository

    @Binds
    internal abstract fun bindsMockStateStore(store: DataStoreMockStateStore): MockStateStore

    @Binds
    internal abstract fun bindsSessionRepository(repository: StoredSessionRepository): SessionRepository

    @Binds
    internal abstract fun bindsExportHistoryRepository(
        repository: StoredExportHistoryRepository,
    ): ExportHistoryRepository

    @Binds
    internal abstract fun bindsTailoringReviewStateRepository(
        repository: StoredTailoringReviewStateRepository,
    ): TailoringReviewStateRepository

    @Binds
    internal abstract fun bindsApplicationCleanup(cleanup: StoredApplicationCleanup): ApplicationCleanup

    @Binds
    internal abstract fun bindsPendingAccountWipe(wipe: StoredPendingAccountWipe): PendingAccountWipe

    @Binds
    internal abstract fun bindsMockLatency(latency: DelayMockLatency): MockLatency

    companion object {
        @Provides
        fun providesClock(): Clock = Clock.System

        @Provides
        @Singleton
        internal fun providesMockStateDataStore(
            @ApplicationContext context: Context,
            @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(MOCK_STATE_FILE) },
        )

        private const val MOCK_STATE_FILE = "mock_backend"
    }
}

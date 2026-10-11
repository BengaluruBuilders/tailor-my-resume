package com.tailormyresume.core.data.repository

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.data.model.asEntity
import com.tailormyresume.core.data.model.asExternalModel
import com.tailormyresume.core.database.dao.JobApplicationDao
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

internal class OfflineFirstApplicationRepository @Inject constructor(
    private val jobApplicationDao: JobApplicationDao,
    private val clock: Clock,
    private val cleanup: ApplicationCleanup,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ApplicationRepository {

    override fun observeApplications(): Flow<List<JobApplication>> =
        jobApplicationDao.observeApplications()
            .map { entities -> entities.map(JobApplicationEntity::asExternalModel) }
            .flowOn(ioDispatcher)

    override fun observeApplication(id: String): Flow<JobApplication?> =
        jobApplicationDao.observeApplication(id)
            .map { entity -> entity?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun upsertApplication(application: JobApplication) =
        jobApplicationDao.upsertKeepingLegacyColumns(application.asEntity())

    override suspend fun updateStatus(id: String, status: ApplicationStatus) =
        jobApplicationDao.updateStatus(id = id, status = status, updatedAt = clock.now())

    override suspend fun deleteApplication(id: String) {
        clearArtefacts(id)
        deleteApplicationRow(id)
    }

    override suspend fun deleteApplicationRow(id: String) = jobApplicationDao.deleteApplication(id)

    override suspend fun clearArtefacts(id: String) = cleanup.clearFor(id)
}

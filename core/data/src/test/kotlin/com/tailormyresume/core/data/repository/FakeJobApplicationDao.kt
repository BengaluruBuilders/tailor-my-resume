package com.tailormyresume.core.data.repository

import com.tailormyresume.core.database.dao.JobApplicationDao
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.model.ApplicationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakeJobApplicationDao : JobApplicationDao {

    private val entities = MutableStateFlow<Map<String, JobApplicationEntity>>(emptyMap())

    override fun observeApplications(): Flow<List<JobApplicationEntity>> =
        entities.map { saved ->
            saved.values.sortedWith(
                compareByDescending(JobApplicationEntity::updatedAt).thenBy(JobApplicationEntity::id),
            )
        }

    override fun observeApplication(id: String): Flow<JobApplicationEntity?> =
        entities.map { it[id] }

    override suspend fun getApplication(id: String): JobApplicationEntity? = entities.value[id]

    override suspend fun upsertApplication(application: JobApplicationEntity) {
        entities.update { it + (application.id to application) }
    }

    override suspend fun updateStatus(id: String, status: ApplicationStatus, updatedAt: Instant) {
        modify(id) { it.copy(status = status, updatedAt = updatedAt) }
    }

    override suspend fun deleteApplication(id: String) {
        entities.update { it - id }
    }

    private fun modify(id: String, change: (JobApplicationEntity) -> JobApplicationEntity) {
        entities.update { current ->
            val existing = current[id] ?: return@update current
            current + (id to change(existing))
        }
    }
}

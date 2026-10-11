package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {
    fun observeApplications(): Flow<List<JobApplication>>

    fun observeApplication(id: String): Flow<JobApplication?>

    suspend fun upsertApplication(application: JobApplication)

    suspend fun updateStatus(id: String, status: ApplicationStatus)

    suspend fun deleteApplication(id: String)

    suspend fun deleteApplicationRow(id: String) = deleteApplication(id)

    suspend fun clearArtefacts(id: String) = Unit
}

package com.tailormyresume.core.testing.repository

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.JobApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestApplicationRepository : ApplicationRepository {

    private val applicationsFlow = MutableStateFlow<List<JobApplication>>(emptyList())

    override fun observeApplications(): Flow<List<JobApplication>> = applicationsFlow

    override fun observeApplication(id: String): Flow<JobApplication?> =
        applicationsFlow.map { applications -> applications.find { it.id == id } }

    override suspend fun upsertApplication(application: JobApplication) {
        applicationsFlow.update { applications ->
            if (applications.any { it.id == application.id }) {
                applications.map { if (it.id == application.id) application else it }
            } else {
                applications + application
            }
        }
    }

    override suspend fun updateStatus(id: String, status: ApplicationStatus) {
        modify(id) { it.copy(status = status) }
    }

    override suspend fun deleteApplication(id: String) {
        applicationsFlow.update { applications -> applications.filterNot { it.id == id } }
    }

    fun sendApplications(applications: List<JobApplication>) {
        applicationsFlow.value = applications
    }

    private fun modify(id: String, transform: (JobApplication) -> JobApplication) {
        applicationsFlow.update { applications ->
            applications.map { if (it.id == id) transform(it) else it }
        }
    }
}

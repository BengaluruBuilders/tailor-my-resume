package com.tailormyresume.core.domain

import com.tailormyresume.core.data.repository.ApplicationRepository
import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.ApplicationStatus
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.JobApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

class FakeApplicationRepository : ApplicationRepository {
    private val applications = MutableStateFlow<Map<String, JobApplication>>(emptyMap())
    var upsertCount = 0
        private set

    override fun observeApplications(): Flow<List<JobApplication>> = applications.map { it.values.toList() }

    override fun observeApplication(id: String): Flow<JobApplication?> = applications.map { it[id] }

    override suspend fun upsertApplication(application: JobApplication) {
        upsertCount++
        applications.value = applications.value + (application.id to application)
    }

    override suspend fun updateStatus(id: String, status: ApplicationStatus) {
        applications.value[id]?.let { upsertApplication(it.copy(status = status)) }
    }

    override suspend fun deleteApplication(id: String) {
        applications.value = applications.value - id
    }

    fun current(id: String): JobApplication? = applications.value[id]
}

class FakeProfileRepository(initial: CandidateProfile? = null) : ProfileRepository {
    private val profile = MutableStateFlow(initial)
    var saveCount = 0
        private set

    override fun observeProfile(): Flow<CandidateProfile?> = profile

    override suspend fun saveProfile(profile: CandidateProfile) {
        saveCount++
        this.profile.value = profile
    }

    override suspend fun clearProfile() {
        profile.value = null
    }

    fun current(): CandidateProfile? = profile.value
}

class FixedClock(var instant: Instant = Instant.fromEpochSeconds(1_700_000_000)) : Clock {
    override fun now(): Instant = instant
}

class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var counter = 0

    override fun newId(): String = "$prefix-${++counter}"
}

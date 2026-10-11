package com.tailormyresume.core.testing.repository

import com.tailormyresume.core.data.repository.ProfileRepository
import com.tailormyresume.core.model.CandidateProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestProfileRepository : ProfileRepository {

    private val profileFlow = MutableStateFlow<CandidateProfile?>(null)

    override fun observeProfile(): Flow<CandidateProfile?> = profileFlow

    override suspend fun saveProfile(profile: CandidateProfile) {
        profileFlow.value = profile
    }

    override suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean =
        profileFlow.compareAndSet(expected, replacement)

    override suspend fun clearProfile() {
        profileFlow.value = null
    }

    fun sendProfile(profile: CandidateProfile?) {
        profileFlow.value = profile
    }
}

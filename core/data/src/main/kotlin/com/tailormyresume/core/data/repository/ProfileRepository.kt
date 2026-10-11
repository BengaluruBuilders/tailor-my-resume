package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.CandidateProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<CandidateProfile?>

    suspend fun saveProfile(profile: CandidateProfile)

    suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean

    suspend fun clearProfile()
}

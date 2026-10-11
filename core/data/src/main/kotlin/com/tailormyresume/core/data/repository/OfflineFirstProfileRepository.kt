package com.tailormyresume.core.data.repository

import com.tailormyresume.core.common.network.Dispatcher
import com.tailormyresume.core.common.network.TmrDispatchers.IO
import com.tailormyresume.core.data.model.asEntity
import com.tailormyresume.core.data.model.asEntryEntities
import com.tailormyresume.core.data.model.asExternalModel
import com.tailormyresume.core.database.dao.ProfileDao
import com.tailormyresume.core.model.CandidateProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineFirstProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    override fun observeProfile(): Flow<CandidateProfile?> =
        profileDao.observePopulatedProfile()
            .map { populated -> populated?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun saveProfile(profile: CandidateProfile) {
        requireUniqueIds(profile)
        profileDao.replaceProfile(
            profile = profile.asEntity(),
            entries = profile.asEntryEntities(),
        )
    }

    override suspend fun saveProfileIfUnchanged(expected: CandidateProfile, replacement: CandidateProfile): Boolean {
        requireUniqueIds(replacement)
        return profileDao.replaceProfileIfUnchanged(
            expected.asEntity(),
            expected.asEntryEntities(),
            replacement.asEntity(),
            replacement.asEntryEntities(),
        )
    }

    private fun requireUniqueIds(profile: CandidateProfile) {
        val duplicateIds = profile.entries
            .groupingBy { it.id }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
        require(duplicateIds.isEmpty()) {
            "Profile entries must have unique ids. Duplicate ids: $duplicateIds"
        }
    }

    override suspend fun clearProfile() = profileDao.deleteProfile()
}

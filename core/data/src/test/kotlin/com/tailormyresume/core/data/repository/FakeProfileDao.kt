package com.tailormyresume.core.data.repository

import com.tailormyresume.core.database.dao.ProfileDao
import com.tailormyresume.core.database.model.PopulatedProfile
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.database.model.ProfileEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class FakeProfileDao : ProfileDao() {

    private val profile = MutableStateFlow<ProfileEntity?>(null)
    private val entries = MutableStateFlow<List<ProfileEntryEntity>>(emptyList())

    override fun observePopulatedProfile(): Flow<PopulatedProfile?> =
        combine(profile, entries) { savedProfile, savedEntries ->
            savedProfile?.let { PopulatedProfile(it, savedEntries) }
        }

    override suspend fun readProfile(): ProfileEntity? = profile.value

    override suspend fun readEntries(): List<ProfileEntryEntity> = entries.value.sortedBy { it.position }

    override suspend fun upsertProfile(profile: ProfileEntity) {
        this.profile.value = profile
    }

    override suspend fun upsertEntries(entries: List<ProfileEntryEntity>) {
        this.entries.update { current ->
            val incomingIds = entries.map(ProfileEntryEntity::id).toSet()
            current.filterNot { it.id in incomingIds } + entries
        }
    }

    override suspend fun deleteEntries() {
        entries.value = emptyList()
    }

    override suspend fun deleteProfile() {
        profile.value = null
        entries.value = emptyList()
    }
}

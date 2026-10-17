package com.tailormyresume.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tailormyresume.core.database.json.EvidenceBulletDto
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource

@kotlinx.serialization.Serializable
@Entity(
    tableName = "profile_entries",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profileId"])],
)
data class ProfileEntryEntity(
    @PrimaryKey
    val id: String,
    val profileId: Int = ProfileEntity.SINGLETON_ID,
    val position: Int,
    val category: EntryCategory,
    val title: String,
    val organization: String,
    val startDate: String,
    val endDate: String,
    val bullets: List<EvidenceBulletDto>,
    val source: FactSource,
    val isConfirmed: Boolean,
)

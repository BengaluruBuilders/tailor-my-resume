package com.tailormyresume.core.network.dto

import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.MatchStatus
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class BulletDto(val id: String, val text: String)

@Serializable
data class FactEntryDto(
    val id: String,
    val category: EntryCategory,
    val title: String,
    val organization: String,
    val startDate: String,
    val endDate: String,
    val source: FactSource,
    val bullets: List<BulletDto>,
)

@Serializable
data class SummaryFactDto(val id: String, val text: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProfileFactsDto(
    val skills: List<String>,
    val entries: List<FactEntryDto>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val userStatedSkills: List<String> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) val summary: SummaryFactDto? = null,
)

@Serializable
data class RequirementDto(
    val id: String,
    val text: String,
    val type: RequirementType,
    val priority: RequirementPriority,
    val keywords: List<String>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class JobDto(
    val title: String,
    val company: String,
    val requirements: List<RequirementDto>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val location: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MatchDto(
    val requirementId: String,
    val status: MatchStatus,
    val evidenceIds: List<String>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val reason: String? = null,
)

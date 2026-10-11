package com.tailormyresume.core.network.dto

import com.tailormyresume.core.model.EntryCategory
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class ResumeParseRequest(val text: String)

@Serializable
data class ParsedBulletDto(val ref: String, val text: String)

internal object LinkKindSerializer : LenientEnumSerializer<LinkKind>(LinkKind.entries.toTypedArray(), LinkKind.UNKNOWN, "LinkKind")

@Serializable(with = LinkKindSerializer::class)
enum class LinkKind { LINKEDIN, PORTFOLIO, OTHER, UNKNOWN }

@Serializable
data class ParsedLinkDto(val kind: LinkKind, val url: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ParsedEntryDto(
    val ref: String,
    val category: EntryCategory,
    val title: String,
    val organization: String?,
    val startDate: String?,
    val endDate: String?,
    val bullets: List<ParsedBulletDto>,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val current: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ParsedProfileDto(
    val fullName: String?,
    val email: String?,
    val phone: String?,
    val headline: String?,
    val skills: List<String>,
    val entries: List<ParsedEntryDto>,
    val summary: String? = null,
    val location: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val links: List<ParsedLinkDto> = emptyList(),
)

@Serializable
enum class SensitiveField { DATE_OF_BIRTH, PHOTO, RELIGION, CASTE, MARITAL_STATUS, GENDER }

@Serializable
data class ResumeParseResponse(
    val generationId: String,
    val profile: ParsedProfileDto,
    val droppedSensitive: List<SensitiveField>,
)

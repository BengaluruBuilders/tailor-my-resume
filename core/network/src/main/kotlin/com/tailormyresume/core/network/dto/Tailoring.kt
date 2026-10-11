package com.tailormyresume.core.network.dto

import com.tailormyresume.core.model.EditType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
enum class AnswerChoice { YES_REGULARLY, A_FEW_TIMES, NOT_YET, SKIPPED }

@Serializable
data class AnswerDto(
    val requirementId: String,
    val choice: AnswerChoice,
    val detail: String?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TailoringStartRequest(
    val requestId: String,
    val applicationId: String,
    val job: JobDto,
    val matches: List<MatchDto>,
    val profile: ProfileFactsDto,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val answer: AnswerDto? = null,
)

internal object TailoringStatusSerializer :
    LenientEnumSerializer<TailoringStatus>(
        TailoringStatus.entries.toTypedArray(),
        TailoringStatus.UNKNOWN,
        "TailoringStatus",
    )

@Serializable(with = TailoringStatusSerializer::class)
enum class TailoringStatus { RUNNING, SUCCEEDED, FAILED, UNKNOWN }

internal object EditTypeSerializer :
    LenientEnumSerializer<EditType>(EditType.entries.toTypedArray(), EditType.UNKNOWN, "EditType")

internal object TailoringFailureCodeSerializer :
    LenientEnumSerializer<TailoringFailureCode>(
        TailoringFailureCode.entries.toTypedArray(),
        TailoringFailureCode.UNKNOWN,
        "TailoringFailureCode",
    )

@Serializable(with = TailoringFailureCodeSerializer::class)
enum class TailoringFailureCode { AI_PROVIDER_ERROR, QUOTA_EXCEEDED, BUDGET_EXCEEDED, INTERRUPTED, UNKNOWN }

internal object BulletVerificationSerializer :
    LenientEnumSerializer<BulletVerification>(
        BulletVerification.entries.toTypedArray(),
        BulletVerification.UNKNOWN,
        "BulletVerification",
    )

@Serializable(with = BulletVerificationSerializer::class)
enum class BulletVerification { PASSED, REPAIRED, REVERTED, UNCHANGED, UNKNOWN }

@Serializable
data class TailoredBulletDto(
    val id: String,
    val entryId: String,
    val sourceIds: List<String>,
    val proposedText: String,
    val editTypes: List<
        @Serializable(with = EditTypeSerializer::class)
        EditType,
        >,
    val keywordsUsed: List<String>,
    val verification: BulletVerification,
)

@Serializable
data class SummaryResultDto(val text: String, val sourceIds: List<String>, val verification: BulletVerification)

@Serializable
data class SkillsResultDto(val ordered: List<String> = emptyList(), val added: List<String> = emptyList())

@Serializable
data class TailoringResultDto(
    val generationId: String,
    val bullets: List<TailoredBulletDto>,
    val summary: SummaryResultDto? = null,
    val skills: SkillsResultDto? = null,
)

@Serializable
data class TailoringDto(
    val id: String,
    val status: TailoringStatus,
    val result: TailoringResultDto? = null,
    val failureCode: TailoringFailureCode? = null,
)

@Serializable
data class TailoringResponse(val tailoring: TailoringDto)

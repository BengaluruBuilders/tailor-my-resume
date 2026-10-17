package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReferralOfferDto(
    val status: String = "UNAVAILABLE",
    val reason: String = "POLICY_UNAVAILABLE",
    val version: String? = null,
    val effectiveAt: String? = null,
    val expiresAt: String? = null,
    val rewardAmount: Int? = null,
    val continuityMode: String = "NONE",
    val codeIssuance: String = "UNAVAILABLE",
    val attachmentEligibility: String = "UNAVAILABLE",
    val deadline: String? = null,
)

@Serializable
data class ReferralCodeDto(val value: String, val createdAt: String)

@Serializable
data class ReferralRewardDto(val recipient: String, val amount: Int, val status: String, val policyVersion: String)

@Serializable
data class ReferralAttachmentDto(val id: String, val status: String, val source: String, val attachedAt: String, val reward: ReferralRewardDto)

@Serializable
data class ReferralStatsDto(val creditedCount: Long, val earnedCredits: Long)

@Serializable
data class ReferralReadResponse(val code: ReferralCodeDto?, val shareUrl: String?, val offer: ReferralOfferDto, val attachment: ReferralAttachmentDto?, val stats: ReferralStatsDto)

@Serializable
data class ReferralCodeRequest(val requestId: String)

@Serializable
data class ReferralCodeResponse(val code: ReferralCodeDto, val shareUrl: String?, val offer: ReferralOfferDto, val replayed: Boolean)

@Serializable
data class ReferralAttachRequest(val requestId: String, val code: String, val source: String, val policyVersion: String, val continuity: TrialContinuityDto?, val challengeId: String, val integrityToken: String)

@Serializable
data class ReferralAttachResponse(val attachment: ReferralAttachmentDto, val reward: ReferralRewardDto, val replayed: Boolean)

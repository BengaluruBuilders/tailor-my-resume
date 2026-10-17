package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreditPolicyResponse(val policy: CreditPolicyDto)

@Serializable
data class CreditPolicyDto(
    val version: String?,
    val effectiveAt: String?,
    val expiresAt: String?,
    val accountMode: String,
    val guestTrial: GuestTrialPolicyDto,
)

@Serializable
data class GuestTrialPolicyDto(
    val status: String,
    val reason: String,
    val amount: Int?,
    val operation: String,
    val integrity: String,
    val continuity: String,
    val bindingVersion: Int,
)

@Serializable
data class GrantChallengeRequest(
    val operation: String,
    val requestId: String,
    val policyVersion: String,
    val payloadDigest: String,
)

@Serializable
data class GrantChallengeResponse(
    val challengeId: String,
    val challenge: String,
    val expiresAt: String,
    val bindingVersion: Int,
    val requestHash: String,
)

@Serializable
data class TrialContinuityDto(val type: String, val value: String)

@Serializable
data class GuestInitializeRequest(
    val requestId: String,
    val policyVersion: String,
    val continuity: TrialContinuityDto,
    val challengeId: String,
    val integrityToken: String,
)

@Serializable
data class GuestInitializeResponse(
    val wallet: WalletDto,
    val grants: List<TrialGrantDto>,
    val trial: TrialOutcomeDto,
    val replayed: Boolean,
)

@Serializable
data class TrialGrantDto(val id: String, val kind: String, val amount: Int, val createdAt: String, val policyVersion: String)

@Serializable
data class TrialOutcomeDto(val status: String, val reason: String)

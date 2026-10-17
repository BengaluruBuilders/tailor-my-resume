package com.tailormyresume.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpgradeIntentRequest(val requestId: String, val proof: String)

@Serializable
data class UpgradeIntentDto(val intentId: String, val createdAt: String, val expiresAt: String, val state: String)

@Serializable
data class UpgradeProofDto(val intentId: String, val proof: String)

@Serializable
data class UpgradeStatusResponse(val intent: UpgradeStatusDto, val completion: UpgradeCompletionDto?)

@Serializable
data class UpgradeStatusDto(val id: String, val state: String, val createdAt: String, val expiresAt: String)

@Serializable
data class UpgradeCompletionDto(
    val kind: String,
    val disposition: String? = null,
    val creditsTransferred: Int = 0,
    val purchasesTransferred: Boolean = false,
    val runningJobsTransferred: Boolean = false,
)

@Serializable
data class UpgradeCompleteRequest(val requestId: String, val intentId: String, val proof: String, val disposition: String)

@Serializable
data class UpgradeCompleteResponse(val wallet: WalletDto, val completion: UpgradeCompletionDto, val replayed: Boolean)

@Serializable
data class ActivationObservationRequest(val requestId: String)

@Serializable
data class ActivationChallengeRequest(
    val operation: String,
    val requestId: String,
    val policyVersion: String?,
    val trialPolicyVersion: String?,
    val payloadDigest: String,
)

@Serializable
data class GoogleActivationRequest(
    val requestId: String,
    val policyVersion: String?,
    val trialPolicyVersion: String?,
    val path: String,
    val upgradeIntent: UpgradeProofDto?,
    val continuity: TrialContinuityDto?,
    val challengeId: String,
    val integrityToken: String,
)

@Serializable
data class ActivationFactDto(
    val source: String,
    val eligibility: String,
    val reason: String,
    val eligibilityAnchorAt: String?,
    val firstGoogleObservedAt: String?,
    val referralDeadline: String?,
)

@Serializable
data class ReferralEligibilityDto(val status: String, val deadline: String?)

@Serializable
data class ActivationReadResponse(
    val wallet: WalletDto,
    val activation: ActivationFactDto?,
    val grants: List<TrialGrantDto>,
    val referralEligibility: ReferralEligibilityDto,
)

@Serializable
data class GoogleActivationResponse(
    val wallet: WalletDto,
    val activation: ActivationFactDto?,
    val grants: List<TrialGrantDto>,
    val signupBonus: TrialOutcomeDto,
    val baseTrial: TrialOutcomeDto,
    val referralEligibility: ReferralEligibilityDto,
    val replayed: Boolean,
)

@Serializable
data class SignupBonusPolicyDto(
    val status: String,
    val reason: String? = null,
    val amount: Int? = null,
    val version: String? = null,
    val grantPolicyVersion: String? = null,
    val effectiveAt: String? = null,
    val expiresAt: String? = null,
    val operation: String? = null,
    val bindingVersion: Int? = null,
    val integrity: String? = null,
    val referralWindowSeconds: Long? = null,
    val baseTrial: BaseTrialPolicyDto? = null,
)

@Serializable
data class BaseTrialPolicyDto(
    val status: String,
    val reason: String,
    val amount: Int?,
    val version: String?,
    val grantPolicyVersion: String?,
)

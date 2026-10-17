package com.tailormyresume.core.network

import com.tailormyresume.core.network.dto.ActivationChallengeRequest
import com.tailormyresume.core.network.dto.ActivationObservationRequest
import com.tailormyresume.core.network.dto.ActivationReadResponse
import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.dto.AnalysisResponse
import com.tailormyresume.core.network.dto.ContentReportRequest
import com.tailormyresume.core.network.dto.ContentReportResponse
import com.tailormyresume.core.network.dto.CreditPolicyResponse
import com.tailormyresume.core.network.dto.CreditsResponse
import com.tailormyresume.core.network.dto.DeletionResponse
import com.tailormyresume.core.network.dto.GoogleActivationRequest
import com.tailormyresume.core.network.dto.GoogleActivationResponse
import com.tailormyresume.core.network.dto.GrantChallengeRequest
import com.tailormyresume.core.network.dto.GrantChallengeResponse
import com.tailormyresume.core.network.dto.GuestInitializeRequest
import com.tailormyresume.core.network.dto.GuestInitializeResponse
import com.tailormyresume.core.network.dto.JobImportRequest
import com.tailormyresume.core.network.dto.JobImportResponse
import com.tailormyresume.core.network.dto.MeResponse
import com.tailormyresume.core.network.dto.PacksResponse
import com.tailormyresume.core.network.dto.PurchaseRequest
import com.tailormyresume.core.network.dto.PurchaseResponse
import com.tailormyresume.core.network.dto.PurchasesResponse
import com.tailormyresume.core.network.dto.ResumeParseRequest
import com.tailormyresume.core.network.dto.ResumeParseResponse
import com.tailormyresume.core.network.dto.ServerExportResponse
import com.tailormyresume.core.network.dto.TailoringResponse
import com.tailormyresume.core.network.dto.TailoringStartRequest
import com.tailormyresume.core.network.dto.UpgradeCompleteRequest
import com.tailormyresume.core.network.dto.UpgradeCompleteResponse
import com.tailormyresume.core.network.dto.UpgradeIntentDto
import com.tailormyresume.core.network.dto.UpgradeIntentRequest
import com.tailormyresume.core.network.dto.UpgradeProofDto
import com.tailormyresume.core.network.dto.UpgradeStatusResponse
import com.tailormyresume.core.network.dto.WalletResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TailorMyResumeApi {
    @GET("v1/tailormyresume/referrals/me")
    suspend fun referrals(): com.tailormyresume.core.network.dto.ReferralReadResponse

    @POST("v1/tailormyresume/referrals/code")
    suspend fun referralCode(@Body body: com.tailormyresume.core.network.dto.ReferralCodeRequest): com.tailormyresume.core.network.dto.ReferralCodeResponse

    @POST("v1/tailormyresume/referrals/challenges")
    suspend fun referralChallenge(@Body body: GrantChallengeRequest): GrantChallengeResponse

    @POST("v1/tailormyresume/referrals/attach")
    suspend fun attachReferral(@Body body: com.tailormyresume.core.network.dto.ReferralAttachRequest): retrofit2.Response<com.tailormyresume.core.network.dto.ReferralAttachResponse>

    @POST("v1/tailormyresume/account/upgrade-intents")
    suspend fun prepareUpgrade(@Body body: UpgradeIntentRequest): UpgradeIntentDto

    @POST("v1/tailormyresume/account/upgrade-intents/status")
    suspend fun upgradeStatus(@Body body: UpgradeProofDto): UpgradeStatusResponse

    @POST("v1/tailormyresume/account/upgrade-intents/complete")
    suspend fun completeUpgrade(@Body body: UpgradeCompleteRequest): UpgradeCompleteResponse

    @POST("v1/tailormyresume/account/activation-observations")
    suspend fun observeActivation(@Body body: ActivationObservationRequest): ActivationReadResponse

    @GET("v1/tailormyresume/account/activation")
    suspend fun activation(): ActivationReadResponse

    @POST("v1/tailormyresume/account/activation-challenges")
    suspend fun activationChallenge(@Body body: ActivationChallengeRequest): GrantChallengeResponse

    @POST("v1/tailormyresume/account/activate")
    suspend fun activateGoogle(@Body body: GoogleActivationRequest): retrofit2.Response<GoogleActivationResponse>

    @GET("v1/tailormyresume/credits/policy")
    suspend fun creditPolicy(): CreditPolicyResponse

    @POST("v1/tailormyresume/credits/grant-challenges")
    suspend fun grantChallenge(@Body body: GrantChallengeRequest): GrantChallengeResponse

    @POST("v1/tailormyresume/credits/initialize")
    suspend fun initializeGuest(@Body body: GuestInitializeRequest): GuestInitializeResponse

    @GET("v1/me")
    suspend fun me(): MeResponse

    @DELETE("v1/me")
    suspend fun deleteMe(): DeletionResponse

    @POST("v1/tailormyresume/resume/parse")
    suspend fun parseResume(@Body body: ResumeParseRequest): ResumeParseResponse

    @POST("v1/tailormyresume/analyses")
    suspend fun analyse(@Body body: AnalysisRequest): AnalysisResponse

    @POST("v1/tailormyresume/job-imports")
    suspend fun jobImport(@Body body: JobImportRequest): JobImportResponse

    @POST("v1/tailormyresume/tailorings")
    suspend fun startTailoring(@Body body: TailoringStartRequest): TailoringResponse

    @GET("v1/tailormyresume/tailorings/{id}")
    suspend fun tailoring(@Path("id") id: String): TailoringResponse

    @GET("v1/tailormyresume/wallet")
    suspend fun wallet(): WalletResponse

    @GET("v1/tailormyresume/credits")
    suspend fun credits(): CreditsResponse

    @GET("v1/tailormyresume/packs")
    suspend fun packs(): PacksResponse

    @POST("v1/tailormyresume/purchases")
    suspend fun purchase(@Body body: PurchaseRequest): PurchaseResponse

    @GET("v1/tailormyresume/purchases")
    suspend fun purchases(): PurchasesResponse

    @POST("v1/tailormyresume/content-reports")
    suspend fun reportContent(@Body body: ContentReportRequest): ContentReportResponse

    @GET("v1/tailormyresume/me/export")
    suspend fun exportMe(): ServerExportResponse
}

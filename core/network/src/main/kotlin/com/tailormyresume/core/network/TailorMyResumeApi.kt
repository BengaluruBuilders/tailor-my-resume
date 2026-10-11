package com.tailormyresume.core.network

import com.tailormyresume.core.network.dto.AnalysisRequest
import com.tailormyresume.core.network.dto.AnalysisResponse
import com.tailormyresume.core.network.dto.ContentReportRequest
import com.tailormyresume.core.network.dto.ContentReportResponse
import com.tailormyresume.core.network.dto.CreditsResponse
import com.tailormyresume.core.network.dto.DeletionResponse
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
import com.tailormyresume.core.network.dto.WalletResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TailorMyResumeApi {
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

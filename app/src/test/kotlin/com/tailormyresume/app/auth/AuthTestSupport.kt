package com.tailormyresume.app.auth

import com.tailormyresume.core.domain.GapMatcher
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.KeywordCoverage
import com.tailormyresume.core.network.IdTokenProvider
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.network.tailormyresumeApi
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.network.tailormyresumeOkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

internal const val SECRET_TOKEN = "SECRET-ID-TOKEN"
internal const val USER_EMAIL = "priya.secret@example.com"

internal val completeConfig = FirebaseConfig(apiKey = "k", appId = "a", projectId = "p", webClientId = "w")

internal fun MockWebServer.api(): TailorMyResumeApi = tailormyresumeApi(
    TailorMyResumeApiConfig(url("/").toString().trimEnd('/')),
    tailormyresumeOkHttpClient(
        object : IdTokenProvider {
            override fun idToken(forceRefresh: Boolean) = SECRET_TOKEN
        },
    ),
    tailormyresumeJson(),
)

internal fun jsonResponse(code: Int, body: String) =
    MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

internal fun errorResponse(code: Int, errorCode: String) =
    jsonResponse(code, """{"error":{"code":"$errorCode","message":"m"}}""")

internal const val ME_BODY = """{"user":{"id":"u1","createdAt":"2026-10-08T00:00:00Z"}}"""

internal class ScriptedCredentials(var outcome: AuthOutcome<String> = AuthOutcome.Success(SECRET_TOKEN)) :
    GoogleCredentialSource {
    var requests = 0
    var cleared = false

    override suspend fun idToken(): AuthOutcome<String> {
        requests++
        return outcome
    }

    override suspend fun clearState() {
        cleared = true
    }
}

internal class ScriptedFirebase(
    var outcome: AuthOutcome<FirebaseUser> = AuthOutcome.Success(FirebaseUser("uid-1", "Priya", USER_EMAIL)),
) : FirebaseSessionClient {
    var signedOut = false

    override suspend fun signIn(googleIdToken: String): AuthOutcome<FirebaseUser> = outcome

    override fun signOut() {
        signedOut = true
    }
}

internal object NoMatcher : GapMatcher {
    override fun match(profile: CandidateProfile, job: JobDescription) = GapAnalysis(emptyList(), KeywordCoverage(1, 2))
}

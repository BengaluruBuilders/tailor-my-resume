package com.tailormyresume.core.data.repository

import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.mock.observeValue
import com.tailormyresume.core.data.mock.writeValue
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.SignInAccount
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StoredSessionRepository @Inject constructor(
    private val store: MockStateStore,
) : SessionRepository {

    override fun observeAccount(): Flow<SignInAccount?> =
        store.observeValue(ACCOUNT_KEY, AccountDto.serializer()).map { dto -> dto?.toModel() }

    override fun observeOnboardingComplete(): Flow<Boolean> =
        store.observe(ONBOARDING_KEY).map { value -> value == COMPLETE }

    override fun observeKeptJobDescription(): Flow<KeptJobDescription?> =
        store.observeValue(KEPT_JOB_KEY, KeptJobDto.serializer()).map { dto -> dto?.toModel() }

    override suspend fun saveAccount(account: SignInAccount) {
        store.writeValue(ACCOUNT_KEY, AccountDto.serializer(), AccountDto(account.id, account.displayName, account.email))
    }

    override suspend fun lastAccountId(): String? = store.read(LAST_ACCOUNT_KEY)

    override suspend fun saveLastAccountId(id: String) {
        store.write(LAST_ACCOUNT_KEY, id)
    }

    override suspend fun markOnboardingComplete() {
        store.write(ONBOARDING_KEY, COMPLETE)
    }

    override suspend fun keepJobDescription(job: KeptJobDescription) {
        store.writeValue(KEPT_JOB_KEY, KeptJobDto.serializer(), KeptJobDto(job.text, job.company, job.role, job.companyIsPrefill, job.roleIsPrefill))
    }

    override suspend fun clearKeptJobDescription() {
        store.remove(KEPT_JOB_KEY)
    }

    override suspend fun signOut() = removeAll(ACCOUNT_KEY, KEPT_JOB_KEY, LEGACY_CONSENT_KEY, LEGACY_CAREER_STAGE_KEY)

    override suspend fun clear() =
        removeAll(ACCOUNT_KEY, LAST_ACCOUNT_KEY, ONBOARDING_KEY, KEPT_JOB_KEY, LEGACY_CONSENT_KEY, LEGACY_CAREER_STAGE_KEY)

    private suspend fun removeAll(vararg keys: String) = withContext(NonCancellable) {
        keys.forEach { key -> store.remove(key) }
    }

    private companion object {
        const val ACCOUNT_KEY = "session.account"
        const val LAST_ACCOUNT_KEY = "session.lastAccountId"
        const val LEGACY_CONSENT_KEY = "session.consent"
        const val ONBOARDING_KEY = "session.onboardingComplete"
        const val KEPT_JOB_KEY = "session.keptJobDescription"
        const val LEGACY_CAREER_STAGE_KEY = "session.careerStage"
        const val COMPLETE = "true"
    }
}

@Serializable
private data class AccountDto(val id: String, val displayName: String, val email: String) {
    fun toModel() = SignInAccount(id = id, displayName = displayName, email = email)
}

@Serializable
private data class KeptJobDto(
    val text: String,
    val company: String,
    val role: String,
    val companyIsPrefill: Boolean = false,
    val roleIsPrefill: Boolean = false,
) {
    fun toModel() = KeptJobDescription(
        text = text,
        company = company,
        role = role,
        companyIsPrefill = companyIsPrefill,
        roleIsPrefill = roleIsPrefill,
    )
}

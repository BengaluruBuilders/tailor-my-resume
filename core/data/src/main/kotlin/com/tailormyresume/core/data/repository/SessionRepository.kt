package com.tailormyresume.core.data.repository

import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.SignInAccount
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAccount(): Flow<SignInAccount?>

    fun observeOnboardingComplete(): Flow<Boolean>

    fun observeKeptJobDescription(): Flow<KeptJobDescription?>

    suspend fun saveAccount(account: SignInAccount)

    suspend fun lastAccountId(): String?

    suspend fun saveLastAccountId(id: String)

    suspend fun markOnboardingComplete()

    suspend fun keepJobDescription(job: KeptJobDescription)

    suspend fun clearKeptJobDescription()

    suspend fun signOut()

    suspend fun clear()
}

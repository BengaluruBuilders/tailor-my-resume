package com.tailormyresume.app.billing

import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.apiResult
import com.tailormyresume.core.network.dto.WalletDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletSource @Inject constructor(
    private val api: TailorMyResumeApi,
    private val uids: FirebaseUidProvider = FirebaseUidProvider { null },
) {
    private class Held(val uid: String?, val wallet: WalletDto)

    private val state = MutableStateFlow<Held?>(null)
    private val lock = Any()
    private var generation = 0
    private var owner: String? = uids.uid()

    val wallet: Flow<WalletDto?> get() = state.map { held -> held.ownedNow() }

    val cached: WalletDto? get() = state.value.ownedNow()

    suspend fun refresh(): WalletDto {
        val started = generation()
        val answer = apiResult { api.wallet().wallet }.getOrThrow()
        check(update(answer, started)) { "The wallet answer arrived after sign-out" }
        return answer
    }

    suspend fun refreshOrCached(): WalletDto? {
        val started = generation()
        val answer = apiResult { api.wallet().wallet }.getOrNull()
        return if (answer != null && update(answer, started)) answer else cached
    }

    fun generation(): Int = synchronized(lock) {
        dropIfAccountChanged()
        generation
    }

    fun update(wallet: WalletDto, started: Int): Boolean = synchronized(lock) {
        dropIfAccountChanged()
        (started == generation).also { current -> if (current) state.value = Held(uids.uid(), wallet) }
    }

    fun clear() {
        synchronized(lock) {
            generation++
            state.value = null
        }
    }

    private fun dropIfAccountChanged() {
        val current = uids.uid()
        if (owner != current) {
            owner = current
            generation++
            state.value = null
        }
    }

    private fun Held?.ownedNow(): WalletDto? = this?.takeIf { it.uid == uids.uid() }?.wallet
}

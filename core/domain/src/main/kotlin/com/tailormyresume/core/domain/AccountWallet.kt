package com.tailormyresume.core.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface AccountWallet {
    suspend fun entitlement(): PurchaseEntitlement

    fun observeEntitlement(): Flow<PurchaseEntitlement> = flow { emit(entitlement()) }

    suspend fun purchaseHistory(): List<PurchaseRecord> = emptyList()

    fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = flow { emit(purchaseHistory()) }

    suspend fun clearCredits(): PurchaseEntitlement
}

sealed interface PurchaseHistoryState {
    data class Known(val records: List<PurchaseRecord>) : PurchaseHistoryState

    data object Unknown : PurchaseHistoryState
}

fun AccountWallet.observePurchaseHistoryState(): Flow<PurchaseHistoryState> =
    observePurchaseHistory()
        .map<List<PurchaseRecord>, PurchaseHistoryState> { records -> PurchaseHistoryState.Known(records) }
        .catch { failure ->
            if (failure is CancellationException) throw failure
            emit(PurchaseHistoryState.Unknown)
        }

package com.tailormyresume.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tailormyresume.core.database.model.CreditLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditLedgerDao {
    @Query("SELECT * FROM credit_ledger ORDER BY createdAt DESC, id DESC")
    fun observeLedger(): Flow<List<CreditLedgerEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_ledger WHERE owner IS NULL")
    fun observeBalance(): Flow<Int>

    @Query("SELECT * FROM credit_ledger WHERE owner = :owner AND environment = :environment ORDER BY createdAt DESC, id DESC LIMIT 100")
    suspend fun ownedEntries(owner: String, environment: String): List<CreditLedgerEntity>

    @Query("DELETE FROM credit_ledger WHERE owner = :owner AND environment = :environment")
    suspend fun clearOwned(owner: String, environment: String)

    @androidx.room.Transaction
    suspend fun replaceOwned(owner: String, environment: String, entries: List<CreditLedgerEntity>) {
        clearOwned(owner, environment)
        entries.forEach { insert(it) }
    }

    @Insert
    suspend fun insert(entry: CreditLedgerEntity)

    @Query("DELETE FROM credit_ledger")
    suspend fun clear()

    @Query("DELETE FROM credit_ledger WHERE applicationId = :applicationId AND owner IS NULL")
    suspend fun deleteForApplication(applicationId: String)
}

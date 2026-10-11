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

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_ledger")
    fun observeBalance(): Flow<Int>

    @Insert
    suspend fun insert(entry: CreditLedgerEntity)

    @Query("DELETE FROM credit_ledger")
    suspend fun clear()

    @Query("DELETE FROM credit_ledger WHERE applicationId = :applicationId")
    suspend fun deleteForApplication(applicationId: String)
}

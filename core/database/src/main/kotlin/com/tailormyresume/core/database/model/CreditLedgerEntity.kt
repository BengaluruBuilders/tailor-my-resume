package com.tailormyresume.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tailormyresume.core.model.CreditLedgerKind
import kotlin.time.Instant

@Entity(tableName = "credit_ledger", indices = [androidx.room.Index(value = ["owner", "environment", "serverId"], unique = true)])
data class CreditLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: CreditLedgerKind,
    val amount: Int,
    val applicationId: String?,
    val productId: String?,
    val createdAt: Instant,
    val serverId: String? = null,
    val owner: String? = null,
    val environment: String? = null,
)

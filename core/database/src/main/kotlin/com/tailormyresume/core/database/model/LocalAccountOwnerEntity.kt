package com.tailormyresume.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_account_owner")
data class LocalAccountOwnerEntity(@PrimaryKey val id: Int = 1, val owner: String, val environment: String)

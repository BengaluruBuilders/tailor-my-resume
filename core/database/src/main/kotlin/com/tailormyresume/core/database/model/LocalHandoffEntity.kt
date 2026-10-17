package com.tailormyresume.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_handoffs")
data class LocalHandoffEntity(@PrimaryKey val id: String, val destination: String)

package com.tailormyresume.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tailormyresume.core.database.model.LocalHandoffEntity

@Dao
interface LocalHandoffDao {
    @Query("SELECT * FROM local_handoffs WHERE id = :id")
    suspend fun find(id: String): LocalHandoffEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: LocalHandoffEntity)
}

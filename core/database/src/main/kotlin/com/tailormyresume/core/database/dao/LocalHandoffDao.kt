package com.tailormyresume.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tailormyresume.core.database.model.LocalHandoffEntity
import com.tailormyresume.core.database.model.LocalAccountOwnerEntity
import androidx.room.Upsert

@Dao
interface LocalHandoffDao {
    @Query("SELECT * FROM local_handoffs WHERE id = :id")
    suspend fun find(id: String): LocalHandoffEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: LocalHandoffEntity)

    @Query("SELECT * FROM local_account_owner WHERE id = 1")
    suspend fun owner(): LocalAccountOwnerEntity?

    @Upsert
    suspend fun setOwner(value: LocalAccountOwnerEntity)

    @Query("DELETE FROM local_handoffs WHERE source = :owner OR destination = :owner")
    suspend fun erase(owner: String)
}

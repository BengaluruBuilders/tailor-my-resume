package com.tailormyresume.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tailormyresume.core.database.dao.CreditLedgerDao
import com.tailormyresume.core.database.dao.JobApplicationDao
import com.tailormyresume.core.database.dao.ProfileDao
import com.tailormyresume.core.database.model.CreditLedgerEntity
import com.tailormyresume.core.database.model.JobApplicationEntity
import com.tailormyresume.core.database.model.ProfileEntity
import com.tailormyresume.core.database.model.ProfileEntryEntity
import com.tailormyresume.core.database.util.InstantConverter
import com.tailormyresume.core.database.util.JsonConverters

@Database(
    entities = [
        ProfileEntity::class,
        ProfileEntryEntity::class,
        JobApplicationEntity::class,
        CreditLedgerEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
    JsonConverters::class,
)
abstract class TmrDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun creditLedgerDao(): CreditLedgerDao
}

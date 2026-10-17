package com.tailormyresume.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_handoffs` (`id` TEXT NOT NULL, `destination` TEXT NOT NULL, `source` TEXT NOT NULL, `mapping` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_account_owner` (`id` INTEGER NOT NULL, `owner` TEXT NOT NULL, `environment` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}

package com.tailormyresume.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE credit_ledger ADD COLUMN serverId TEXT")
        db.execSQL("ALTER TABLE credit_ledger ADD COLUMN owner TEXT")
        db.execSQL("ALTER TABLE credit_ledger ADD COLUMN environment TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credit_ledger_owner_environment_serverId ON credit_ledger (owner, environment, serverId)")
    }
}

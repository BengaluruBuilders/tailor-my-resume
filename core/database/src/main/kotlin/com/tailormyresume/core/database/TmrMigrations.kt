package com.tailormyresume.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profile ADD COLUMN userStatedSkills TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "ALTER TABLE profile ADD COLUMN city TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE profile ADD COLUMN linkedinUrl TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE profile ADD COLUMN portfolioUrl TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE profile ADD COLUMN summary TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE profile ADD COLUMN sourceFileName TEXT",
            "ALTER TABLE profile ADD COLUMN reviewedAt INTEGER",
            "ALTER TABLE job_applications ADD COLUMN legacyStatus TEXT",
            "ALTER TABLE job_applications ADD COLUMN location TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE job_applications ADD COLUMN appliedOn INTEGER",
            "ALTER TABLE job_applications ADD COLUMN coverageNow INTEGER",
            "ALTER TABLE job_applications ADD COLUMN coverageUpTo INTEGER",
            "ALTER TABLE job_applications ADD COLUMN coverageFinal INTEGER",
            "ALTER TABLE job_applications ADD COLUMN exportFileName TEXT",
            "ALTER TABLE job_applications ADD COLUMN quickAnswerRequirementId TEXT",
            "ALTER TABLE job_applications ADD COLUMN quickAnswerChoice TEXT",
            "ALTER TABLE job_applications ADD COLUMN quickAnswerDetail TEXT",
            "ALTER TABLE job_applications ADD COLUMN changesAcceptedAt INTEGER",
            "UPDATE job_applications SET legacyStatus = status WHERE status = 'NO_RESPONSE'",
            "UPDATE job_applications SET status = 'APPLIED' WHERE status = 'NO_RESPONSE'",
            "CREATE TABLE IF NOT EXISTS credit_ledger (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "kind TEXT NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "applicationId TEXT, " +
                "productId TEXT, " +
                "createdAt INTEGER NOT NULL)",
        ).forEach { db.execSQL(it) }
    }
}

package com.tailormyresume.core.database

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalHandoffMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test fun upgradeKeepsExistingProfileAndLedgerAndCreatesEmptyOwnershipTables() {
        helper.createDatabase("local-handoff-v5", 4).apply {
            insert("profile", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                put("id", 1)
                put("fullName", "Local profile")
                put("email", "")
                put("phone", "")
                put("headline", "Saved work")
                put("skills", "[\"SQL\"]")
            })
            execSQL("INSERT INTO credit_ledger (id,kind,amount,applicationId,productId,createdAt) VALUES (1,'FREE_GRANT',2,NULL,NULL,0)")
            close()
        }
        val db = helper.runMigrationsAndValidate("local-handoff-v5", 5, true, MIGRATION_4_5)
        db.query("SELECT headline FROM profile").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Saved work")
        }
        db.query("SELECT amount FROM credit_ledger").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(2)
        }
        listOf("local_handoffs", "local_account_owner").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }
}

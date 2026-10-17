package com.tailormyresume.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReferralLedgerMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test fun migrationPreservesLegacyIdsAndAddsScopedNullableUniqueServerIdentity() {
        helper.createDatabase("referral-ledger-v6", 5).apply {
            execSQL("INSERT INTO credit_ledger (id,kind,amount,createdAt) VALUES (17,'PURCHASE',15,12)")
            close()
        }
        val db = helper.runMigrationsAndValidate("referral-ledger-v6", 6, true, MIGRATION_5_6)
        db.query("SELECT id,kind,amount,serverId,owner,environment FROM credit_ledger").use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getLong(0)).isEqualTo(17)
            assertThat(it.getString(1)).isEqualTo("PURCHASE")
            assertThat(it.getInt(2)).isEqualTo(15)
            assertThat(it.isNull(3) && it.isNull(4) && it.isNull(5)).isTrue()
        }
        db.execSQL("INSERT INTO credit_ledger (kind,amount,createdAt,owner,environment,serverId) VALUES ('REFERRAL_REWARD',13,13,'a','staging','reward-1')")
        db.execSQL("INSERT INTO credit_ledger (kind,amount,createdAt,owner,environment,serverId) VALUES ('REFERRAL_REWARD',13,13,'b','staging','reward-1')")
        db.execSQL("INSERT INTO credit_ledger (kind,amount,createdAt,owner,environment,serverId) VALUES ('REFERRAL_REWARD',13,13,'a','production','reward-1')")
        var rejected = false
        try {
            db.execSQL("INSERT INTO credit_ledger (kind,amount,createdAt,owner,environment,serverId) VALUES ('REFERRAL_REWARD',13,13,'a','staging','reward-1')")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }
        assertThat(rejected).isTrue()
        db.close()
    }
}

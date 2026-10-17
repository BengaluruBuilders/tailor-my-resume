package com.tailormyresume.core.database

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.di.DatabaseModule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseModuleTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test
    fun registersEveryMigrationWithoutDestructiveFallback() {
        helper.createDatabase(PRODUCTION_NAME, 1).apply {
            insert(
                "profile",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1)
                    put("fullName", "Priya")
                    put("email", "p@example.com")
                    put("phone", "1")
                    put("headline", "h")
                    put("skills", "[]")
                },
            )
            close()
        }

        val database = DatabaseModule.providesTmrDatabase(RuntimeEnvironment.getApplication())
        try {
            val version = database.openHelper.writableDatabase.version
            assertThat(version).isEqualTo(5)
            database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                val tables = buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
                assertThat(tables).containsAtLeast("local_handoffs", "local_account_owner", "profile", "job_applications")
            }
            database.openHelper.writableDatabase.execSQL("INSERT INTO local_account_owner (id, owner, environment) VALUES (1, 'historical-owner', 'staging')")
            database.openHelper.writableDatabase.execSQL("INSERT INTO local_handoffs (id, destination, source, mapping) VALUES ('handoff', 'destination', 'source', '{}')")
            database.openHelper.writableDatabase.query("SELECT fullName FROM profile").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Priya")
            }
        } finally {
            database.close()
        }

        assertThat(moduleSource().lowercase()).doesNotContain("fallbacktodestructivemigration")
    }

    private fun moduleSource(): String =
        File("src/main/kotlin/com/tailormyresume/core/database/di/DatabaseModule.kt").readText()

    private companion object {
        const val PRODUCTION_NAME = "hh-database"
    }
}

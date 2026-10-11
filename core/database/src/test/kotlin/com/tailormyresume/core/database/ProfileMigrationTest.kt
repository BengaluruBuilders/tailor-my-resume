package com.tailormyresume.core.database

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
class ProfileMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test
    fun migration1To2KeepsTheProfileRowAndStartsWithNoUserStatedSkills() {
        helper.createDatabase(DB_NAME, 1).apply {
            insert(
                "profile",
                android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
                ContentValues().apply {
                    put("id", 1)
                    put("fullName", "Priya")
                    put("email", "p@example.com")
                    put("phone", "1")
                    put("headline", "h")
                    put("skills", "[\"SQL\",\"Excel\"]")
                },
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        migrated.query("SELECT fullName, skills, userStatedSkills FROM profile").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Priya")
            assertThat(cursor.getString(1)).isEqualTo("[\"SQL\",\"Excel\"]")
            assertThat(cursor.getString(2)).isEqualTo("[]")
        }
    }

    @Test
    fun migration2To3KeepsValuesMapsNoResponseToAppliedKeepsLegacyStatusAndNotes() {
        helper.createDatabase(DB_NAME, 2).apply {
            insertProfile()
            insertApplication("saved", "SAVED", "keep this note")
            insertApplication("silent", "NO_RESPONSE", "no reply yet")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        migrated.query("SELECT fullName, skills, userStatedSkills, city, linkedinUrl, portfolioUrl, summary, sourceFileName, reviewedAt FROM profile").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Priya")
            assertThat(cursor.getString(1)).isEqualTo("[\"SQL\"]")
            assertThat(cursor.getString(2)).isEqualTo("[]")
            assertThat(cursor.getString(3)).isEqualTo("")
            assertThat(cursor.getString(4)).isEqualTo("")
            assertThat(cursor.getString(5)).isEqualTo("")
            assertThat(cursor.getString(6)).isEqualTo("")
            assertThat(cursor.isNull(7)).isTrue()
            assertThat(cursor.isNull(8)).isTrue()
        }
        migrated.query("SELECT id, jobTitle, company, rawText, status, notes, legacyStatus, location, appliedOn, coverageNow, coverageUpTo, coverageFinal, exportFileName, quickAnswerRequirementId, quickAnswerChoice, quickAnswerDetail, changesAcceptedAt FROM job_applications ORDER BY id").use { cursor ->
            assertThat(cursor.count).isEqualTo(2)
            cursor.moveToFirst()
            assertThat(cursor.getString(0)).isEqualTo("saved")
            assertThat(cursor.getString(1)).isEqualTo("Analyst")
            assertThat(cursor.getString(2)).isEqualTo("Northwind")
            assertThat(cursor.getString(3)).isEqualTo("raw")
            assertThat(cursor.getString(4)).isEqualTo("SAVED")
            assertThat(cursor.getString(5)).isEqualTo("keep this note")
            assertThat(cursor.isNull(6)).isTrue()
            assertThat(cursor.getString(7)).isEqualTo("")
            (8..16).forEach { column -> assertThat(cursor.isNull(column)).isTrue() }
            cursor.moveToNext()
            assertThat(cursor.getString(0)).isEqualTo("silent")
            assertThat(cursor.getString(4)).isEqualTo("APPLIED")
            assertThat(cursor.getString(5)).isEqualTo("no reply yet")
            assertThat(cursor.getString(6)).isEqualTo("NO_RESPONSE")
        }
    }

    @Test
    fun migration2To3CreatesEmptyCreditLedger() {
        helper.createDatabase(DB_NAME, 2).close()

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        migrated.query("SELECT COUNT(*) FROM credit_ledger").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(0)
        }
    }

    @Test
    fun migration2To3SqlIsAdditiveOnly() {
        val statements = recordedStatements()

        assertThat(statements).isNotEmpty()
        statements.forEach { statement ->
            assertThat(statement.trimStart().uppercase()).containsMatch("^(ALTER TABLE \\w+ ADD COLUMN|CREATE TABLE|UPDATE)")
            assertThat(statement.uppercase()).doesNotContain("DROP")
            assertThat(statement.uppercase()).doesNotContain("RENAME")
        }
    }

    @Test
    fun migratingFromVersion1ReachesVersion3() {
        helper.createDatabase(DB_NAME, 1).apply {
            insertProfile()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        migrated.query("SELECT fullName FROM profile").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Priya")
        }
    }

    private fun recordedStatements(): List<String> {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") statements += args[0] as String
            null
        } as SupportSQLiteDatabase
        MIGRATION_2_3.migrate(database)
        return statements
    }

    private fun SupportSQLiteDatabase.insertProfile() {
        insert(
            "profile",
            android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
            ContentValues().apply {
                put("id", 1)
                put("fullName", "Priya")
                put("email", "p@example.com")
                put("phone", "1")
                put("headline", "h")
                put("skills", "[\"SQL\"]")
                if (version >= 2) put("userStatedSkills", "[]")
            },
        )
    }

    private fun SupportSQLiteDatabase.insertApplication(id: String, status: String, notes: String) {
        insert(
            "job_applications",
            android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL,
            ContentValues().apply {
                put("id", id)
                put("jobTitle", "Analyst")
                put("company", "Northwind")
                put("rawText", "raw")
                put("requirements", "[]")
                put("status", status)
                put("notes", notes)
                put("createdAt", 1L)
                put("updatedAt", 2L)
            },
        )
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}

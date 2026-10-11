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
class ContactFlagMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test
    fun migration3To4KeepsTheProfileRowAndReadsContactUserStatedAsFalse() {
        helper.createDatabase(DB_NAME, 3).apply {
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
                    put("city", "Pune")
                },
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        migrated.query("SELECT fullName, city, skills, contactUserStated FROM profile").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("Priya")
            assertThat(cursor.getString(1)).isEqualTo("Pune")
            assertThat(cursor.getString(2)).isEqualTo("[\"SQL\"]")
            assertThat(cursor.getInt(3)).isEqualTo(0)
        }
    }

    @Test
    fun migration3To4SqlIsAdditiveOnly() {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") statements += args[0] as String
            null
        } as SupportSQLiteDatabase

        MIGRATION_3_4.migrate(database)

        assertThat(statements).isNotEmpty()
        statements.forEach { statement ->
            assertThat(statement.trimStart().uppercase()).containsMatch("^ALTER TABLE \\w+ ADD COLUMN .* NOT NULL DEFAULT 0")
            assertThat(statement.uppercase()).doesNotContain("DROP")
            assertThat(statement.uppercase()).doesNotContain("RENAME")
        }
    }

    private companion object {
        const val DB_NAME = "contact-flag-migration-test"
    }
}

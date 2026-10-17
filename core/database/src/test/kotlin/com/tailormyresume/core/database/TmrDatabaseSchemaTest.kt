package com.tailormyresume.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TmrDatabaseSchemaTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TmrDatabase::class.java)

    @Test
    fun versionIs5AndSchema5JsonIsCommitted() {
        val database = createInMemoryTmrDatabase(RuntimeEnvironment.getApplication())
        try {
            assertThat(database.openHelper.writableDatabase.version).isEqualTo(5)
        } finally {
            database.close()
        }

        helper.createDatabase("schema-5", 5).close()
    }
}

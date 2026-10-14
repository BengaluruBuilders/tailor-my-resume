package com.tailormyresume.app.auth

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.database.createInMemoryTmrDatabase
import com.tailormyresume.core.domain.account.ExportedFiles
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalDataWiperOrderTest {
    private lateinit var database: TmrDatabase

    @Before
    fun createDatabase() {
        database = createInMemoryTmrDatabase(RuntimeEnvironment.getApplication())
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun exportFilesAreDeletedBeforeTheStoreIsCleared() = runTest {
        val store = TestMockStateStore()
        store.write("exports.history", "[]")
        var storeHadDataWhenFilesWereDeleted: Boolean? = null
        val files = ExportedFiles { storeHadDataWhenFilesWereDeleted = store.read("exports.history") != null }

        RoomLocalDataWiper(database, store, files, UnconfinedTestDispatcher()).wipeAll()

        assertThat(storeHadDataWhenFilesWereDeleted).isTrue()
        assertThat(store.read("exports.history")).isNull()
    }
}

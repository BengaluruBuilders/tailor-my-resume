package com.tailormyresume.core.data.mock

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LegacyDataPurgeTest {

    @Test
    fun legacyFeatureKeysAreRemovedIncludingDraftsAndOthersKept() = runTest {
        val store = TestMockStateStore()
        store.write("coverletter.x", "letter")
        store.write("prep.plan.y", "plan")
        store.write("prep.plan.analysis-draft-z", "draft plan")
        store.write("session.account", "kept")
        store.write("profile.facts", "kept")

        LegacyDataPurge(store)()

        assertThat(store.read("coverletter.x")).isNull()
        assertThat(store.read("prep.plan.y")).isNull()
        assertThat(store.read("prep.plan.analysis-draft-z")).isNull()
        assertThat(store.read("session.account")).isEqualTo("kept")
        assertThat(store.read("profile.facts")).isEqualTo("kept")
    }

    @Test
    fun runningTwiceOnAnEmptyStoreSucceeds() = runTest {
        val purge = LegacyDataPurge(TestMockStateStore())

        purge()
        purge()
    }
}

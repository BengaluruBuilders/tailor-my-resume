package com.tailormyresume.app.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataExportsStartTaskTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L

    private fun archiveModified(hoursAgo: Long, name: String): File =
        File(folder.root.resolve("data-exports").apply { mkdirs() }, name).apply {
            writeText("x")
            setLastModified(now - hoursAgo * hour)
        }

    private fun kotlinx.coroutines.test.TestScope.startTask() {
        DataExportsStartTask(
            File(folder.root, "data-exports"),
            UnconfinedTestDispatcher(testScheduler),
            TestScope(testScheduler),
        ) { now }.start()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun startRemovesAnArchiveOlderThanADayAndLeavesTheRestOfTheCache() = runTest(UnconfinedTestDispatcher()) {
        val stale = archiveModified(hoursAgo = 25, name = "tailormyresume-my-data.zip")
        val resume = File(folder.newFolder("exports"), "resume.pdf").apply { writeText("x") }

        startTask()

        assertThat(stale.exists()).isFalse()
        assertThat(resume.exists()).isTrue()
    }

    @Test
    fun startKeepsAnArchiveFromTheLastDaySoAColdStartedShareTargetCanStillReadIt() = runTest(UnconfinedTestDispatcher()) {
        val fresh = archiveModified(hoursAgo = 23, name = "tailormyresume-my-data.zip")

        startTask()

        assertThat(fresh.exists()).isTrue()
    }
}

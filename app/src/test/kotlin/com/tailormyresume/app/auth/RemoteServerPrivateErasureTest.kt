package com.tailormyresume.app.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.account.LocalWorkCheckpoint
import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.database.TmrDatabase
import com.tailormyresume.core.domain.FirebaseUidProvider
import com.tailormyresume.core.network.TailorMyResumeApiConfig
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestPendingAccountWipe
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RemoteServerPrivateErasureTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, TmrDatabase::class.java).allowMainThreadQueries().build()
    private val server = MockWebServer().apply { start() }

    @After fun close() {
        database.close()
        server.shutdown()
        listOf("account-work", "account-upgrades").forEach { File(context.filesDir, it).deleteRecursively() }
    }

    @Test fun successfulDeleteErasesPrivateRecoveryAndFreezesLateWriters() = runTest { verifyErasure(probe = false, deleted = false) }

    @Test fun alreadyDeletedRecoveryStillErasesPrivateRecoveryAndFreezesLateWriters() = runTest { verifyErasure(probe = false, deleted = true) }

    @Test fun closedAccountProbeErasesPrivateRecoveryAndFreezesLateWriters() = runTest { verifyErasure(probe = true, deleted = true) }

    private suspend fun TestScope.verifyErasure(probe: Boolean, deleted: Boolean) {
        val io = UnconfinedTestDispatcher(testScheduler)
        val environment = server.url("/").toString()
        val checkpoints = LocalWorkCheckpoint(context, database, TestMockStateStore(), io)
        val journals = PrivateUpgradeStore(context, io)
        val fence = LocalWorkFence()
        val ticket = fence.ticket()
        checkpoints.capture("A", environment)
        checkpoints.restore("B", environment)
        checkpoints.capture("B", environment)
        val original = UpgradeJournal(environment, "A", "prepare-A", "proof", completionKey = "complete", observationKey = "observe")
            .copy(recentAuthRecovery = RecentGoogleAuthRecovery("A", GoogleCredentialIdentity.digestSubject("google-A"), "CLAIM"))
        val other = original.copy(source = "B", prepareKey = "prepare-B", recentAuthRecovery = null)
        journals.write(original)
        journals.write(other)
        val eraser = AccountPrivateWorkEraser(FirebaseUidProvider { "A" }, TestSessionRepository(), TestPendingAccountWipe(), checkpoints, journals, TailorMyResumeApiConfig(environment), fence)
        val deleter = RemoteServerAccountDeleter(server.api(), eraser)
        server.enqueue(if (deleted) errorResponse(409, "ACCOUNT_DELETED") else jsonResponse(200, """{"deletion":{"status":"done"}}"""))
        if (probe) assertThat(deleter.isClosed().getOrThrow()).isTrue() else assertThat(deleter.delete().isSuccess).isTrue()
        assertThat(checkpoints.read("A", environment)).isNull()
        assertThat(checkpoints.read("B", environment)).isNotNull()
        assertThat(journals.readFor("A", environment)).isNull()
        assertThat(journals.readFor("B", environment)).isEqualTo(other)
        assertThat(runCatching { journals.write(original) }.isFailure).isTrue()
        assertThat(runCatching { checkpoints.capture("A", environment) }.isFailure).isTrue()
        assertThat(runCatching { fence.check(ticket) }.isFailure).isTrue()
        assertThat(runCatching { fence.check(fence.ticket()) }.isFailure).isTrue()
    }
}

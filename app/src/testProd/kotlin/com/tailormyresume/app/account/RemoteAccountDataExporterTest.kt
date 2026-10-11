package com.tailormyresume.app.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.network.tailormyresumeJson
import com.tailormyresume.core.testing.account.AccountDataExporterContractTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile
import kotlin.time.Instant

class RemoteAccountDataExporterTest : AccountDataExporterContractTest() {
    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer().apply {
        dispatcher = QueueDispatcher().apply { setFailFast(errorResponse(500, "INTERNAL_ERROR")) }
        start()
    }

    override fun createExporter(): AccountDataExporter =
        RemoteAccountDataExporter({ folder.root }, server.api(), tailormyresumeJson(), Dispatchers.Unconfined)

    private val data = AccountData(
        generatedAt = Instant.fromEpochMilliseconds(1_790_000_000_000),
        account = null,
        profile = null,
        applications = emptyList(),
        entitlement = PurchaseEntitlement(freeCredits = 1, purchasedCredits = 0, pendingPackIds = emptyList()),
        purchases = emptyList(),
        exports = emptyList(),
    )

    @After
    fun tearDown() = server.shutdown()

    private fun exporter(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        RemoteAccountDataExporter({ folder.root }, server.api(), tailormyresumeJson(), dispatcher)

    private fun entries(file: java.io.File): Map<String, String> = ZipFile(file).use { zip ->
        zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
    }

    @Test
    fun serverPartIsAddedNextToTheLocalFiles() = runTest {
        server.enqueue(
            jsonResponse(
                200,
                """{"export":{"generatedAt":"t","user":{"id":"u","createdAt":"t"},"consents":[],"wallet":{},"unlocks":[],"purchases":[],"contentReports":[]}}""",
            ),
        )

        val archive = exporter(StandardTestDispatcher(testScheduler)).export(data)

        val files = entries(archive.file)
        assertThat(files.keys).containsAtLeast("account.txt", "profile.txt", "applications.txt", "purchases.txt", "server.json")
        assertThat(files.getValue("server.json")).contains("\"contentReports\":[]")
    }

    @Test
    fun failedFetchStillExportsTheLocalPartWithAPlaceholder() = runTest {
        server.enqueue(errorResponse(500, "INTERNAL_ERROR"))

        val archive = exporter(StandardTestDispatcher(testScheduler)).export(data)

        val files = entries(archive.file)
        assertThat(files).containsKey("account.txt")
        assertThat(files).containsKey("my-data.json")
        assertThat(files.getValue("server.json")).contains("could not be fetched")
    }

    @Test
    fun offlineFetchStillExportsTheLocalPartWithAPlaceholder() = runTest {
        server.enqueue(jsonResponse(200, "{}").setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val archive = exporter(StandardTestDispatcher(testScheduler)).export(data)

        assertThat(entries(archive.file).getValue("server.json")).contains("could not be fetched")
    }
}

package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseState
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.model.CreditKind
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.account.AccountDataExporterContractTest
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile
import kotlin.time.Instant

class OfflineAccountDataExporterTest : AccountDataExporterContractTest() {

    @get:Rule
    val folder = TemporaryFolder()

    override fun createExporter(): AccountDataExporter =
        OfflineAccountDataExporter({ folder.root }, NoMockLatency, Dispatchers.Unconfined)

    @Test
    fun theArchiveHoldsTheFiveDataFiles() = runTest {
        val now = Instant.fromEpochMilliseconds(1_790_000_000_000)
        val data = AccountData(
            generatedAt = now,
            account = SignInAccount.localAccount,
            profile = canonicalCandidateProfile,
            applications = listOf(sampleApplication),
            entitlement = PurchaseEntitlement(0, 4, emptyList()),
            purchases = listOf(PurchaseRecord("application_pack_5", "order-1", now, PurchaseState.COMPLETED)),
            exports = listOf(ExportRecord(sampleApplication.id, ExportFormat.PDF, "a.pdf", now, CreditKind.FREE)),
        )

        val archive = createExporter().export(data)

        ZipFile(archive.file).use { zip ->
            assertThat(zip.entries().toList().map { it.name })
                .containsExactly("account.txt", "profile.txt", "applications.txt", "purchases.txt", "my-data.json")
            val purchases = zip.getInputStream(zip.getEntry("purchases.txt")).bufferedReader().readText()
            assertThat(purchases).contains("order-1")
            val applications = zip.getInputStream(zip.getEntry("applications.txt")).bufferedReader().readText()
            assertThat(applications).contains("a.pdf")
        }
    }

    @Test
    fun exportingRemovesAnEarlierArchiveFromItsOwnDirectory() = runTest {
        val earlier = File(folder.root, "earlier-export.zip").apply { writeText("x") }
        val now = Instant.fromEpochMilliseconds(1_790_000_000_000)
        val data = AccountData(
            generatedAt = now,
            account = null,
            profile = null,
            applications = emptyList(),
            entitlement = PurchaseEntitlement(0, 0, emptyList()),
            purchases = emptyList(),
            exports = emptyList(),
        )

        val archive = createExporter().export(data)

        assertThat(earlier.exists()).isFalse()
        assertThat(archive.file.exists()).isTrue()
    }
}

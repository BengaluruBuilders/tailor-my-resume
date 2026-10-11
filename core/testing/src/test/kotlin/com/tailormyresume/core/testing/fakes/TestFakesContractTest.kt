package com.tailormyresume.core.testing.fakes

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.MockStateStore
import com.tailormyresume.core.data.repository.ContentReportRepository
import com.tailormyresume.core.data.repository.ExportHistoryRepository
import com.tailormyresume.core.data.repository.SessionRepository
import com.tailormyresume.core.data.repository.TailoringReviewStateRepository
import com.tailormyresume.core.domain.PaymentGateway
import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.testing.account.AccountDataExporterContractTest
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.gateway.PaymentGatewayContractTest
import com.tailormyresume.core.testing.gateway.SignInGatewayContractTest
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.mock.MockStateStoreContractTest
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.ContentReportRepositoryContractTest
import com.tailormyresume.core.testing.repository.ExportHistoryRepositoryContractTest
import com.tailormyresume.core.testing.repository.SessionRepositoryContractTest
import com.tailormyresume.core.testing.repository.TailoringReviewStateRepositoryContractTest
import com.tailormyresume.core.testing.repository.TestContentReportRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.repository.TestTailoringReviewStateRepository
import com.tailormyresume.core.testing.sample.TestSampleDataController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TestMockStateStoreContractTest : MockStateStoreContractTest() {
    override fun createStore(scope: CoroutineScope): MockStateStore = TestMockStateStore()
}

class TestSessionRepositoryContractTest : SessionRepositoryContractTest() {
    override fun createSessionRepository(): SessionRepository = TestSessionRepository()
}

class TestExportHistoryRepositoryContractTest : ExportHistoryRepositoryContractTest() {
    override fun createExportHistoryRepository(): ExportHistoryRepository = TestExportHistoryRepository()
}

class TestContentReportRepositoryContractTest : ContentReportRepositoryContractTest() {
    override fun createContentReportRepository(): ContentReportRepository = TestContentReportRepository()
}

class TestTailoringReviewStateRepositoryContractTest : TailoringReviewStateRepositoryContractTest() {
    override fun createTailoringReviewStateRepository(): TailoringReviewStateRepository =
        TestTailoringReviewStateRepository()
}

class TestAccountDataExporterContractTest : AccountDataExporterContractTest() {
    override fun createExporter(): AccountDataExporter = TestAccountDataExporter()
}

class TestPaymentGatewayContractTest : PaymentGatewayContractTest() {
    override fun createPaymentGateway(): PaymentGateway = TestPaymentGateway()
}

class TestSignInGatewayContractTest : SignInGatewayContractTest() {
    override fun createSignInGateway(): SignInGateway = TestSignInGateway()
}

class TestSampleDataControllerTest {
    @Test
    fun itCountsLoadsAndResets() = runTest {
        val controller = TestSampleDataController()

        controller.load()
        controller.reset()
        controller.reset()

        assertThat(controller.loadCount).isEqualTo(1)
        assertThat(controller.resetCount).isEqualTo(2)
    }
}

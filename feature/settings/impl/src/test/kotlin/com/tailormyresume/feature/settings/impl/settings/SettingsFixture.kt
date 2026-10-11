package com.tailormyresume.feature.settings.impl.settings

import com.tailormyresume.core.domain.SignInGateway
import com.tailormyresume.core.domain.account.AccountData
import com.tailormyresume.core.domain.account.AccountDataArchive
import com.tailormyresume.core.domain.account.AccountDataExporter
import com.tailormyresume.core.domain.account.CollectAccountDataUseCase
import com.tailormyresume.core.domain.account.ExportAccountDataUseCase
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.account.TestAccountDataExporter
import com.tailormyresume.core.testing.gateway.TestPaymentGateway
import com.tailormyresume.core.testing.gateway.TestSignInGateway
import com.tailormyresume.core.testing.repository.TestApplicationRepository
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestExportHistoryRepository
import com.tailormyresume.core.testing.repository.TestProfileRepository
import com.tailormyresume.core.testing.repository.TestResumeSettingsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.CompletableDeferred
import java.io.File

internal val settingsTestAccount = SignInAccount(
    id = "account-1",
    displayName = "Priya Deshmukh",
    email = "priya.deshmukh@gmail.com",
)

internal class GatedExporter : AccountDataExporter {
    val gate = CompletableDeferred<Unit>()
    var calls = 0

    override suspend fun export(data: AccountData): AccountDataArchive {
        calls += 1
        gate.await()
        val file = File.createTempFile("gated-export", ".zip").apply { deleteOnExit() }
        return AccountDataArchive(fileName = file.name, file = file)
    }
}

internal class FailingExporter : AccountDataExporter {
    var calls = 0

    override suspend fun export(data: AccountData): AccountDataArchive {
        calls += 1
        error("exporter failed")
    }
}

internal class CountingSignInGateway(
    private val delegate: SignInGateway,
    private val gate: CompletableDeferred<Unit>? = null,
) : SignInGateway by delegate {
    var signOutCalls = 0
    var signOutCompleted = false

    override suspend fun signOut() {
        signOutCalls += 1
        gate?.await()
        delegate.signOut()
        signOutCompleted = true
    }
}

internal class SettingsFixture(
    val exporter: AccountDataExporter = TestAccountDataExporter(),
    gate: CompletableDeferred<Unit>? = null,
) {
    val session = TestSessionRepository()
    val credits = TestCreditsRepository()
    val settings = TestResumeSettingsRepository()
    val signIn = CountingSignInGateway(TestSignInGateway(session), gate)

    fun viewModel(): SettingsViewModel = SettingsViewModel(
        sessionRepository = session,
        creditsRepository = credits,
        resumeSettingsRepository = settings,
        exportAccountData = ExportAccountDataUseCase(
            collectAccountData = CollectAccountDataUseCase(
                sessionRepository = session,
                profileRepository = TestProfileRepository(),
                applicationRepository = TestApplicationRepository(),
                exportHistoryRepository = TestExportHistoryRepository(),
                paymentGateway = TestPaymentGateway(),
                creditsRepository = credits,
                resumeSettingsRepository = settings,
                clock = TestClock(),
            ),
            exporter = exporter,
        ),
        signInGateway = signIn,
    )
}

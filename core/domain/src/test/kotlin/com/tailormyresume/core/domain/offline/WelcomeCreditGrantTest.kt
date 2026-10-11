package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.mock.NoMockLatency
import com.tailormyresume.core.domain.SignInOutcome
import com.tailormyresume.core.domain.TailoringCreditSpend
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.TestCreditsRepository
import com.tailormyresume.core.testing.repository.TestSessionRepository
import com.tailormyresume.core.testing.util.TestClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WelcomeCreditGrantTest {

    private val credits = TestCreditsRepository()
    private val clock = TestClock()
    private val grant = LedgerWelcomeCreditGrant(credits, clock)

    private fun gateway() = OfflineSignInGateway(
        TestSessionRepository(),
        NoMockLatency,
        TestMockStateStore(),
        welcomeGrant = grant,
    )

    private suspend fun kinds() = credits.observeLedger().first().map { it.kind }

    @Test
    fun theFirstSignInGrantsTheFreeFirstResumeToTheLedger() = runTest {
        gateway().signIn()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(1)
        assertThat(kinds()).containsExactly(CreditLedgerKind.FREE_GRANT)
    }

    @Test
    fun signingInAgainDoesNotGrantASecondOne() = runTest {
        gateway().signIn()
        gateway().signIn()
        grant.grantOnce()

        assertThat(kinds()).containsExactly(CreditLedgerKind.FREE_GRANT)
    }

    @Test
    fun aSpentGrantIsNotGrantedAgain() = runTest {
        gateway().signIn()
        credits.record(TailoringCreditSpend.spendEntry("application-1", "run-1", clock))

        gateway().signIn()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(0)
    }

    @Test
    fun theFirstTailoringIsCoveredByTheGrant() = runTest {
        gateway().signIn()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isGreaterThan(0)
        credits.record(TailoringCreditSpend.spendEntry("application-1", "run-1", clock))
        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(0)
    }

    @Test
    fun aCancelledOrFailedSignInGrantsNothing() = runTest {
        gateway().withOutcome(SignInOutcome.Cancelled).signIn()
        gateway().withOutcome(SignInOutcome.Failed).signIn()

        assertThat(kinds()).isEmpty()
    }

    @Test
    fun aClearedLedgerGrantsAgainForTheNextAccount() = runTest {
        gateway().signIn()
        credits.clear()

        gateway().signIn()

        assertThat(credits.observeLedger().first().sumOf { it.amount }).isEqualTo(1)
    }
}

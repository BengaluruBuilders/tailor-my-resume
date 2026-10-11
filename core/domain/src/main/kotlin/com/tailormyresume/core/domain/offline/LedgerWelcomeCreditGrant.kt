package com.tailormyresume.core.domain.offline

import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.domain.account.WelcomeCreditGrant
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class LedgerWelcomeCreditGrant @Inject constructor(
    private val credits: CreditsRepository,
    private val clock: Clock,
) : WelcomeCreditGrant {

    private val mutex = Mutex()

    override suspend fun grantOnce() = mutex.withLock {
        val granted = credits.observeLedger().first().any { it.kind == CreditLedgerKind.FREE_GRANT }
        if (!granted) credits.record(CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, FIRST_RESUME_CREDITS, null, null, clock.now()))
    }

    private companion object {
        const val FIRST_RESUME_CREDITS = 1
    }
}

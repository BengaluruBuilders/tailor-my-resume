package com.tailormyresume.feature.applications.impl

import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.testing.data.PrototypeFixtures
import kotlin.time.Instant

internal fun prototypeApplication(id: String): JobApplication =
    PrototypeFixtures.returning().applications.single { it.id == id }

internal fun grant(amount: Int): CreditLedgerEntry =
    CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, amount, null, null, Instant.parse("2026-10-02T09:00:00Z"))

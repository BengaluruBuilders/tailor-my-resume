package com.tailormyresume.feature.settings.impl.credits

import com.tailormyresume.core.domain.offline.MockPackCatalogue
import com.tailormyresume.core.testing.data.PrototypeFixtures
import com.tailormyresume.core.testing.data.PrototypeScenario
import java.time.ZoneOffset

internal val returningScenario: PrototypeScenario = PrototypeFixtures.returning()

internal fun returningHistory() = toHistoryRows(
    entries = returningScenario.ledger,
    applications = returningScenario.applications,
    packs = MockPackCatalogue.all,
    zone = ZoneOffset.UTC,
)

internal fun returningCreditsState() = CreditsUiState.Content(credits = 3, history = returningHistory())

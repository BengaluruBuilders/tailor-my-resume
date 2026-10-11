package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MockPackCatalogueTest {
    @Test
    fun holdsExactlyPacks5_15_40WithCreditsNeverExpiring() {
        val packs = MockPackCatalogue.all

        assertThat(packs.map { it.id }).containsExactly("application_pack_5", "application_pack_15", "application_pack_40").inOrder()
        assertThat(packs.map { it.credits }).containsExactly(5, 15, 40).inOrder()
        assertThat(packs.map { it.creditsExpire }).containsExactly(false, false, false)
        assertThat(packs.map { it.priceInPaise }).containsExactly(19_900L, 44_900L, 99_900L).inOrder()
    }
}

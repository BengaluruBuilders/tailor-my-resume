package com.tailormyresume.feature.settings.impl.paywall

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.MockPackCatalogue
import org.junit.Test
import java.io.File

class PackPriceFormatterTest {

    private val rupee = "₹"

    @Test
    fun mockCataloguePricesMatchThePrototype() {
        val prices = MockPackCatalogue.all.map { formatPackPrice(it.priceInPaise, it.currencyCode) }

        assertThat(prices).containsExactly("${rupee}199", "${rupee}449", "${rupee}999").inOrder()
    }

    @Test
    fun perResumePriceIsTheRoundedWholeUnit() {
        val perResume = MockPackCatalogue.all.map(::formatPricePerResume)

        assertThat(perResume).containsExactly("${rupee}40", "${rupee}30", "${rupee}25").inOrder()
    }

    @Test
    fun priceWithPaiseKeepsTwoDecimals() {
        assertThat(formatPackPrice(19_950, "INR")).isEqualTo("${rupee}199.50")
    }

    @Test
    fun noRupeeLiteralInMainSources() {
        val roots = listOf(
            File("src/main/kotlin/com/tailormyresume/feature/settings/impl/paywall"),
            File("src/main/kotlin/com/tailormyresume/feature/settings/impl/credits"),
            File("src/main/res/values/strings_credits.xml"),
        )
        val offenders = roots.flatMap { root -> root.walkTopDown().filter { it.isFile && rupee in it.readText() }.toList() }

        assertThat(roots.all { it.exists() }).isTrue()
        assertThat(offenders).isEmpty()
    }
}

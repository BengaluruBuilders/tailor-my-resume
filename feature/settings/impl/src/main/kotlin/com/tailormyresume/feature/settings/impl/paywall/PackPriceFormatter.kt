package com.tailormyresume.feature.settings.impl.paywall

import com.tailormyresume.core.domain.ApplicationPack
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val INDIA: Locale = Locale.forLanguageTag("en-IN")

internal fun formatPackPrice(priceInPaise: Long, currencyCode: String): String =
    formatMoney(BigDecimal.valueOf(priceInPaise, minorUnitDigits(currencyCode)), currencyCode)

internal fun formatPricePerResume(pack: ApplicationPack): String {
    val price = BigDecimal.valueOf(pack.priceInPaise, minorUnitDigits(pack.currencyCode))
    val perResume = price.divide(BigDecimal(pack.credits.coerceAtLeast(1)), 0, RoundingMode.HALF_UP)
    return formatMoney(perResume, pack.currencyCode)
}

private fun minorUnitDigits(currencyCode: String): Int = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)

private fun formatMoney(amount: BigDecimal, currencyCode: String): String {
    val currency = Currency.getInstance(currencyCode)
    val format = NumberFormat.getCurrencyInstance(INDIA)
    format.currency = currency
    format.minimumFractionDigits = if (amount.stripTrailingZeros().scale() <= 0) 0 else currency.defaultFractionDigits
    return format.format(amount)
}

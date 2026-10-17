package com.tailormyresume.feature.tailor.impl.tailoring

import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey

internal class TailoringNavigation(
    private val navigator: Navigator,
    private val applicationId: String,
) {
    fun noCredit() = navigator.replace(PaywallNavKey(applicationId))
}

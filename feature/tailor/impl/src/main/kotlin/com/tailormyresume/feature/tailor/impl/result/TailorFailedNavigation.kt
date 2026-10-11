package com.tailormyresume.feature.tailor.impl.result

import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey

internal class TailorFailedNavigation(
    private val navigator: Navigator,
    private val applicationId: String,
    private val runId: String,
) {
    fun retry() = navigator.replace(TailoringNavKey(applicationId, runId))

    fun goBack() {
        navigator.goBack()
    }
}

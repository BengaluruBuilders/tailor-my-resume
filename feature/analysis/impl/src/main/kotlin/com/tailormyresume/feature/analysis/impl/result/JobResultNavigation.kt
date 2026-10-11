package com.tailormyresume.feature.analysis.impl.result

import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.navigateToQuickQuestion
import com.tailormyresume.feature.settings.api.navigation.navigateToPaywall
import com.tailormyresume.feature.tailor.api.navigation.navigateToTailoring

internal class JobResultNavigation(private val navigator: Navigator) {
    fun handle(event: JobResultEvent) {
        when (event) {
            is JobResultEvent.Paywall -> navigator.navigateToPaywall(event.applicationId)
            is JobResultEvent.QuickQuestion -> navigator.navigateToQuickQuestion(event.applicationId)
            is JobResultEvent.Tailor -> navigator.navigateToTailoring(event.applicationId)
        }
    }
}

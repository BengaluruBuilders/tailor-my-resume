package com.tailormyresume.feature.analysis.impl.question

import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.tailor.api.navigation.navigateToTailoring

internal class QuickQuestionNavigation(private val navigator: Navigator) {
    fun forwardToTailoring(applicationId: String) {
        if (navigator.state.canGoBack) navigator.state.stack.removeLastOrNull()
        navigator.navigateToTailoring(applicationId)
    }
}

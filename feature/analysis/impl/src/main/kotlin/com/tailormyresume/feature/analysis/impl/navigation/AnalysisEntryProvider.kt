package com.tailormyresume.feature.analysis.impl.navigation

import androidx.compose.foundation.text.BasicText
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.AnalysisNavKey

fun EntryProviderScope<NavKey>.analysisEntry(navigator: Navigator) {
    entry<AnalysisNavKey> { key ->
        BasicText(text = key::class.simpleName.orEmpty())
    }
}

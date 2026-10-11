package com.tailormyresume.feature.analysis.impl.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.JobLinkNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.analysis.impl.job.JobRoute
import com.tailormyresume.feature.analysis.impl.joblink.JobLinkRoute
import com.tailormyresume.feature.analysis.impl.question.QuickQuestionRoute
import com.tailormyresume.feature.analysis.impl.question.QuickQuestionViewModel
import com.tailormyresume.feature.analysis.impl.result.JobResultRoute
import com.tailormyresume.feature.analysis.impl.result.JobResultViewModel

fun EntryProviderScope<NavKey>.analysisEntry(navigator: Navigator) {
    entry<JobNavKey> { JobRoute(navigator) }
    entry<JobLinkNavKey> { JobLinkRoute(navigator) }
    entry<JobResultNavKey> { key ->
        JobResultRoute(
            navigator = navigator,
            viewModel = hiltViewModel<JobResultViewModel, JobResultViewModel.Factory>(key = "result-${key.applicationId}") { factory ->
                factory.create(key.applicationId)
            },
        )
    }
    entry<QuickQuestionNavKey> { key ->
        QuickQuestionRoute(
            navigator = navigator,
            viewModel = hiltViewModel<QuickQuestionViewModel, QuickQuestionViewModel.Factory>(key = "question-${key.applicationId}") { factory ->
                factory.create(key.applicationId)
            },
        )
    }
}

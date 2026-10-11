package com.tailormyresume.feature.analysis.impl.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.navigation.NavigationState
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.JobResultNavKey
import com.tailormyresume.feature.analysis.api.navigation.QuickQuestionNavKey
import com.tailormyresume.feature.analysis.impl.question.QuickQuestionNavigation
import com.tailormyresume.feature.analysis.impl.result.JobResultEvent
import com.tailormyresume.feature.analysis.impl.result.JobResultNavigation
import com.tailormyresume.feature.settings.api.navigation.PaywallNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoredNavKey
import com.tailormyresume.feature.tailor.api.navigation.TailoringNavKey
import org.junit.Test

private const val APP = "app-1"

class ResultNavigationTest {

    private fun navigatorOver(vararg keys: NavKey) = Navigator(NavigationState(NavBackStack(*keys)))

    private fun Navigator.tailoringKeys() = state.stack.filterIsInstance<TailoringNavKey>()

    @Test
    fun paywallCarriesTheApplicationToReturnTo() {
        val navigator = navigatorOver(JobResultNavKey(APP))
        JobResultNavigation(navigator).handle(JobResultEvent.Paywall(APP))
        assertThat(navigator.state.stack.last()).isEqualTo(PaywallNavKey(returnToApplicationId = APP))
    }

    @Test
    fun questionEventOpensTheQuickQuestion() {
        val navigator = navigatorOver(JobResultNavKey(APP))
        JobResultNavigation(navigator).handle(JobResultEvent.QuickQuestion(APP))
        assertThat(navigator.state.stack.last()).isEqualTo(QuickQuestionNavKey(APP))
    }

    @Test
    fun doubleTapStacksOneTailoringKey() {
        val navigator = navigatorOver(JobResultNavKey(APP))
        val navigation = JobResultNavigation(navigator)
        navigation.handle(JobResultEvent.Tailor(APP))
        navigation.handle(JobResultEvent.Tailor(APP))
        assertThat(navigator.tailoringKeys()).hasSize(1)
    }

    @Test
    fun tapAfterAFinishedRunMintsADifferentRunId() {
        val navigator = navigatorOver(JobResultNavKey(APP))
        val navigation = JobResultNavigation(navigator)
        navigation.handle(JobResultEvent.Tailor(APP))
        val first = navigator.tailoringKeys().single()
        navigator.replace(TailoredNavKey(APP))
        navigation.handle(JobResultEvent.Tailor(APP))
        assertThat(navigator.tailoringKeys().single().runId).isNotEqualTo(first.runId)
    }

    @Test
    fun forwardingFromTheQuestionReplacesIt() {
        val navigator = navigatorOver(JobResultNavKey(APP), QuickQuestionNavKey(APP))
        QuickQuestionNavigation(navigator).forwardToTailoring(APP)
        assertThat(navigator.state.stack.map { it::class }).containsExactly(
            JobResultNavKey::class,
            TailoringNavKey::class,
        ).inOrder()
    }

    @Test
    fun forwardingFromAQuestionAtTheRootStillOpensTailoring() {
        val navigator = navigatorOver(QuickQuestionNavKey(APP))
        QuickQuestionNavigation(navigator).forwardToTailoring(APP)
        assertThat(navigator.state.stack.last()).isInstanceOf(TailoringNavKey::class.java)
    }
}

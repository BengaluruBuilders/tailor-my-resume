package com.tailormyresume.feature.tailor.impl.tailoring

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.designsystem.component.content.TmrProgressState
import com.tailormyresume.core.model.QuickAnswer
import org.junit.Test

class TailoringRowCopyTest {

    @Test
    fun answerYesFewShowsAddingYourExample_otherwiseChecking() {
        val yes = QuickAnswer("req-1", "YES_REGULARLY")
        val few = QuickAnswer("req-1", "A_FEW_TIMES")
        val no = QuickAnswer("req-1", "NOT_REALLY")

        assertThat(yes.addsExample()).isTrue()
        assertThat(few.addsExample()).isTrue()
        assertThat(no.addsExample()).isFalse()
        assertThat((null as QuickAnswer?).addsExample()).isFalse()
        assertThat(tailoringRows(10, addsExample = true).map { it.kind }).containsExactly(
            TailoringRowKind.Matching,
            TailoringRowKind.Rewriting,
            TailoringRowKind.AddingExample,
            TailoringRowKind.Fitting,
        ).inOrder()
        assertThat(tailoringRows(10, addsExample = false).map { it.kind }).containsExactly(
            TailoringRowKind.Matching,
            TailoringRowKind.Rewriting,
            TailoringRowKind.CheckingMustHaves,
            TailoringRowKind.Fitting,
        ).inOrder()
    }

    @Test
    fun rowStatesFollowTheQuarters() {
        fun states(percent: Int) = tailoringRows(percent, addsExample = false).map { it.state }

        val d = TmrProgressState.Done
        val a = TmrProgressState.Active
        val p = TmrProgressState.Pending
        assertThat(states(0)).containsExactly(a, p, p, p).inOrder()
        assertThat(states(24)).containsExactly(a, p, p, p).inOrder()
        assertThat(states(25)).containsExactly(d, a, p, p).inOrder()
        assertThat(states(50)).containsExactly(d, d, a, p).inOrder()
        assertThat(states(75)).containsExactly(d, d, d, a).inOrder()
        assertThat(states(99)).containsExactly(d, d, d, a).inOrder()
        assertThat(states(100)).containsExactly(d, d, d, d).inOrder()
    }
}

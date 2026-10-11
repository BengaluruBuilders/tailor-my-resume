package com.tailormyresume.feature.analysis.impl.question

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrWholeWordText
import com.tailormyresume.core.designsystem.component.hero.TmrHeroCard
import com.tailormyresume.core.designsystem.component.hero.TmrHeroColor
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.input.TmrChoiceRow
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.component.input.TmrTextArea
import com.tailormyresume.core.designsystem.component.input.TmrTextButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.analysis.impl.R

private const val QUESTION_ART_FONT_SCALE = 1.3f

@Composable
internal fun QuickQuestionScreen(
    state: QuickQuestionUiState.Ready,
    onPick: (QuickChoice) -> Unit,
    onDetailChange: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuestionHero(
                question = state.question,
                eyebrow = stringResource(R.string.feature_analysis_impl_question_label),
                showArt = fontScale <= QUESTION_ART_FONT_SCALE,
            )
            TmrWholeWordText(
                text = state.why,
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickChoice.entries.forEach { choice ->
                    TmrChoiceRow(
                        label = choice.label(),
                        selected = state.picked == choice,
                        onClick = { onPick(choice) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.showDetail) {
                TmrTextArea(
                    value = state.detail,
                    onValueChange = onDetailChange,
                    label = stringResource(R.string.feature_analysis_impl_question_detail_label),
                    placeholder = stringResource(R.string.feature_analysis_impl_question_detail_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_analysis_impl_question_continue),
                onClick = onContinue,
                enabled = state.canContinue,
                onDisabledClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            TmrTextButton(
                label = stringResource(R.string.feature_analysis_impl_question_skip),
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QuestionHero(question: String, eyebrow: String, showArt: Boolean) {
    TmrHeroCard(
        color = TmrHeroColor.Lime,
        label = eyebrow,
        headline = question,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 230.dp)
            .semantics { text = AnnotatedString(eyebrow) },
    ) {
        if (!showArt) return@TmrHeroCard
        TmrPaige(
            TmrPaigePose.Question,
            Modifier
                .align(Alignment.BottomEnd)
                .offset(y = 40.dp),
        )
    }
}

@Composable
private fun QuickChoice.label(): String = when (this) {
    QuickChoice.YES_REGULARLY -> stringResource(R.string.feature_analysis_impl_question_yes_regularly)
    QuickChoice.A_FEW_TIMES -> stringResource(R.string.feature_analysis_impl_question_a_few_times)
    QuickChoice.NOT_YET -> stringResource(R.string.feature_analysis_impl_question_not_yet)
}

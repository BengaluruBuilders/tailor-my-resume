package com.tailormyresume.feature.settings.impl.credits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrDivider
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.settings.impl.R

internal const val CREDITS_LIST_TAG = "credits-list"

private val CardRadius = 20.dp
private val RowMinHeight = 68.dp
private val BalanceGap = 16.dp

@Composable
internal fun CreditsScreen(state: CreditsUiState, onBuyMore: () -> Unit, modifier: Modifier = Modifier) {
    val content = state as? CreditsUiState.Content ?: return
    val spacing = TmrTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(CREDITS_LIST_TAG),
        contentPadding = PaddingValues(start = spacing.gutter, end = spacing.gutter, bottom = spacing.xxxl),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.feature_settings_impl_credits_title),
                style = TmrTheme.typography.headlineSmall,
                color = TmrTheme.colors.text,
                modifier = Modifier.padding(bottom = spacing.md),
            )
        }
        item(key = "balance") {
            BalanceCard(credits = content.credits, onBuyMore = onBuyMore, modifier = Modifier.padding(bottom = spacing.md))
        }
        item(key = "history-label") {
            TmrSectionLabel(
                text = stringResource(R.string.feature_settings_impl_credits_history),
                modifier = Modifier.padding(start = spacing.xs, bottom = spacing.sm),
            )
        }
        itemsIndexed(content.history, key = { _, row -> row.key }) { index, row ->
            HistoryRow(row = row, first = index == 0, last = index == content.history.lastIndex)
        }
        item(key = "receipts") {
            Text(
                text = stringResource(R.string.feature_settings_impl_credits_receipts),
                style = TmrTheme.typography.caption,
                color = TmrTheme.colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
            )
        }
        if (content.receiptEmail.isNotBlank()) {
            item(key = "receipt-email") {
                Text(
                    text = content.receiptEmail,
                    style = TmrTheme.typography.caption,
                    color = TmrTheme.colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(credits: Int, onBuyMore: () -> Unit, modifier: Modifier = Modifier) {
    TmrCard(modifier = modifier) {
        BalanceHero(credits = credits)
        TmrPrimaryButton(
            label = stringResource(R.string.feature_settings_impl_credits_buy_more),
            onClick = onBuyMore,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BalanceHero(credits: Int) {
    val colors = TmrTheme.colors
    val numberStyle = TmrTheme.typography.displayLarge
    val captionStyle = TmrTheme.typography.body
    val caption = pluralStringResource(R.plurals.feature_settings_impl_credits_caption, credits)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gapPx = with(density) { BalanceGap.roundToPx() }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val sideBySide = remember(credits, caption, numberStyle, captionStyle, constraints.maxWidth, gapPx, density) {
            val numberWidth = measurer.measure(credits.toString(), numberStyle).size.width
            val longestWord = caption.split(' ').maxOf { measurer.measure(it, captionStyle).size.width }
            numberWidth + gapPx + longestWord <= constraints.maxWidth
        }
        val number: @Composable () -> Unit = {
            Text(text = credits.toString(), style = numberStyle, color = colors.lime)
        }
        val captionText: @Composable (Modifier) -> Unit = { captionModifier ->
            Text(text = caption, style = captionStyle, color = colors.textSecondary, modifier = captionModifier)
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(BalanceGap), verticalAlignment = Alignment.CenterVertically) {
                number()
                captionText(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                number()
                captionText(Modifier)
            }
        }
    }
}

@Composable
private fun HistoryRow(row: CreditsHistoryRow, first: Boolean, last: Boolean) {
    val colors = TmrTheme.colors
    val shape = RoundedCornerShape(
        topStart = if (first) CardRadius else 0.dp,
        topEnd = if (first) CardRadius else 0.dp,
        bottomStart = if (last) CardRadius else 0.dp,
        bottomEnd = if (last) CardRadius else 0.dp,
    )
    Column(modifier = Modifier.fillMaxWidth().background(colors.surfaceRaised, shape)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .padding(horizontal = TmrTheme.spacing.cardPadding, vertical = 13.dp)
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = row.title.text(), style = TmrTheme.typography.body, color = colors.text)
                Text(text = row.subtitle(), style = TmrTheme.typography.caption, color = colors.textMuted)
            }
            Text(
                text = row.amountText,
                style = TmrTheme.typography.mono15.copy(fontWeight = FontWeight.Bold),
                color = if (row.amount > 0) colors.lime else colors.text,
            )
        }
        if (!last) TmrDivider(color = colors.line, modifier = Modifier.padding(horizontal = TmrTheme.spacing.cardPadding))
    }
}

@Composable
private fun HistoryTitle.text(): String = when (this) {
    HistoryTitle.FreeFirstResume -> stringResource(R.string.feature_settings_impl_credits_free_first_resume)
    is HistoryTitle.BoughtCredits -> pluralStringResource(R.plurals.feature_settings_impl_credits_bought, credits, credits)
    is HistoryTitle.Tailored -> jobTitle
    HistoryTitle.TailoredResume -> stringResource(R.string.feature_settings_impl_credits_tailored_resume)
    HistoryTitle.Refund -> stringResource(R.string.feature_settings_impl_credits_refund)
    HistoryTitle.CarriedOver -> stringResource(R.string.feature_settings_impl_credits_carried_over)
}

@Composable
private fun CreditsHistoryRow.subtitle(): String = when (detail) {
    HistoryDetail.Welcome -> stringResource(
        R.string.feature_settings_impl_credits_detail_date,
        stringResource(R.string.feature_settings_impl_credits_welcome),
        date,
    )
    is HistoryDetail.Text -> stringResource(R.string.feature_settings_impl_credits_detail_date, detail.value, date)
    HistoryDetail.None -> date
}

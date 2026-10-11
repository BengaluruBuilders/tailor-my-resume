package com.tailormyresume.feature.settings.impl.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.component.hero.TmrHeroCard
import com.tailormyresume.core.designsystem.component.hero.TmrHeroColor
import com.tailormyresume.core.designsystem.component.hero.TmrPaige
import com.tailormyresume.core.designsystem.component.hero.TmrPaigePose
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.domain.ApplicationPack
import com.tailormyresume.feature.settings.impl.R

internal class PaywallActions(
    val onSelectPack: (String) -> Unit,
    val onBuy: () -> Unit,
    val onRestore: () -> Unit,
)

private const val ART_MAX_FONT_SCALE = 1.15f
internal const val PAYWALL_ART_TAG = "paywall-art"
private val HeroMinHeight = 236.dp
private val PackMinHeight = 68.dp
private val BadgeSize = 54.dp

@Composable
internal fun PaywallScreen(state: PaywallUiState, actions: PaywallActions, modifier: Modifier = Modifier) {
    val spacing = TmrTheme.spacing
    val enlarged = LocalDensity.current.fontScale > ART_MAX_FONT_SCALE
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            PaywallHero(credits = state.credits)
            Text(
                text = stringResource(R.string.feature_settings_impl_paywall_explainer),
                style = TmrTheme.typography.bodySmall,
                color = TmrTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = spacing.sm),
            )
            Column(
                modifier = Modifier.padding(top = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.packs.forEach { pack ->
                    PackRow(
                        pack = pack,
                        selected = pack.id == state.selectedPackId,
                        enabled = !state.busy,
                        onClick = { actions.onSelectPack(pack.id) },
                    )
                }
            }
            if (enlarged) PaywallBottomBar(state = state, actions = actions)
        }
        if (!enlarged) PaywallBottomBar(state = state, actions = actions)
    }
}

@Composable
private fun PaywallHero(credits: Int?) {
    val showArt = LocalDensity.current.fontScale <= ART_MAX_FONT_SCALE
    TmrHeroCard(
        color = TmrHeroColor.Amber,
        label = credits?.let { pluralStringResource(R.plurals.feature_settings_impl_paywall_credits_left, it, it) }
            ?: stringResource(R.string.feature_settings_impl_paywall_credits_unknown),
        headline = stringResource(R.string.feature_settings_impl_paywall_headline),
        modifier = Modifier.fillMaxWidth().heightIn(min = HeroMinHeight),
    ) {
        if (showArt) HeroArt()
    }
}

@Composable
private fun BoxScope.HeroArt() {
    val colors = TmrTheme.colors
    TmrPaige(
        pose = TmrPaigePose.Paywall,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp).offset(y = 50.dp).testTag(PAYWALL_ART_TAG),
    )
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 24.dp, bottom = 20.dp)
            .size(BadgeSize)
            .rotate(-10f)
            .background(colors.lime, CircleShape)
            .border(BorderStroke(2.5.dp, colors.ink), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.feature_settings_impl_paywall_plus_one),
            style = TmrTheme.typography.display.copy(fontSize = 18.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
            color = colors.ink,
        )
    }
}

@Composable
private fun PackRow(pack: ApplicationPack, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.statusRow
    val outline = if (selected) colors.lime else colors.line
    val best = pack.id == ApplicationPack.APPLICATION_PACK_FIFTEEN
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PackMinHeight)
                .background(if (selected) colors.limeSelected else colors.surface, shape)
                .border(BorderStroke(1.5.dp, outline), shape)
                .clip(shape)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(start = 16.dp, end = 16.dp, top = if (best && LocalDensity.current.fontScale > ART_MAX_FONT_SCALE) 32.dp else 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioDot(selected = selected, outline = outline)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = pluralStringResource(R.plurals.feature_settings_impl_paywall_pack_credits, pack.credits, pack.credits),
                    style = TmrTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                )
                Text(
                    text = stringResource(R.string.feature_settings_impl_paywall_per_resume, formatPricePerResume(pack)),
                    style = TmrTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
            Text(
                text = formatPackPrice(pack.priceInPaise, pack.currencyCode),
                style = TmrTheme.typography.mono15.copy(fontWeight = FontWeight.Bold),
                color = colors.text,
            )
        }
        if (best) BestValuePill(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-11).dp))
    }
}

@Composable
private fun RadioDot(selected: Boolean, outline: Color) {
    Box(
        modifier = Modifier.size(22.dp).border(BorderStroke(2.dp, outline), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(TmrTheme.colors.lime, CircleShape))
    }
}

@Composable
private fun BestValuePill(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.feature_settings_impl_paywall_best_value).uppercase(),
        style = TmrTheme.typography.label,
        color = TmrTheme.colors.ink,
        modifier = modifier
            .background(TmrTheme.colors.lime, CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun PaywallBottomBar(state: PaywallUiState, actions: PaywallActions, modifier: Modifier = Modifier) {
    val pack = state.selectedPack
    val label = when {
        state.busy -> stringResource(R.string.feature_settings_impl_paywall_processing)
        pack == null -> stringResource(R.string.feature_settings_impl_paywall_buy_idle)
        else -> pluralStringResource(
            R.plurals.feature_settings_impl_paywall_buy,
            pack.credits,
            pack.credits,
            formatPackPrice(pack.priceInPaise, pack.currencyCode),
        )
    }
    Column(
        modifier = modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TmrPrimaryButton(
            label = label,
            onClick = actions.onBuy,
            enabled = pack != null && !state.busy,
            onDisabledClick = if (pack == null && !state.busy) actions.onBuy else ({}),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(enabled = !state.busy, role = Role.Button, onClick = actions.onRestore),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_settings_impl_paywall_restore),
                style = TmrTheme.typography.caption,
                color = TmrTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

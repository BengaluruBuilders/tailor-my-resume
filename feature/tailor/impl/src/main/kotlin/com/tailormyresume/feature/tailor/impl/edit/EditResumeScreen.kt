package com.tailormyresume.feature.tailor.impl.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.feature.tailor.impl.R
import com.tailormyresume.feature.tailor.impl.result.ChangeSource

@Composable
internal fun EditResumeScreen(
    state: EditResumeUiState.Ready,
    onBulletChange: (bulletId: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
    onSummaryChange: (String) -> Unit = {},
    onSkillsChange: (String) -> Unit = {},
) {
    val colors = TmrTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_tailor_impl_edit_title),
            style = TmrTheme.typography.headline,
            color = colors.text,
        )
        if (state.fitsOnOnePage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.limeSelected, TmrTheme.shapes.card)
                    .border(1.dp, colors.lime, TmrTheme.shapes.card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TmrIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.lime,
                )
                Text(
                    text = stringResource(R.string.feature_tailor_impl_edit_fits),
                    style = TmrTheme.typography.body,
                    color = colors.lime,
                )
            }
        }
        TmrSectionLabel(text = stringResource(R.string.feature_tailor_impl_edit_summary_label))
        EditTextCard(initial = state.summary, label = stringResource(R.string.feature_tailor_impl_edit_summary_label), onChange = onSummaryChange)
        state.roles.forEach { role ->
            TmrSectionLabel(text = role.label)
            role.bullets.forEach { bullet ->
                EditBulletCard(bullet = bullet, onBulletChange = onBulletChange)
            }
        }
        TmrSectionLabel(text = stringResource(R.string.feature_tailor_impl_edit_skills_label))
        EditTextCard(initial = state.skills, label = stringResource(R.string.feature_tailor_impl_edit_skills_label), onChange = onSkillsChange)
    }
}

@Composable
private fun EditTextCard(initial: String, label: String, onChange: (String) -> Unit) {
    val colors = TmrTheme.colors
    var text by remember { mutableStateOf(initial) }
    TmrCard {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onChange(it)
            },
            textStyle = TmrTheme.typography.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.lime),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

@Composable
private fun EditBulletCard(
    bullet: EditBullet,
    onBulletChange: (bulletId: String, text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val id = bullet.id
    TmrCard(modifier = modifier) {
        Text(
            text = stringResource(bullet.source.sourceLabel()),
            style = TmrTheme.typography.caption,
            color = when (bullet.source) {
                ChangeSource.YourResume -> colors.lime
                ChangeSource.YourAnswer -> colors.amber
            },
        )
        if (id == null) {
            Text(
                text = bullet.text,
                style = TmrTheme.typography.body,
                color = colors.text,
            )
        } else {
            val bulletLabel = stringResource(R.string.feature_tailor_impl_edit_bullet_label)
            var text by remember(bullet.id) { mutableStateOf(bullet.text) }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onBulletChange(id, it)
                },
                textStyle = TmrTheme.typography.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.lime),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = bulletLabel },
            )
        }
    }
}

@Composable
private fun ChangeSource.sourceLabel(): Int = when (this) {
    ChangeSource.YourResume -> R.string.feature_tailor_impl_edit_source_resume
    ChangeSource.YourAnswer -> R.string.feature_tailor_impl_edit_source_answer
}

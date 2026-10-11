package com.tailormyresume.feature.onboarding.impl.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.TmrBottomActionBar
import com.tailormyresume.core.designsystem.component.chrome.LocalTmrToast
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrFitText
import com.tailormyresume.core.designsystem.component.content.TmrSectionLabel
import com.tailormyresume.core.designsystem.component.input.TmrPrimaryButton
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.analysis.api.navigation.JobNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.R
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.EditRoleNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey

private const val STACKED_MIN_FONT_SCALE = 1.3f

private val FILE_NAME_BREAKS = charArrayOf('_', '.', '-', '/')

private val EMAIL_BREAKS = charArrayOf('@', '.')

@Composable
internal fun ReviewScreen(
    state: ReviewUiState.Ready,
    onAddDateClick: (entryId: String) -> Unit,
    onContactClick: () -> Unit,
    onExperienceClick: () -> Unit,
    onSkillsClick: () -> Unit,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(state.sourceFileName)
            CompletenessCard(state.percent)
            if (state.gap != null) {
                RequiredBanner(state.gap, onAddDateClick)
            } else {
                DoneBanner()
            }
            if (state.showLinkedinRow) {
                LinkedinRow(onContactClick)
            }
            ContactCard(state.contact, onContactClick)
            ExperienceCard(state.roles, onExperienceClick)
            SkillsCard(state.skills, onSkillsClick)
            if (state.education.isNotEmpty()) {
                EducationCard(state.education)
            }
        }
        TmrBottomActionBar {
            TmrPrimaryButton(
                label = stringResource(R.string.feature_onboarding_impl_review_continue),
                onClick = onContinueClick,
                onDisabledClick = onContinueClick,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun ReviewRoute(
    viewModel: ReviewViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalTmrToast.current
    val missingEndDate = stringResource(R.string.feature_onboarding_impl_review_missing_end_date_toast)
    val saveFailed = stringResource(R.string.feature_onboarding_impl_review_save_failed_toast)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReviewEvent.Continue -> navigator.navigate(JobNavKey())
                ReviewEvent.NothingToReview -> navigator.replace(UploadNavKey())
                ReviewEvent.MissingEndDate -> toast.show(missingEndDate)
                ReviewEvent.SaveFailed -> toast.show(saveFailed)
            }
        }
    }
    when (val current = state) {
        ReviewUiState.Loading -> Box(modifier.fillMaxSize().background(TmrTheme.colors.background))
        is ReviewUiState.Ready -> ReviewScreen(
            state = current,
            onAddDateClick = { navigator.navigate(EditRoleNavKey(entryId = it)) },
            onContactClick = { navigator.navigate(EditContactNavKey()) },
            onExperienceClick = { navigator.navigate(ExperienceNavKey()) },
            onSkillsClick = { navigator.navigate(SkillsNavKey()) },
            onContinueClick = viewModel::onContinue,
            modifier = modifier,
        )
    }
}

private fun Modifier.readAs(value: String): Modifier = semantics { text = AnnotatedString(value) }

@Composable
private fun Header(sourceFileName: String?) {
    Column(
        Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_onboarding_impl_review_title),
            style = TmrTheme.typography.headline,
            color = TmrTheme.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        TmrFitText(
            text = if (sourceFileName != null) {
                stringResource(R.string.feature_onboarding_impl_review_source_file, sourceFileName)
            } else {
                stringResource(R.string.feature_onboarding_impl_review_source_none)
            },
            style = TmrTheme.typography.caption,
            color = TmrTheme.colors.textMuted,
            breakAfter = FILE_NAME_BREAKS,
        )
    }
}

@Composable
private fun CompletenessCard(percent: Int) {
    val colors = TmrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, TmrTheme.shapes.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "$percent%",
            style = TmrTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.lime,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = stringResource(R.string.feature_onboarding_impl_review_complete)
            Text(
                text = label.uppercase(),
                style = TmrTheme.typography.label,
                color = colors.textSecondary,
                modifier = Modifier.readAs(label),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.line),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = percent.coerceIn(0, 100) / 100f)
                        .height(4.dp)
                        .background(colors.lime),
                )
            }
        }
    }
}

@Composable
private fun RequiredBanner(gap: ReviewGap, onAddDateClick: (String) -> Unit) {
    val colors = TmrTheme.colors
    val required = stringResource(R.string.feature_onboarding_impl_review_required)
    val message: @Composable () -> Unit = {
        Text(
            text = required.uppercase(),
            style = TmrTheme.typography.label,
            color = colors.ink,
            modifier = Modifier.readAs(required),
        )
        Text(
            text = stringResource(R.string.feature_onboarding_impl_review_end_date_missing, gap.title),
            style = TmrTheme.typography.strongLarge.copy(fontSize = 14.sp, lineHeight = 19.sp),
            color = colors.ink,
        )
    }
    val addDate: @Composable () -> Unit = {
        Box(
            Modifier
                .heightIn(min = 44.dp)
                .clip(CircleShape)
                .background(colors.ink, CircleShape)
                .clickable(role = Role.Button) { onAddDateClick(gap.entryId) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_onboarding_impl_review_add_date),
                style = TmrTheme.typography.button,
                color = colors.text,
            )
        }
    }
    val bannerModifier = Modifier
        .fillMaxWidth()
        .background(colors.amber, TmrTheme.shapes.card)
        .padding(horizontal = 16.dp, vertical = 14.dp)
    if (LocalDensity.current.fontScale >= STACKED_MIN_FONT_SCALE) {
        Column(bannerModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { message() }
            addDate()
        }
    } else {
        Row(bannerModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { message() }
            addDate()
        }
    }
}

@Composable
private fun DoneBanner() {
    val colors = TmrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.lime, TmrTheme.shapes.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = TmrIcons.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.feature_onboarding_impl_review_all_done),
            style = TmrTheme.typography.strongLarge.copy(fontSize = 14.sp),
            color = colors.ink,
        )
    }
}

@Composable
private fun LinkedinRow(onClick: () -> Unit) {
    val colors = TmrTheme.colors
    val shape = TmrTheme.shapes.card
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surfaceRaised, shape)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = colors.lineHigher,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(20.dp.toPx() - stroke / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.feature_onboarding_impl_review_add_linkedin),
            style = TmrTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        val optional = stringResource(R.string.feature_onboarding_impl_review_optional)
        Text(
            text = optional.uppercase(),
            style = TmrTheme.typography.label,
            color = colors.textMuted,
            modifier = Modifier.readAs(optional),
        )
    }
}

@Composable
private fun EditableCard(
    label: String,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clip(TmrTheme.shapes.card).clickable(role = Role.Button, onClick = onClick) else Modifier
    TmrCard(modifier = clickable) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TmrSectionLabel(text = label, modifier = Modifier.weight(1f))
            if (onClick != null) {
                Text(
                    text = stringResource(R.string.feature_onboarding_impl_review_edit),
                    style = TmrTheme.typography.label,
                    color = TmrTheme.colors.lime,
                )
            }
        }
        content()
    }
}

@Composable
private fun ContactCard(contact: ReviewContact, onClick: () -> Unit) {
    EditableCard(stringResource(R.string.feature_onboarding_impl_review_contact), onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (contact.name.isNotBlank()) {
                TmrFitText(
                    text = contact.name,
                    style = TmrTheme.typography.strongLarge.copy(fontSize = 18.sp),
                    color = TmrTheme.colors.text,
                )
            }
            listOf(contact.email, contact.phoneAndCity).filter(String::isNotBlank).forEach {
                TmrFitText(text = it, style = TmrTheme.typography.bodySmall, color = TmrTheme.colors.textSecondary, breakAfter = EMAIL_BREAKS)
            }
        }
    }
}

@Composable
private fun ExperienceCard(roles: List<ReviewRole>, onClick: () -> Unit) {
    EditableCard(stringResource(R.string.feature_onboarding_impl_review_experience, roles.size), onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            roles.forEach { RoleRow(it) }
        }
    }
}

@Composable
private fun RoleRow(role: ReviewRole) {
    val colors = TmrTheme.colors
    val dates = role.dates.label()
    val datesColor = if (role.dates is ReviewDates.EndMissing) colors.amber else colors.textMuted
    val details: @Composable () -> Unit = {
        TmrFitText(text = role.title, style = TmrTheme.typography.strongLarge, color = colors.text)
        if (role.organization.isNotBlank()) {
            TmrFitText(text = role.organization, style = TmrTheme.typography.caption, color = colors.textMuted)
        }
    }
    val datesText: @Composable () -> Unit = {
        if (dates != null) Text(text = dates, style = TmrTheme.typography.label.copy(letterSpacing = 0.sp, fontSize = 12.sp), color = datesColor)
    }
    if (LocalDensity.current.fontScale >= STACKED_MIN_FONT_SCALE) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            details()
            datesText()
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { details() }
            datesText()
        }
    }
}

@Composable
private fun ReviewDates.label(): String? = when (this) {
    ReviewDates.None -> null
    is ReviewDates.Range -> stringResource(R.string.feature_onboarding_impl_review_dates_range, start, end)
    is ReviewDates.Current -> stringResource(R.string.feature_onboarding_impl_review_dates_current, start)
    is ReviewDates.EndMissing -> stringResource(R.string.feature_onboarding_impl_review_dates_end_missing, start)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsCard(skills: ReviewSkills, onClick: () -> Unit) {
    val colors = TmrTheme.colors
    EditableCard(stringResource(R.string.feature_onboarding_impl_review_skills, skills.count), onClick) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            skills.chips.forEach { SkillChip(it, colors.text, colors.fill, Color.Transparent) }
            if (skills.more > 0) {
                SkillChip(
                    stringResource(R.string.feature_onboarding_impl_review_skills_more, skills.more),
                    colors.textMuted,
                    Color.Transparent,
                    colors.lineHigher,
                )
            }
        }
    }
}

@Composable
private fun SkillChip(label: String, content: Color, fill: Color, border: Color) {
    Box(
        Modifier
            .background(fill, CircleShape)
            .drawBehind {
                if (border != Color.Transparent) {
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        color = border,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(width = stroke),
                    )
                }
            }
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        TmrFitText(text = label, style = TmrTheme.typography.caption, color = content)
    }
}

@Composable
private fun EducationCard(education: List<ReviewEducation>) {
    EditableCard(stringResource(R.string.feature_onboarding_impl_review_education), onClick = null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            education.forEach {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TmrFitText(text = it.title, style = TmrTheme.typography.strongLarge, color = TmrTheme.colors.text)
                    if (it.detail.isNotBlank()) {
                        TmrFitText(text = it.detail, style = TmrTheme.typography.caption, color = TmrTheme.colors.textMuted)
                    }
                }
            }
        }
    }
}

package com.tailormyresume.feature.profile.impl.overview

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tailormyresume.core.designsystem.component.content.TmrCard
import com.tailormyresume.core.designsystem.component.content.TmrInitialDisc
import com.tailormyresume.core.designsystem.component.content.TmrListRow
import com.tailormyresume.core.designsystem.component.content.TmrTag
import com.tailormyresume.core.designsystem.component.input.TmrTextButton
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.profile.api.navigation.EditContactNavKey
import com.tailormyresume.feature.profile.api.navigation.ExperienceNavKey
import com.tailormyresume.feature.profile.api.navigation.ListEditNavKey
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import com.tailormyresume.feature.profile.api.navigation.SkillsNavKey
import com.tailormyresume.feature.profile.impl.R
import com.tailormyresume.feature.settings.api.navigation.SettingsNavKey

internal enum class ProfileTarget { SETTINGS, CONTACT, SUMMARY, EXPERIENCE, EDUCATION, SKILLS, ACHIEVEMENTS, LINKEDIN, REPLACE }

internal fun ProfileTarget.navKey(): NavKey = when (this) {
    ProfileTarget.SETTINGS -> SettingsNavKey()
    ProfileTarget.CONTACT, ProfileTarget.LINKEDIN -> EditContactNavKey()
    ProfileTarget.SUMMARY -> ListEditNavKey(ProfileListSection.SUMMARY)
    ProfileTarget.EDUCATION -> ListEditNavKey(ProfileListSection.EDUCATION)
    ProfileTarget.ACHIEVEMENTS -> ListEditNavKey(ProfileListSection.ACHIEVEMENTS)
    ProfileTarget.EXPERIENCE -> ExperienceNavKey()
    ProfileTarget.SKILLS -> SkillsNavKey()
    ProfileTarget.REPLACE -> UploadNavKey()
}

internal fun Navigator.open(target: ProfileTarget) = navigate(target.navKey())

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    onOpen: (ProfileTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val typography = TmrTheme.typography
    val spacing = TmrTheme.spacing
    val content = state as? ProfileUiState.Content
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.feature_profile_impl_title),
                style = typography.headlineSmall,
                color = colors.text,
            )
            TmrTextButton(
                label = stringResource(R.string.feature_profile_impl_settings),
                onClick = { onOpen(ProfileTarget.SETTINGS) },
            )
        }
        if (content != null) {
            val subtitle = listOfNotNull(
                content.headline.takeIf { it.isNotBlank() },
                content.years?.let { pluralStringResource(R.plurals.feature_profile_impl_years, it, it) },
                content.city.takeIf { it.isNotBlank() },
            ).joinToString(stringResource(R.string.feature_profile_impl_subtitle_separator))
            TmrCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    TmrInitialDisc(initial = content.initials, color = colors.lime)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(
                            text = content.name,
                            style = typography.strongLarge,
                            color = colors.text,
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = typography.caption,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(colors.fill, TmrTheme.shapes.pill),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(content.percent / 100f)
                                .height(4.dp)
                                .background(colors.lime, TmrTheme.shapes.pill),
                        )
                    }
                    Text(
                        text = stringResource(R.string.feature_profile_impl_percent_complete, content.percent),
                        style = typography.caption,
                        color = colors.text,
                    )
                }
            }
            TmrCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_contact),
                        onClick = { onOpen(ProfileTarget.CONTACT) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_summary),
                        onClick = { onOpen(ProfileTarget.SUMMARY) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_experience),
                        meta = content.experienceCount.toString(),
                        onClick = { onOpen(ProfileTarget.EXPERIENCE) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_education),
                        meta = content.educationCount.toString(),
                        onClick = { onOpen(ProfileTarget.EDUCATION) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_skills),
                        meta = content.skillsCount.toString(),
                        onClick = { onOpen(ProfileTarget.SKILLS) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_achievements),
                        meta = content.achievementsCount.toString(),
                        onClick = { onOpen(ProfileTarget.ACHIEVEMENTS) },
                    )
                    TmrListRow(
                        label = stringResource(R.string.feature_profile_impl_row_linkedin),
                        tag = if (content.linkedinMissing) TmrTag.Add else null,
                        meta = if (content.linkedinMissing) {
                            null
                        } else {
                            stringResource(R.string.feature_profile_impl_linkedin_added)
                        },
                        onClick = { onOpen(ProfileTarget.LINKEDIN) },
                        showDivider = false,
                    )
                }
            }
            content.sourceFileName?.let { fileName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onOpen(ProfileTarget.REPLACE) }
                        .heightIn(min = 48.dp)
                        .padding(vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Icon(
                        imageVector = TmrIcons.File,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.feature_profile_impl_built_from, fileName),
                        style = typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.feature_profile_impl_replace),
                        style = typography.caption,
                        color = colors.lime,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfileRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state = state, onOpen = navigator::open, modifier = modifier)
}

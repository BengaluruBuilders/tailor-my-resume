package com.tailormyresume.core.designsystem.component.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrTab { Applications, Profile }

@Composable
private fun RowScope.TmrTabItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    index: Int,
    onClick: () -> Unit,
) {
    val enlarged = LocalDensity.current.fontScale > 1f
    val tint = if (selected) TmrTheme.colors.lime else TmrTheme.colors.textDisabled
    Column(
        modifier =
        (if (enlarged) Modifier.weight(1f) else Modifier.widthIn(min = 100.dp))
            .heightIn(min = TmrTheme.spacing.tabItem)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics {
                collectionItemInfo = CollectionItemInfo(rowIndex = 0, rowSpan = 1, columnIndex = index, columnSpan = 1)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TmrTheme.spacing.xs, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        TmrCapsText(text = label, style = TmrTheme.typography.label, color = tint)
    }
}

@Composable
fun TmrTabBar(
    selected: TmrTab,
    onApplications: () -> Unit,
    onAdd: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val spacing = TmrTheme.spacing
    Column(modifier = modifier) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.tabDivider)
                .testTag(TmrChromeTags.TAB_DIVIDER),
        )
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(top = spacing.sm, bottom = 28.dp, start = spacing.xxl, end = spacing.xxl)
                .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = 2) },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TmrTabItem(
                selected = selected == TmrTab.Applications,
                label = stringResource(R.string.core_designsystem_chrome_tab_applications),
                selectedIcon = TmrIcons.Applications,
                unselectedIcon = TmrIcons.ApplicationsBorder,
                index = 0,
                onClick = onApplications,
            )
            val addDescription = stringResource(R.string.core_designsystem_chrome_add_application)
            Box(
                modifier =
                Modifier
                    .size(spacing.tabCentreDisc)
                    .background(colors.lime, CircleShape)
                    .clickable(role = Role.Button, onClick = onAdd)
                    .semantics { contentDescription = addDescription }
                    .testTag(TmrChromeTags.TAB_ADD),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TmrIcons.Plus,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colors.ink,
                )
            }
            TmrTabItem(
                selected = selected == TmrTab.Profile,
                label = stringResource(R.string.core_designsystem_chrome_tab_profile),
                selectedIcon = TmrIcons.Profile,
                unselectedIcon = TmrIcons.ProfileBorder,
                index = 1,
                onClick = onProfile,
            )
        }
    }
}

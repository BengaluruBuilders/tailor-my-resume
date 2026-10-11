package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.icon.TmrIcons
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val FileTileSize = 44.dp

private val FileIconSize = 18.dp

private val FileTileCorner = 12.dp

private val FILE_NAME_SEPARATORS = charArrayOf('_', '.', '-', '/')

@Composable
fun TmrFileCard(
    fileName: String,
    meta: String,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TmrCard(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(FileTileSize)
                    .background(TmrTheme.colors.fill, RoundedCornerShape(FileTileCorner))
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TmrIcons.File,
                    contentDescription = null,
                    tint = TmrTheme.colors.text,
                    modifier = Modifier.size(FileIconSize),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TmrFitText(
                    text = fileName,
                    style = TmrTheme.typography.strongLarge,
                    color = TmrTheme.colors.text,
                    breakAfter = FILE_NAME_SEPARATORS,
                )
                Text(
                    text = meta,
                    style = TmrTheme.typography.caption,
                    color = TmrTheme.colors.textMuted,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FileCardButton(
                label = stringResource(R.string.core_designsystem_content_file_share),
                description = stringResource(R.string.core_designsystem_content_file_share_description, fileName),
                onClick = onShare,
                modifier = Modifier.weight(1f),
            )
            FileCardButton(
                label = stringResource(R.string.core_designsystem_content_file_open),
                description = stringResource(R.string.core_designsystem_content_file_open_description, fileName),
                onClick = onOpen,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FileCardButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .defaultMinSize(minWidth = 48.dp)
            .clip(TmrTheme.shapes.pill)
            .background(TmrTheme.colors.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TmrTheme.typography.button,
            color = TmrTheme.colors.text,
        )
    }
}

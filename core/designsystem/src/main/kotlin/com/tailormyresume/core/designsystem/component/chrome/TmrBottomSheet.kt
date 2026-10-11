package com.tailormyresume.core.designsystem.component.chrome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrTheme

private val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

@Composable
fun TmrBottomSheet(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = TmrTheme.spacing
    val motion = TmrTheme.motion
    val progress = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(motion.reduced) {
        if (motion.reduced) progress.snapTo(1f) else progress.animateTo(1f, motion.proofSpecs.spatial)
    }
    BackHandler(onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TmrTheme.colors.scrim)
                .testTag(TmrChromeTags.SCRIM)
                .then(
                    if (dismissible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = stringResource(R.string.core_designsystem_chrome_dismiss),
                            role = Role.Button,
                            onClick = onDismiss,
                        )
                    } else {
                        Modifier
                    },
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = (1f - progress.value) * size.height }
                .fillMaxWidth()
                .testTag(TmrChromeTags.SHEET)
                .clip(SheetShape)
                .background(TmrTheme.colors.sheet)
                .pointerInput(Unit) { detectTapGestures { } }
                .semantics { paneTitle = title }
                .padding(top = spacing.sheetPaddingTop),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(spacing.sheetHandleWidth, spacing.sheetHandleHeight)
                    .background(
                        TmrTheme.colors.lineHigher,
                        RoundedCornerShape(2.dp),
                    )
                    .testTag(TmrChromeTags.SHEET_HANDLE),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.sheetPaddingHorizontal,
                        end = spacing.sheetPaddingHorizontal,
                        top = 16.dp,
                        bottom = spacing.sheetPaddingBottom,
                    ).testTag(TmrChromeTags.SHEET_CONTENT),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

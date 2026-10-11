package com.tailormyresume.core.designsystem.component

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme
import kotlin.math.roundToInt

val LocalTmrBottomInset = compositionLocalOf { 0.dp }

private const val TMR_COMPACT_HEIGHT_DP = 480

private enum class TmrScreenSlot { Header, Sheet, Content, Bottom, Notice, Snackbar, Action }

@Composable
fun TmrScreen(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    lightTop: Boolean = header == null,
    sheet: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingAction: (@Composable () -> Unit)? = null,
    bottomBarNotice: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = TmrTheme.colors
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockInset = LocalTmrBottomInset.current
    val resting = navBottom + dockInset
    val gutter = TmrTheme.spacing.gutter
    val sheetTop = TmrTheme.spacing.d24
    val collapse = rememberTmrHeaderCollapseState()
    TmrStatusBarIcons(darkIcons = tmrDarkStatusBarIcons(lightTop, colors))
    SubcomposeLayout(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(collapse.connection)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { collapse.consume(it) },
                onDragStopped = { collapse.settle() },
            )
            .background(if (sheet) colors.background else colors.ground),
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = Constraints(maxWidth = width, maxHeight = height)
        val headerItem = header?.let { subcompose(TmrScreenSlot.Header, it).firstOrNull() }
        val headerPlaceable = headerItem?.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = height))
        val data = headerItem?.layoutId as? TmrHeaderData
        val compact = header != null && height < TMR_COMPACT_HEIGHT_DP.dp.roundToPx() && data?.drawsAboveContent != true
        val bottoms = bottomBar?.let { subcompose(TmrScreenSlot.Bottom, it) }.orEmpty().map { it.measure(loose) }
        val barHeight = bottoms.maxOfOrNull { it.height } ?: 0
        val notices = bottomBarNotice?.let {
            subcompose(TmrScreenSlot.Notice) { TmrBarNotice(it) }
        }.orEmpty().map { it.measure(loose) }
        val noticeHeight = notices.maxOfOrNull { it.height } ?: 0
        val floor = if (barHeight > 0) barHeight else resting.roundToPx()
        val reserved = if (barHeight > 0 || dockInset > 0.dp || noticeHeight > 0) floor + noticeHeight else 0
        val contentTop = if (headerPlaceable == null) {
            0
        } else {
            (headerPlaceable.height - (data?.overlap ?: 0.dp).roundToPx()).coerceAtLeast(0)
        }
        collapse.range = if (compact) {
            (contentTop - statusTop.roundToPx()).coerceAtLeast(0).toFloat()
        } else {
            0f
        }
        val shift = if (compact) collapse.offset.roundToInt() else 0
        val padding = PaddingValues(
            top = if (headerPlaceable == null) {
                statusTop
            } else if (sheet) {
                sheetTop
            } else {
                0.dp
            },
            bottom = if (reserved > 0) gutter else resting,
        )
        val fullArea = Constraints.fixed(width, (height - contentTop - shift).coerceAtLeast(0))
        val area = Constraints.fixed(width, (height - contentTop - shift - reserved).coerceAtLeast(0))
        val sheetItems = if (sheet && headerPlaceable != null) {
            subcompose(TmrScreenSlot.Sheet) { TmrSheet(Modifier.fillMaxSize()) {} }.map { it.measure(fullArea) }
        } else {
            emptyList()
        }
        val contents = subcompose(TmrScreenSlot.Content) { content(padding) }.map { it.measure(area) }
        val snacks = subcompose(TmrScreenSlot.Snackbar, snackbarHost).map { it.measure(loose) }
        val actions = floatingAction?.let { subcompose(TmrScreenSlot.Action, it) }.orEmpty().map { it.measure(loose) }
        layout(width, height) {
            val aboveContent = data?.drawsAboveContent == true
            if (!aboveContent) headerPlaceable?.place(0, shift)
            sheetItems.forEach { it.place(0, contentTop + shift) }
            contents.forEach { it.place(0, contentTop + shift) }
            if (aboveContent) headerPlaceable?.place(0, 0)
            bottoms.forEach { it.place((width - it.width) / 2, height - it.height) }
            notices.forEach { it.place(0, height - floor - it.height) }
            val lift = floor + noticeHeight
            snacks.forEach { it.place((width - it.width) / 2, height - lift - it.height) }
            actions.forEach { it.place(width - it.width, height - lift - it.height - snacks.heightSum()) }
        }
    }
}

internal fun tmrDarkStatusBarIcons(lightTop: Boolean, colors: TmrColors): Boolean =
    (if (lightTop) colors.ground else colors.header).luminance() > 0.5f

@Composable
private fun TmrStatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}

@Composable
private fun TmrBarNotice(notice: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = TmrTheme.spacing.gutter).padding(bottom = TmrTheme.spacing.sm)) {
        notice()
    }
}

private fun List<Placeable>.heightSum(): Int = sumOf { it.height }

@Preview(showBackground = true)
@Composable
private fun TmrScreenDarkPreview() {
    TmrPreviewTheme { TmrScreenSample() }
}

@Composable
private fun TmrScreenSample() {
    TmrScreen(
        header = { TmrInnerHeader(title = "Applications", onBack = {}) },
        bottomBar = { TmrBottomActionBar { TmrPrimaryButton(label = "Export", onClick = {}) } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding))
    }
}

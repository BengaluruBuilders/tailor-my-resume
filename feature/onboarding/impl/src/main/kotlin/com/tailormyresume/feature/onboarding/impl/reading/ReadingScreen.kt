package com.tailormyresume.feature.onboarding.impl.reading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tailormyresume.core.designsystem.component.content.TmrProgressRow
import com.tailormyresume.core.designsystem.component.content.TmrProgressRows
import com.tailormyresume.core.designsystem.component.content.TmrProgressState
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.navigation.Navigator
import com.tailormyresume.feature.onboarding.api.navigation.ReviewProfileNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadErrorNavKey
import com.tailormyresume.feature.onboarding.api.navigation.UploadNavKey
import com.tailormyresume.feature.onboarding.impl.R
import com.tailormyresume.feature.onboarding.impl.importresume.ResumeFileCard
import com.tailormyresume.feature.onboarding.impl.importresume.resumeFileMeta

@Composable
private fun readingRowLabel(kind: ReadingRowKind): String =
    when (kind) {
        ReadingRowKind.Contact -> stringResource(R.string.feature_onboarding_impl_reading_row_contact)
        ReadingRowKind.Experience -> stringResource(R.string.feature_onboarding_impl_reading_row_experience)
        ReadingRowKind.Education -> stringResource(R.string.feature_onboarding_impl_reading_row_education)
        ReadingRowKind.Skills -> stringResource(R.string.feature_onboarding_impl_reading_row_skills)
        ReadingRowKind.Achievements -> stringResource(R.string.feature_onboarding_impl_reading_row_achievements)
    }

@Composable
private fun readingRowProgressState(state: ReadingRowState): TmrProgressState =
    when (state) {
        ReadingRowState.Done -> TmrProgressState.Done
        ReadingRowState.Active -> TmrProgressState.Active
        ReadingRowState.Pending -> TmrProgressState.Pending
    }

@Composable
private fun readingRowMeta(row: ReadingRowUi): String? {
    val count = row.count ?: 0
    return when (row.state) {
        ReadingRowState.Pending -> null
        ReadingRowState.Active -> stringResource(R.string.feature_onboarding_impl_reading_meta_reading)
        ReadingRowState.Done ->
            when (row.kind) {
                ReadingRowKind.Contact ->
                    if (count > 0) {
                        stringResource(R.string.feature_onboarding_impl_reading_meta_found)
                    } else {
                        pluralStringResource(R.plurals.feature_onboarding_impl_reading_meta_found_count, 0, 0)
                    }
                ReadingRowKind.Experience ->
                    pluralStringResource(
                        R.plurals.feature_onboarding_impl_reading_meta_roles,
                        count,
                        count,
                    )
                ReadingRowKind.Education ->
                    pluralStringResource(
                        R.plurals.feature_onboarding_impl_reading_meta_degrees,
                        count,
                        count,
                    )
                ReadingRowKind.Skills ->
                    pluralStringResource(R.plurals.feature_onboarding_impl_reading_meta_found_count, count, count)
                ReadingRowKind.Achievements ->
                    pluralStringResource(R.plurals.feature_onboarding_impl_reading_meta_found_count, count, count)
            }
    }
}

@Composable
private fun readingProgressRows(state: ReadingUiState): TmrProgressRowsData {
    val metaBesideLabel = LocalDensity.current.fontScale < META_BESIDE_LABEL_MAX_FONT_SCALE
    return TmrProgressRowsData(
        rows = state.rows.map { row ->
            val label = readingRowLabel(row.kind)
            val meta = readingRowMeta(row)
            if (metaBesideLabel || meta == null) {
                TmrProgressRow(label = label, state = readingRowProgressState(row.state), meta = meta)
            } else {
                TmrProgressRow(label = "$label · $meta", state = readingRowProgressState(row.state))
            }
        },
        percent = state.percent,
    )
}

private const val META_BESIDE_LABEL_MAX_FONT_SCALE = 1.3f

private data class TmrProgressRowsData(val rows: List<TmrProgressRow>, val percent: Int)

@Composable
internal fun ReadingScreen(
    state: ReadingUiState,
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
                .padding(start = 14.dp, end = 14.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_onboarding_impl_reading_title),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TmrTheme.typography.headline,
                color = TmrTheme.colors.text,
            )
            val fileName = state.file.name
            if (fileName != null) {
                ResumeFileCard(
                    name = fileName,
                    meta = resumeFileMeta(
                        mimeType = state.file.mimeType,
                        fileName = fileName,
                        byteSize = state.file.byteSize,
                    ),
                )
            } else {
                ResumeFileCard(
                    name = stringResource(R.string.feature_onboarding_impl_reading_pasted_name),
                    meta = pluralStringResource(
                        R.plurals.feature_onboarding_impl_reading_pasted_meta,
                        state.file.characters,
                        state.file.characters,
                    ),
                )
            }
            val progress = readingProgressRows(state)
            TmrProgressRows(rows = progress.rows, percent = progress.percent)
        }
    }
}

@Composable
internal fun ReadingRoute(
    viewModel: ReadingViewModel,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = true) {}
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReadingEvent.Done -> navigator.replace(ReviewProfileNavKey())
                ReadingEvent.Failed -> navigator.replace(UploadErrorNavKey())
                ReadingEvent.NothingToRead -> navigator.replace(UploadNavKey())
            }
        }
    }
    ReadingScreen(state = state, modifier = modifier)
}

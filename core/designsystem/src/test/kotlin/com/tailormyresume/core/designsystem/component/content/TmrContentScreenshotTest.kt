package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevice
import com.tailormyresume.core.screenshot.TmrTestDevices
import com.tailormyresume.core.screenshot.captureForDevice
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrContentScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        screenName: String,
        device: TmrTestDevice = TmrTestDevices.prototype,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            TmrPreviewTheme {
                if (device.fontScale == TmrTestDevices.DEFAULT_FONT_SCALE) {
                    content()
                } else {
                    CompositionLocalProvider(
                        LocalDensity provides Density(LocalDensity.current.density, device.fontScale),
                        content = content,
                    )
                }
            }
        }
        runBlocking {
            composeRule.captureForDevice(
                outputDirectory = SCREENSHOT_DIRECTORY,
                screenName = screenName,
                device = device,
            )
        }
    }

    @Test
    fun listRow() = capture("content_listrow") { ListRows() }

    @Test
    fun listRowLargeFont() = capture(
        screenName = "content_listrow_font200",
        device = TmrTestDevices.prototypeLargeFont,
        content = { ListRows() },
    )

    @Test
    fun card() = capture("content_card") { CardSample() }

    @Test
    fun sectionLabel() = capture("content_sectionlabel") { SectionLabelSample() }

    @Test
    fun statusChip() = capture("content_statuschip") { StatusChips() }

    @Test
    fun keywordChips() = capture("content_chip_keyword") { KeywordChips() }

    @Test
    fun sourceChips() = capture("content_chip_source") { SourceChips() }

    @Test
    fun tagChips() = capture("content_tag") { TagChips() }

    @Test
    fun progressZero() = capture("content_progress_0") { Progress(percent = 0, rows = PROGRESS_ROWS_ZERO) }

    @Test
    fun progressForty() = capture("content_progress_40") { Progress(percent = 40, rows = PROGRESS_ROWS_FORTY) }

    @Test
    fun progressHundred() = capture("content_progress_100") { Progress(percent = 100, rows = PROGRESS_ROWS_HUNDRED) }

    @Test
    fun progressLargeFont() = capture(
        screenName = "content_progress_font200",
        device = TmrTestDevices.prototypeLargeFont,
        content = { Progress(percent = 40, rows = PROGRESS_ROWS_FORTY) },
    )

    @Test
    fun statusBar() = capture("content_statusbar") { StatusBar() }

    @Test
    fun initialDiscs() = capture("content_initialdisc") { InitialDiscs() }

    @Test
    fun storyBars() = capture("content_storybars") { StoryBars() }

    @Test
    fun resumePaper() = capture("content_resume_paper") { ResumePaper() }

    @Test
    fun fileCard() = capture("content_filecard") { FileCard() }

    @Test
    fun soonRows() = capture("content_soonrow") { SoonRows() }

    @Test
    fun coverageDelta() = capture("content_coverage_delta") { CoverageDelta() }

    private companion object {
        const val SCREENSHOT_DIRECTORY = "src/test/screenshots"
    }
}

@Composable
private fun ListRows() {
    ScreenColumn {
        TmrListRow(label = "Experience", meta = "3", onClick = {})
        TmrListRow(label = "Skills", meta = "7", onClick = {})
        TmrListRow(
            label = "LinkedIn",
            tag = TmrTag.Add,
            onClick = {},
            showDivider = false,
        )
    }
}

@Composable
private fun CardSample() {
    ScreenColumn {
        TmrCard {
            TmrSectionLabel(text = "Tailored resumes")
            Text(
                text = "Built from your confirmed facts and the job description.",
                style = TmrTheme.typography.body,
                color = TmrTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SectionLabelSample() {
    ScreenColumn {
        TmrSectionLabel(text = "Tailored resumes")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips() {
    ScreenColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TmrApplicationStatus.entries.forEach { status ->
                TmrStatusChip(status = status)
            }
        }
    }
}

@Composable
private fun KeywordChips() {
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TmrKeywordChip(label = "Have SQL", state = TmrKeywordState.Have)
            TmrKeywordChip(label = "Have Power BI", state = TmrKeywordState.Have)
            TmrKeywordChip(label = "Missing Variance analysis", state = TmrKeywordState.Missing)
        }
    }
}

@Composable
private fun SourceChips() {
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TmrSource.entries.forEach { source ->
                TmrSourceChip(source = source)
            }
        }
    }
}

@Composable
private fun TagChips() {
    ScreenColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TmrTag.entries.forEach { tag ->
            TmrTagChip(tag = tag)
        }
    }
}

@Composable
private fun Progress(percent: Int, rows: List<TmrProgressRow>) {
    ScreenColumn {
        TmrProgressRows(rows = rows, percent = percent)
    }
}

@Composable
private fun StatusBar() {
    ScreenColumn {
        TmrStatusBar(
            counts = mapOf(
                TmrApplicationStatus.Saved to 2,
                TmrApplicationStatus.Applied to 1,
                TmrApplicationStatus.Interview to 1,
                TmrApplicationStatus.Offer to 0,
                TmrApplicationStatus.Rejected to 1,
            ),
        )
    }
}

@Composable
private fun InitialDiscs() {
    ScreenColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TmrInitialDisc(initial = "N", color = TmrTheme.colors.amber)
            TmrInitialDisc(initial = "K", color = TmrTheme.colors.lime)
            TmrInitialDisc(initial = "H", color = TmrTheme.colors.blue)
        }
    }
}

@Composable
private fun StoryBars() {
    ScreenColumn {
        Box(
            Modifier
                .fillMaxWidth()
                .background(TmrTheme.colors.blue, TmrTheme.shapes.cardLarge)
                .padding(horizontal = 26.dp, vertical = 12.dp),
        ) {
            TmrStoryBars(count = 3, activeIndex = 1, activeFraction = 0.5f)
        }
    }
}

@Composable
private fun ResumePaper() {
    ScreenColumn {
        TmrResumePaper(
            name = "Priya Deshmukh",
            contact = "priya@example.com, Pune, India",
            coveragePercent = 92,
            blocks = listOf(
                TmrPaperBlock(
                    heading = "Experience",
                    title = "Business Analyst",
                    dates = "2022 - Now",
                    lines = listOf(
                        listOf(
                            TmrPaperSpan(text = "Built "),
                            TmrPaperSpan(text = "Power BI", highlight = TmrPaperHighlight.FromResume),
                            TmrPaperSpan(text = " dashboards for reporting"),
                        ),
                        listOf(
                            TmrPaperSpan(
                                text = "Presented monthly variance analysis to the CFO",
                                highlight = TmrPaperHighlight.FromAnswer,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Composable
private fun FileCard() {
    ScreenColumn {
        TmrFileCard(
            fileName = "Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf",
            meta = "PDF, 1 page, 48 KB",
            onShare = {},
            onOpen = {},
        )
    }
}

@Composable
private fun SoonRows() {
    ScreenColumn {
        TmrSoonRow(label = "Get prep questions", onClick = {})
        TmrSoonRow(label = "Write a cover letter", onClick = {}, showDivider = false)
    }
}

@Composable
private fun CoverageDelta() {
    ScreenColumn {
        TmrCoverageDelta(now = 61, upTo = 92)
    }
}

@Composable
private fun ScreenColumn(
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TmrTheme.colors.background)
            .padding(14.dp),
        verticalArrangement = verticalArrangement,
    ) {
        content()
    }
}

private val PROGRESS_ROWS_ZERO = listOf(
    TmrProgressRow(label = "Matching 8 keywords", state = TmrProgressState.Active),
    TmrProgressRow(label = "Rewriting bullets", state = TmrProgressState.Pending),
    TmrProgressRow(label = "Checking must-haves", state = TmrProgressState.Pending),
    TmrProgressRow(label = "Fitting to 1 page", state = TmrProgressState.Pending),
)

private val PROGRESS_ROWS_FORTY = listOf(
    TmrProgressRow(label = "Matching 8 keywords", state = TmrProgressState.Done, meta = "Done"),
    TmrProgressRow(label = "Rewriting bullets", state = TmrProgressState.Active),
    TmrProgressRow(label = "Checking must-haves", state = TmrProgressState.Pending),
    TmrProgressRow(label = "Fitting to 1 page", state = TmrProgressState.Pending),
)

private val PROGRESS_ROWS_HUNDRED = listOf(
    TmrProgressRow(label = "Matching 8 keywords", state = TmrProgressState.Done, meta = "Done"),
    TmrProgressRow(label = "Rewriting bullets", state = TmrProgressState.Done, meta = "Done"),
    TmrProgressRow(label = "Checking must-haves", state = TmrProgressState.Done, meta = "Done"),
    TmrProgressRow(label = "Fitting to 1 page", state = TmrProgressState.Done, meta = "Done"),
)

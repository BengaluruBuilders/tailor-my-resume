package com.tailormyresume.core.designsystem.component.content

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tailormyresume.core.designsystem.component.TmrPreviewTheme
import com.tailormyresume.core.designsystem.component.breaksInsideWord
import com.tailormyresume.core.designsystem.theme.LocalTmrMotion
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import com.tailormyresume.core.designsystem.theme.TmrTheme
import com.tailormyresume.core.screenshot.TmrTestDevices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val CONTAINER_TAG = "content_container"

private const val LONG_FILE_NAME = "Priya-Deshmukh_Northwind-GCC_Associate-Analyst.pdf"

private const val FILE_META = "PDF, 1 page, 48 KB"

private val COVERAGE_SUMMARY = "Keywords matched: 61% now, up to 92% after tailoring"

private val PAPER_SUMMARY = "Keywords matched: 92%"

private val MATCH_WORD = Regex("""\bmatch\b""", RegexOption.IGNORE_CASE)

private val PROGRESS_ROWS = listOf(
    TmrProgressRow(label = "Matching 8 keywords", state = TmrProgressState.Done, meta = "Done"),
    TmrProgressRow(label = "Rewriting bullets", state = TmrProgressState.Active),
    TmrProgressRow(label = "Checking must-haves", state = TmrProgressState.Pending),
    TmrProgressRow(label = "Fitting to 1 page", state = TmrProgressState.Pending),
)

private val STATUS_COUNTS = mapOf(
    TmrApplicationStatus.Saved to 2,
    TmrApplicationStatus.Applied to 1,
    TmrApplicationStatus.Interview to 1,
    TmrApplicationStatus.Offer to 0,
    TmrApplicationStatus.Rejected to 1,
)

private val PAPER_BLOCKS = listOf(
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
)

internal fun AndroidComposeTestRule<*, ComponentActivity>.semanticsNodes(
    useUnmergedTree: Boolean = true,
) = onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes()

internal fun AndroidComposeTestRule<*, ComponentActivity>.textValues(useUnmergedTree: Boolean = true): List<String> =
    semanticsNodes(useUnmergedTree).flatMap { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { text -> text.text }
    }

internal fun AndroidComposeTestRule<*, ComponentActivity>.descriptionValues(
    useUnmergedTree: Boolean = true,
): List<String> =
    semanticsNodes(useUnmergedTree).flatMap { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    }

private fun assertWrapsAtWordBoundaries(node: SemanticsNode) {
    textLayouts(node).forEach { layout ->
        val text = layout.layoutInput.text.text
        (0 until layout.lineCount).forEach { line ->
            assertFalse("'$text' is ellipsized on line $line", layout.isLineEllipsized(line))
        }
        assertFalse("'$text' breaks inside a word", breaksInsideWord(layout))
    }
}

private fun textLayouts(node: SemanticsNode): List<TextLayoutResult> {
    val fitted = node.config.getOrNull(TmrFitTextLayoutKey)?.invoke()
    if (fitted != null) return listOf(fitted)
    return mutableListOf<TextLayoutResult>().also {
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(it)
    }
}

private fun AndroidComposeTestRule<*, ComponentActivity>.spinnerAnimated(): Boolean =
    semanticsNodes().mapNotNull { node -> node.config.getOrNull(TmrSpinnerAnimatedKey) }.single()

internal fun AndroidComposeTestRule<*, ComponentActivity>.assertStaysInsideContainer(
    content: @Composable () -> Unit,
) {
    setContent {
        TmrPreviewTheme {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                Box(Modifier.width(337.dp).testTag(CONTAINER_TAG)) {
                    content()
                }
            }
        }
    }
    val containerNode = onNodeWithTag(CONTAINER_TAG, useUnmergedTree = true).fetchSemanticsNode()
    val container = containerNode.boundsInRoot
    val descendants = generateSequence(containerNode.children) { level -> level.flatMap { it.children }.ifEmpty { null } }
        .flatten()
        .toList()
    assertTrue("container has no semantics descendants", descendants.isNotEmpty())
    descendants.forEach { node -> assertWrapsAtWordBoundaries(node) }
    descendants.forEach { node ->
        val bounds = node.boundsInRoot
        assertTrue("left ${bounds.left} < ${container.left}", bounds.left >= container.left - 1f)
        assertTrue("right ${bounds.right} > ${container.right}", bounds.right <= container.right + 1f)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrCoverageDeltaTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsKeywordsMatchedWith61And92() {
        rule.setContent {
            TmrPreviewTheme {
                TmrCoverageDelta(now = 61, upTo = 92)
            }
        }

        rule.onNodeWithText("Keywords matched", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("61%", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("92%", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Now", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Up to", substring = true, useUnmergedTree = true).assertExists()

        val texts = rule.textValues()
        assertFalse(texts.any { it.equals("match", ignoreCase = true) })

        assertTrue(rule.descriptionValues(useUnmergedTree = false).contains(COVERAGE_SUMMARY))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrProgressRowsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun percentExposesProgressSemantics() {
        rule.setContent {
            TmrPreviewTheme {
                TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
            }
        }

        rule.onNodeWithText("40%").assertExists()
        val node = rule.onNodeWithText("40%").fetchSemanticsNode()
        assertEquals(0.4f, node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.current)
        assertEquals("40 percent complete", node.config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun doneRowShowsMeta() {
        rule.setContent {
            TmrPreviewTheme {
                TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
            }
        }

        rule.onNodeWithText("Done", useUnmergedTree = true).assertExists()
        rule
            .onNodeWithText("Rewriting bullets")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "In progress"))
        rule
            .onNodeWithText("Checking must-haves")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Waiting"))
        rule
            .onNodeWithText("Matching 8 keywords")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"))
    }

    @Test
    fun staysInsideParentAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrProgressSpinnerMotionTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reducedMotionStillRenders() {
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(LocalTmrMotion provides TmrMotionDefaults.Reduced) {
                    TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
                }
            }
        }

        assertFalse("spinner animates under reduced motion", rule.spinnerAnimated())
    }

    @Test
    fun spinnerAnimatesWhenMotionIsNotReduced() {
        rule.setContent {
            TmrPreviewTheme {
                TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
            }
        }

        assertTrue("spinner does not animate when motion is allowed", rule.spinnerAnimated())
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrResumePaperTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun summaryHasKeywordsMatchedWording() {
        rule.setContent {
            TmrPreviewTheme {
                TmrResumePaper(
                    name = "Priya Deshmukh",
                    contact = "priya@example.com, Pune, India",
                    blocks = PAPER_BLOCKS,
                    coveragePercent = 92,
                )
            }
        }

        val merged = rule.semanticsNodes(useUnmergedTree = false)
        val descriptions = merged.flatMap {
            it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        }
        assertEquals(1, descriptions.count { description -> description.contains(PAPER_SUMMARY) })
        assertEquals(1, descriptions.count { description -> description.startsWith("Resume preview.") })
        assertFalse(descriptions.any { description -> MATCH_WORD.containsMatchIn(description) })
        assertFalse(rule.textValues().any { text -> MATCH_WORD.containsMatchIn(text) })

        val mergedWithText = merged.filter { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().isNotEmpty()
        }
        assertTrue(mergedWithText.isEmpty())
    }

    @Test
    fun legendShowsBothSources() {
        rule.setContent {
            TmrPreviewTheme {
                TmrResumePaper(
                    name = "Priya Deshmukh",
                    contact = "priya@example.com, Pune, India",
                    blocks = PAPER_BLOCKS,
                    coveragePercent = 92,
                )
            }
        }

        rule.onNodeWithText("From your resume", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("From your answer", useUnmergedTree = true).assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrChipsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keywordChipAnnouncesState() {
        rule.setContent {
            TmrPreviewTheme {
                TmrKeywordChip(label = "SQL", state = TmrKeywordState.Have)
                TmrKeywordChip(label = "Variance analysis", state = TmrKeywordState.Missing)
            }
        }

        rule
            .onNodeWithText("SQL", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "In your resume"))
        rule
            .onNodeWithText("Variance analysis", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Missing"))
    }

    @Test
    fun sourceChipShowsLabel() {
        rule.setContent {
            TmrPreviewTheme {
                TmrSource.entries.forEach { source ->
                    TmrSourceChip(source = source)
                }
            }
        }

        TmrSource.entries.forEach { source ->
            rule.onNodeWithText(rule.activity.getString(source.labelRes), useUnmergedTree = true)
                .assertExists()
        }
    }

    @Test
    fun tagLabelsFromResources() {
        rule.setContent {
            TmrPreviewTheme {
                TmrTag.entries.forEach { tag ->
                    TmrTagChip(tag = tag)
                }
            }
        }

        TmrTag.entries.forEach { tag ->
            rule
                .onNodeWithText(rule.activity.getString(tag.labelRes), useUnmergedTree = true)
                .assertExists()
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrFileCardTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shareAndOpenCallbacksFire() {
        var shareClicks = 0
        var openClicks = 0
        rule.setContent {
            TmrPreviewTheme {
                TmrFileCard(
                    fileName = LONG_FILE_NAME,
                    meta = FILE_META,
                    onShare = { shareClicks++ },
                    onOpen = { openClicks++ },
                )
            }
        }

        val share = rule.onNodeWithContentDescription("Share $LONG_FILE_NAME", useUnmergedTree = true)
        val open = rule.onNodeWithContentDescription("Open $LONG_FILE_NAME", useUnmergedTree = true)
        share.assertHeightIsAtLeast(48.dp)
        share.assertWidthIsAtLeast(48.dp)
        open.assertHeightIsAtLeast(48.dp)
        open.assertWidthIsAtLeast(48.dp)
        share.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        open.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        share.performClick()
        open.performClick()

        assertEquals(1, shareClicks)
        assertEquals(1, openClicks)
    }

    @Test
    fun showsNameAndMeta() {
        rule.setContent {
            TmrPreviewTheme {
                TmrFileCard(
                    fileName = LONG_FILE_NAME,
                    meta = FILE_META,
                    onShare = {},
                    onOpen = {},
                )
            }
        }

        rule.onNodeWithText(LONG_FILE_NAME, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(FILE_META, useUnmergedTree = true).assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrListRowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickableRowFiresAndIsAtLeast48dp() {
        var clicks = 0
        rule.setContent {
            TmrPreviewTheme {
                TmrListRow(
                    label = "Experience",
                    meta = "3",
                    tag = TmrTag.Add,
                    onClick = { clicks++ },
                )
            }
        }

        val row = rule.onNodeWithText("Experience")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        row.assertHeightIsAtLeast(48.dp)

        row.performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun rowWithoutClickHasNoClickAction() {
        rule.setContent {
            TmrPreviewTheme {
                TmrListRow(label = "Skills", meta = "7")
            }
        }

        val row = rule.onNodeWithText("Skills").fetchSemanticsNode()
        assertNull(row.config.getOrNull(SemanticsActions.OnClick))
    }

    @Test
    fun tagIsShown() {
        rule.setContent {
            TmrPreviewTheme {
                TmrListRow(label = "LinkedIn", tag = TmrTag.Add)
            }
        }

        rule.onNodeWithText("Add", useUnmergedTree = true).assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrSoonRowTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun soonRowAnnouncesComingSoon() {
        var clicks = 0
        rule.setContent {
            TmrPreviewTheme {
                TmrSoonRow(label = "Get prep questions", onClick = { clicks++ })
            }
        }

        val row = rule.onNodeWithText("Get prep questions")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Coming soon"))
        rule.onNodeWithText("Soon", useUnmergedTree = true).assertExists()
        row.assertHeightIsAtLeast(48.dp)

        row.performClick()

        assertEquals(1, clicks)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrStatusBarLegendTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun legendShowsNonZeroCounts() {
        rule.setContent {
            TmrPreviewTheme {
                TmrStatusBar(counts = STATUS_COUNTS)
            }
        }

        listOf("Saved 2", "Applied 1", "Interview 1", "Rejected 1").forEach { legend ->
            rule.onNodeWithText(legend, useUnmergedTree = true).assertExists()
        }
        assertFalse(rule.textValues().contains("Offer 0"))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrStatusChipLabelTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsLabelForEveryStatus() {
        rule.setContent {
            TmrPreviewTheme {
                TmrApplicationStatus.entries.forEach { status ->
                    TmrStatusChip(status = status)
                }
            }
        }

        TmrApplicationStatus.entries.forEach { status ->
            rule
                .onNodeWithText(rule.activity.getString(status.labelRes), useUnmergedTree = true)
                .assertExists()
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TmrStoryBarsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun stepIsAnnounced() {
        rule.setContent {
            TmrPreviewTheme {
                TmrStoryBars(count = 3, activeIndex = 1, activeFraction = 0.5f)
            }
        }

        rule.onNodeWithContentDescription("Step 2 of 3").assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrSectionLabelTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screenReaderReadsTheSourceTextAsAHeading() {
        rule.setContent { TmrPreviewTheme { TmrSectionLabel(text = "Tailored resumes") } }
        rule.onNodeWithText("Tailored resumes", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = TmrTestDevices.PROTOTYPE_QUALIFIERS)
class TmrContentFontScaleTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var strongLargeSize = 0.sp

    @Test
    fun listRowStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrListRow(label = "Experience", meta = "3", tag = TmrTag.Add, onClick = {})
        }
    }

    @Test
    fun soonRowStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrSoonRow(label = "Get prep questions", onClick = {})
        }
    }

    @Test
    fun progressRowsStayInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrProgressRows(rows = PROGRESS_ROWS, percent = 40)
        }
    }

    @Test
    fun fileCardStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrFileCard(fileName = LONG_FILE_NAME, meta = FILE_META, onShare = {}, onOpen = {})
        }
        rule.onNodeWithText(LONG_FILE_NAME, useUnmergedTree = true).assertExists()
        val nameLayout = rule.semanticsNodes()
            .flatMap { textLayouts(it) }
            .single { it.layoutInput.text.text.replace("\u200B", "") == LONG_FILE_NAME }
        assertTrue("file name did not wrap", nameLayout.lineCount > 1)
    }

    @Test
    fun fileNameWithoutSeparatorsShrinksToTheFloorInsteadOfBreaking() {
        val unbroken = "DeshmukhNorthwindResumePdf"
        rule.assertStaysInsideContainer {
            strongLargeSize = TmrTheme.typography.strongLarge.fontSize
            TmrFileCard(fileName = unbroken, meta = FILE_META, onShare = {}, onOpen = {})
        }
        val layout = rule.semanticsNodes()
            .flatMap { textLayouts(it) }
            .single { it.layoutInput.text.text.replace("\u200B", "") == unbroken }
        val startSize = strongLargeSize.value
        val fitted = layout.layoutInput.style.fontSize.value
        assertTrue("name shrank below the 1x floor: $fitted", fitted >= startSize / 2f - 0.01f)
        assertTrue("name did not shrink: $fitted", fitted < startSize)
        assertFalse(breaksInsideWord(layout))
    }

    @Test
    fun firstFrameAtFontScale200ShowsNoSplitWord() {
        val unbroken = "DeshmukhNorthwindResumePdf"
        rule.mainClock.autoAdvance = false
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                ) {
                    Box(Modifier.width(337.dp)) {
                        TmrFileCard(fileName = unbroken, meta = FILE_META, onShare = {}, onOpen = {})
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val layout = rule.semanticsNodes().flatMap { textLayouts(it) }.single { it.layoutInput.text.text == unbroken }
        assertFalse(breaksInsideWord(layout))
    }

    @Test
    fun widerContainerLetsTheNameGrowBack() {
        val unbroken = "DeshmukhNorthwindResumePdf"
        var width by mutableStateOf(337.dp)
        rule.setContent {
            TmrPreviewTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                ) {
                    Box(Modifier.width(width)) {
                        TmrFileCard(fileName = unbroken, meta = FILE_META, onShare = {}, onOpen = {})
                    }
                }
            }
        }
        val narrow = fittedNameSize(unbroken)
        width = 1000.dp
        rule.waitForIdle()
        val wide = fittedNameSize(unbroken)
        assertTrue("name did not grow back: $narrow -> $wide", wide > narrow)
    }

    private fun fittedNameSize(name: String): Float =
        rule.semanticsNodes()
            .flatMap { textLayouts(it) }
            .single { it.layoutInput.text.text.replace("\u200B", "") == name }
            .layoutInput.style.fontSize.value

    @Test
    fun coverageDeltaStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrCoverageDelta(now = 61, upTo = 92)
        }
    }

    @Test
    fun statusBarStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrStatusBar(
                counts = TmrApplicationStatus.entries.associateWith { status -> 3 },
            )
        }
    }

    @Test
    fun statusChipStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrStatusChip(status = TmrApplicationStatus.Interview)
        }
    }

    @Test
    fun keywordChipStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrKeywordChip(
                label = "Variance analysis and statistical modelling",
                state = TmrKeywordState.Missing,
            )
        }
    }

    @Test
    fun initialDiscStaysInsideAtFontScale200() {
        rule.assertStaysInsideContainer {
            TmrInitialDisc(initial = "PD", color = TmrTheme.colors.lime)
        }
    }
}

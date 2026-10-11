package com.tailormyresume.core.designsystem.theme

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TmrDocsValuesTest {
    private val designSystem = File("../../docs/DESIGN_SYSTEM.md").readText()
    private val agents = File("../../AGENTS.md").readText()

    private fun tokensSection(): String =
        designSystem.substringAfter("## Tokens").substringBefore("\n## ")

    @Test
    fun designSystemTokensSectionAndAgentsPointerArePresent() {
        val tokens = tokensSection()
        listOf(
            "surfaceRaised", "uploadCard", "disabledFill", "lineHigher", "tabDivider",
            "textDisabled", "limeSelected", "amberHighlight", "paperFold", "segOffer",
            "rejectedBorder", "Space Mono", "Space Grotesk", "strongLarge", "cardSmall",
            "paperCorner", "primaryButtonHeight", "tabCentreDisc", "sheetHandleWidth",
            "idle", "420", "300", "210", "5000", "900", "reduced", "OFL", "GoogleG",
        ).forEach { word -> assertTrue(tokens.contains(word), "Tokens section lacks $word") }
    }

    @Test
    fun prototypeIsFirstInDesignSourceTableAndOtherAgentsHeadingsRemain() {
        val designSource = agents.substringAfter("## Design source").substringBefore("\n## ")
        val rows = designSource.lines().filter { it.startsWith("| ") }
        val firstDataRow = rows.first { !it.startsWith("| Need") && !it.startsWith("|---") }
        assertTrue(firstDataRow.contains("design/prototype-2026-10-10/"), firstDataRow)
        assertTrue(designSource.contains("Prototype.dc.html"))
        val headings = agents.lines().filter { it.startsWith("## ") }
        assertEquals(
            listOf(
                "## Read these first",
                "## Design source",
                "## Motion",
                "## Commands",
                "## Flavours",
                "## Debug build",
                "## Continuous integration",
            ),
            headings,
        )
        val motion = agents.substringAfter("## Motion").substringBefore("\n## ")
        assertTrue(motion.contains("idle"))
        assertTrue(motion.contains("5 dp"))
    }
}

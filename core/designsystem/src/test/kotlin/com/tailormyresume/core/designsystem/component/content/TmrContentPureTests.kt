package com.tailormyresume.core.designsystem.component.content

import androidx.compose.ui.graphics.Color
import com.tailormyresume.core.designsystem.theme.TmrDarkColors
import com.tailormyresume.core.designsystem.theme.TmrMotionDefaults
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TmrStatusChipTest {
    private val colors = TmrDarkColors

    private fun chip(status: TmrApplicationStatus) = tmrStatusChipColors(status, colors)

    @Test
    fun coloursMatchSS() {
        val saved = chip(TmrApplicationStatus.Saved)
        assertEquals(colors.blue, saved.text)
        assertEquals(Color.Transparent, saved.fill)
        assertEquals(colors.blue, saved.border)
        assertEquals(Color(0xFF5AA9F8), saved.text)
        assertEquals(Color(0xFF5AA9F8), saved.border)

        val applied = chip(TmrApplicationStatus.Applied)
        assertEquals(colors.amber, applied.text)
        assertEquals(Color.Transparent, applied.fill)
        assertEquals(colors.amber, applied.border)
        assertEquals(Color(0xFFF7A940), applied.text)
        assertEquals(Color(0xFFF7A940), applied.border)

        val interview = chip(TmrApplicationStatus.Interview)
        assertEquals(colors.ink, interview.text)
        assertEquals(colors.lime, interview.fill)
        assertEquals(colors.lime, interview.border)
        assertEquals(Color(0xFF0A0A0A), interview.text)
        assertEquals(Color(0xFFA3F43F), interview.fill)
        assertEquals(Color(0xFFA3F43F), interview.border)

        val offer = chip(TmrApplicationStatus.Offer)
        assertEquals(colors.ink, offer.text)
        assertEquals(colors.amber, offer.fill)
        assertEquals(colors.amber, offer.border)
        assertEquals(Color(0xFF0A0A0A), offer.text)
        assertEquals(Color(0xFFF7A940), offer.fill)
        assertEquals(Color(0xFFF7A940), offer.border)

        val rejected = chip(TmrApplicationStatus.Rejected)
        assertEquals(colors.textMuted, rejected.text)
        assertEquals(Color.Transparent, rejected.fill)
        assertEquals(colors.rejectedBorder, rejected.border)
        assertEquals(Color(0xFFA6A6A6), rejected.text)
        assertEquals(Color(0xFF444444), rejected.border)
    }
}

class TmrStatusBarTest {
    private val colors = TmrDarkColors

    @Test
    fun zeroSegmentsOmittedAndColoursMatchSEGC() {
        val segments = tmrStatusBarSegments(
            counts = mapOf(
                TmrApplicationStatus.Saved to 2,
                TmrApplicationStatus.Applied to 1,
                TmrApplicationStatus.Interview to 1,
                TmrApplicationStatus.Offer to 0,
                TmrApplicationStatus.Rejected to 1,
            ),
            colors = colors,
        )

        assertEquals(4, segments.size)
        assertEquals(
            listOf(
                TmrApplicationStatus.Saved,
                TmrApplicationStatus.Applied,
                TmrApplicationStatus.Interview,
                TmrApplicationStatus.Rejected,
            ),
            segments.map { it.status },
        )
        assertEquals(listOf(2, 1, 1, 1), segments.map { it.count })
        assertEquals(
            listOf(colors.blue, colors.amber, colors.lime, colors.segRejected),
            segments.map { it.color },
        )
        assertEquals(Color(0xFF5AA9F8), segments[0].color)
        assertEquals(Color(0xFFF7A940), segments[1].color)
        assertEquals(Color(0xFFA3F43F), segments[2].color)
        assertEquals(Color(0xFF555555), segments[3].color)
    }

    @Test
    fun offerSegmentUsesSegOffer() {
        val segments = tmrStatusBarSegments(mapOf(TmrApplicationStatus.Offer to 3), colors)

        assertEquals(1, segments.size)
        assertEquals(TmrApplicationStatus.Offer, segments[0].status)
        assertEquals(3, segments[0].count)
        assertEquals(colors.segOffer, segments[0].color)
        assertEquals(Color(0xFFE9E8E4), segments[0].color)
    }

    @Test
    fun emptyCountsGiveNoSegments() {
        assertEquals(emptyList(), tmrStatusBarSegments(emptyMap(), colors))
        assertEquals(
            emptyList(),
            tmrStatusBarSegments(mapOf(TmrApplicationStatus.Offer to 0, TmrApplicationStatus.Saved to 0), colors),
        )
    }
}

class TmrStringsPolicyTest {
    private val stringElement = Regex("""<string\b[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val nameAttribute = Regex("""name\s*=\s*"([^"]+)"""")
    private val scoreWording = Regex("""\b(match|matches)\b""", RegexOption.IGNORE_CASE)
    private val allowedPhrase = "Keywords matched"

    private fun resDir(): File {
        val start = File(System.getProperty("user.dir") ?: ".")
        return if (File(start, "src/main/res").isDirectory) {
            File(start, "src/main/res")
        } else {
            File(File(start, "core/designsystem"), "src/main/res")
        }
    }

    private fun stringFiles(): List<File> =
        resDir()
            .walkTopDown()
            .filter { it.isFile }
            .filter { it.extension == "xml" && it.name.startsWith("strings") }
            .filter { it.parentFile?.name?.startsWith("values") == true }
            .toList()

    private fun valuesOf(file: File): Map<String, String> =
        stringElement.findAll(file.readText())
            .map { element ->
                val openTag = element.value.substringBefore(">")
                (nameAttribute.find(openTag)?.groupValues?.get(1) ?: element.groupValues[1]) to element.groupValues[1]
            }
            .toMap()

    @Test
    fun noScoreWordingInDesignSystemStrings() {
        val files = stringFiles()
        assertTrue(
            files.any { it.name == "strings_content.xml" },
            "expected strings_content.xml under ${resDir().path}, found ${files.map { it.name }}",
        )

        val values = files.flatMap { valuesOf(it).toList() }
        assertTrue(values.isNotEmpty())

        values.forEach { (name, value) ->
            val withoutPhrase = value.replace(allowedPhrase, "")
            assertFalse(
                scoreWording.containsMatchIn(value),
                "$name contains score wording: $value",
            )
            assertFalse(
                withoutPhrase.contains("fit score", ignoreCase = true),
                "$name contains fit score: $value",
            )
            assertFalse(
                withoutPhrase.contains("ats score", ignoreCase = true),
                "$name contains ats score: $value",
            )
        }

        assertEquals(allowedPhrase, valuesOf(File(resDir(), "values/strings_content.xml"))["core_designsystem_content_keywords_matched"])
    }
}

class TmrChipStyleTest {
    private val colors = TmrDarkColors

    @Test
    fun keywordHaveHasLimeDotOnSurface() {
        val style = tmrKeywordChipStyle(TmrKeywordState.Have, colors)

        assertEquals(colors.fill, style.fill)
        assertEquals(colors.text, style.text)
        assertEquals(Color.Transparent, style.border)
    }

    @Test
    fun keywordMissingIsAmberOutline() {
        val style = tmrKeywordChipStyle(TmrKeywordState.Missing, colors)

        assertEquals(colors.amber, style.text)
        assertEquals(Color.Transparent, style.fill)
        assertEquals(colors.amber, style.border)
    }

    @Test
    fun sourceColoursAreLimeAndAmber() {
        assertEquals(colors.lime, tmrSourceColor(TmrSource.YourResume, colors))
        assertEquals(colors.amber, tmrSourceColor(TmrSource.YourAnswer, colors))
    }

    @Test
    fun tagStylesPerKind() {
        val add = tmrTagStyle(TmrTag.Add, colors)
        assertEquals(colors.ink, add.text)
        assertEquals(colors.amber, add.fill)
        assertEquals(colors.amber, add.border)

        val bestValue = tmrTagStyle(TmrTag.BestValue, colors)
        assertEquals(colors.ink, bestValue.text)
        assertEquals(colors.lime, bestValue.fill)
        assertEquals(colors.lime, bestValue.border)

        val soon = tmrTagStyle(TmrTag.Soon, colors)
        assertEquals(colors.textMuted, soon.text)
        assertEquals(Color.Transparent, soon.fill)
        assertEquals(colors.boundary, soon.border)

        val required = tmrTagStyle(TmrTag.Required, colors)
        assertEquals(colors.amber, required.text)
        assertEquals(Color.Transparent, required.fill)
        assertEquals(colors.amber, required.border)

        listOf(TmrTag.New, TmrTag.Added, TmrTag.Rewritten, TmrTag.Reordered).forEach { tag ->
            val style = tmrTagStyle(tag, colors)
            assertEquals(colors.lime, style.text, tag.name)
            assertEquals(colors.limeSelected, style.fill, tag.name)
            assertEquals(colors.lime, style.border, tag.name)
        }
    }
}

class TmrProgressRowStyleTest {
    private val colors = TmrDarkColors

    @Test
    fun doneActivePendingStyles() {
        val done = tmrProgressRowColors(TmrProgressState.Done, colors)
        assertEquals(colors.text, done.label)
        assertEquals(colors.lime, done.meta)

        val active = tmrProgressRowColors(TmrProgressState.Active, colors)
        assertEquals(colors.text, active.label)
        assertEquals(colors.textMuted, active.meta)

        val pending = tmrProgressRowColors(TmrProgressState.Pending, colors)
        assertEquals(colors.textMuted, pending.label)
        assertEquals(colors.textMuted, pending.meta)
        assertEquals(Color(0xFFA6A6A6), pending.label)
        assertEquals(Color(0xFFA6A6A6), pending.meta)
    }

    @Test
    fun reducedSpinnerIsStatic() {
        val reduced = TmrMotionDefaults.Reduced
        listOf(0L, 450L, 1234L).forEach { frameTimeMs ->
            assertEquals(0f, tmrSpinnerAngle(frameTimeMs, reduced), 0.0001f)
        }

        val default = TmrMotionDefaults.Default
        assertEquals(0f, tmrSpinnerAngle(0L, default), 0.0001f)
        assertEquals(180f, tmrSpinnerAngle(450L, default), 0.0001f)
        assertEquals(0f, tmrSpinnerAngle(900L, default), 0.0001f)
        assertEquals(90f, tmrSpinnerAngle(225L, default), 0.0001f)
    }
}

class TmrPaperColourTest {
    private val colors = TmrDarkColors

    @Test
    fun spanColoursMatchTheDesign() {
        val fromResume = tmrPaperHighlightColor(TmrPaperHighlight.FromResume, colors)
        assertEquals(colors.limeSoft, fromResume)
        assertEquals(Color(0xFFE4FBB8), fromResume)

        val fromAnswer = tmrPaperHighlightColor(TmrPaperHighlight.FromAnswer, colors)
        assertEquals(colors.amberHighlight, fromAnswer)
        assertEquals(Color(0xFFFFE2C2), fromAnswer)

        assertEquals(Color.Transparent, tmrPaperHighlightColor(TmrPaperHighlight.None, colors))
    }
}

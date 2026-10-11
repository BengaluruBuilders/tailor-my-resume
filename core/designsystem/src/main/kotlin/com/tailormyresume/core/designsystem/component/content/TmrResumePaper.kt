package com.tailormyresume.core.designsystem.component.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailormyresume.core.designsystem.R
import com.tailormyresume.core.designsystem.theme.TmrColors
import com.tailormyresume.core.designsystem.theme.TmrTheme

enum class TmrPaperHighlight { None, FromResume, FromAnswer }

@Immutable
data class TmrPaperSpan(val text: String, val highlight: TmrPaperHighlight = TmrPaperHighlight.None)

@Immutable
data class TmrPaperBlock(
    val heading: String? = null,
    val title: String? = null,
    val dates: String? = null,
    val lines: List<List<TmrPaperSpan>> = emptyList(),
)

internal fun tmrPaperHighlightColor(highlight: TmrPaperHighlight, colors: TmrColors): Color =
    when (highlight) {
        TmrPaperHighlight.None -> Color.Transparent
        TmrPaperHighlight.FromResume -> colors.limeSoft
        TmrPaperHighlight.FromAnswer -> colors.amberHighlight
    }

@Composable
private fun Dp.fixedSp(): TextUnit = with(LocalDensity.current) { this@fixedSp.toSp() }

@Composable
fun TmrResumePaper(
    name: String,
    contact: String,
    blocks: List<TmrPaperBlock>,
    coveragePercent: Int,
    modifier: Modifier = Modifier,
) {
    val colors = TmrTheme.colors
    val summary = stringResource(R.string.core_designsystem_content_paper_summary, coveragePercent)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.padding(top = 24.dp, end = 6.dp)) {
            PaperPage(name = name, contact = contact, blocks = blocks)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-24).dp)
                    .size(64.dp)
                    .rotate(10f)
                    .background(colors.ink, CircleShape)
                    .border(2.5.dp, colors.lime, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.core_designsystem_content_percent, coveragePercent),
                    style = TmrTheme.typography.mono15.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.dp.fixedSp(),
                        color = colors.lime,
                    ),
                )
            }
        }
        PaperLegend()
    }
}

@Composable
private fun PaperPage(name: String, contact: String, blocks: List<TmrPaperBlock>) {
    val colors = TmrTheme.colors
    val nameSize = 17.dp.fixedSp()
    val contactSize = 7.5.dp.fixedSp()
    val headingSize = 7.dp.fixedSp()
    val rowTitleSize = 8.5.dp.fixedSp()
    val lineSize = 7.5.dp.fixedSp()
    val pageStyle = TextStyle(color = colors.ink, fontFamily = FontFamily.SansSerif)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paper, TmrTheme.shapes.paperCorner)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            text = name,
            style = pageStyle.copy(fontWeight = FontWeight.Bold, fontSize = nameSize),
        )
        Text(text = contact, style = pageStyle.copy(fontSize = contactSize))
        blocks.forEach { block ->
            if (block.heading != null) {
                Text(
                    text = block.heading.uppercase(),
                    style = pageStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = headingSize,
                        letterSpacing = 0.8.sp,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.paperFold),
                )
            }
            if (block.title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = block.title,
                        style = pageStyle.copy(fontWeight = FontWeight.Bold, fontSize = rowTitleSize),
                        modifier = Modifier.weight(1f),
                    )
                    if (block.dates != null) {
                        Text(text = block.dates, style = pageStyle.copy(fontSize = rowTitleSize))
                    }
                }
            }
            block.lines.forEach { spans ->
                Text(
                    text = buildPaperLine(spans),
                    style = pageStyle.copy(fontSize = lineSize, lineHeight = lineSize * 1.45f),
                )
            }
        }
    }
}

@Composable
private fun buildPaperLine(spans: List<TmrPaperSpan>): AnnotatedString {
    val colors = TmrTheme.colors
    return buildAnnotatedString {
        spans.forEach { span ->
            withStyle(SpanStyle(background = tmrPaperHighlightColor(span.highlight, colors))) {
                append(span.text)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaperLegend() {
    val colors = TmrTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendEntry(colors.limeSoft, stringResource(R.string.core_designsystem_content_paper_from_resume))
        LegendEntry(colors.amberHighlight, stringResource(R.string.core_designsystem_content_paper_from_answer))
    }
}

@Composable
private fun LegendEntry(swatch: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(swatch, RoundedCornerShape(3.dp)),
        )
        Text(text = label, style = TmrTheme.typography.caption, color = TmrTheme.colors.textMuted)
    }
}

package com.tailormyresume.feature.profile.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val ZERO_WIDTH_SPACE = '\u200B'

private fun Char.isBoundary() = isWhitespace() || this == ZERO_WIDTH_SPACE || this == '-'

private fun TextLayoutResult.breaksInsideWord(): Boolean {
    val text = layoutInput.text.text
    return (0 until lineCount - 1).any { line ->
        val end = getLineEnd(line, visibleEnd = false)
        end in 1 until text.length && !text[end - 1].isBoundary() && !text[end].isWhitespace()
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditorsLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    @Config(qualifiers = NARROW_QUALIFIERS)
    fun noTextIsEllipsizedOrBrokenInsideAWordAt337dpFont2() {
        var content by mutableStateOf<@androidx.compose.runtime.Composable () -> Unit>({})
        rule.setTmrContent(NARROW_DEVICE) { content() }
        editorStates().forEach { (name, marker, state) ->
            content = state
            rule.waitForIdle()
            rule.assertTexts(marker)
            rule.assertNoTruncatedText()
            val layouts = rule
                .onAllNodes(SemanticsMatcher("has text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) })
                .fetchSemanticsNodes()
                .mapNotNull { node ->
                    val out = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(out)
                    out.firstOrNull()
                }
            layouts.forEach { layout ->
                assertWithMessage("$name: \"${layout.layoutInput.text.text}\" breaks inside a word")
                    .that(layout.breaksInsideWord())
                    .isFalse()
            }
        }
    }
}

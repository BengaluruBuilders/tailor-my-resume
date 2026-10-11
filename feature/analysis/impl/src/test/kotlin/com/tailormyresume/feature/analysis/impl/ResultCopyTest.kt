package com.tailormyresume.feature.analysis.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ResultCopyTest {

    private val strings = File("src/main/res/values/strings_result.xml").readText()

    @Test
    fun noStringUsesMatchFitAtsOrScoreAsLabel() {
        val scoreWording = Regex("""\b(match|fit|ATS|score)\b""", RegexOption.IGNORE_CASE)
        assertThat(scoreWording.findAll(strings).map { it.value }.toList()).isEmpty()
    }

    @Test
    fun theKeywordsMatchedLabelStaysAllowed() {
        val scoreWording = Regex("""\b(match|fit|ATS|score)\b""", RegexOption.IGNORE_CASE)
        assertThat(scoreWording.containsMatchIn("Keywords matched")).isFalse()
    }

    @Test
    fun drawnCopyIsPresent() {
        listOf(
            "Tailor my resume",
            "Uses 1 credit · you have %1\$d",
            "No credits left · get more to continue",
            "One quick question",
            "Pick an answer, or skip",
            "Skip this",
            "Not clear in your resume. We\\'ll ask you next.",
        ).forEach { assertThat(strings).contains(it) }
    }
}

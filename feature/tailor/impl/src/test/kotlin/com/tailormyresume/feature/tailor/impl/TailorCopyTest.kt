package com.tailormyresume.feature.tailor.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class TailorCopyTest {

    private val valuesDirectory = File("src/main/res/values")

    private val stringFiles = valuesDirectory.listFiles { file -> file.name.startsWith("strings") && file.extension == "xml" }
        .orEmpty()

    private val stringValues = stringFiles.flatMap { file ->
        Regex("""<string [^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(file.readText())
            .map { it.groupValues[1] }
            .toList()
    }

    @Test
    fun noStringUsesMatchAsAScore() {
        val score = Regex("""\b(match|fit|ATS)\b""", RegexOption.IGNORE_CASE)

        assertThat(stringFiles.map { it.name }).contains("strings_result.xml")
        assertThat(stringValues).isNotEmpty()
        assertThat(stringValues.filter { score.containsMatchIn(it) }).isEmpty()
    }

    @Test
    fun resultCopyNamesKeywordCoverageAndTheAcceptRule() {
        assertThat(stringValues).contains("Accept the changes first")
        assertThat(stringValues.any { it.contains("keywords") }).isTrue()
    }
}

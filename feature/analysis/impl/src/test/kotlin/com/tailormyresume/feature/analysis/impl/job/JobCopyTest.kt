package com.tailormyresume.feature.analysis.impl.job

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class JobCopyTest {

    private val strings = File("src/main/res/values/strings_job.xml").readText()

    @Test
    fun noScoreWordingInJobStrings() {
        val scoreWording = Regex("""\b(match|matches|matched|matching|fit|fits|ATS)\b""", RegexOption.IGNORE_CASE)
        assertThat(scoreWording.findAll(strings).map { it.value }.toList()).isEmpty()
    }

    @Test
    fun stringsFileExistsWithTheDrawnCopy() {
        assertThat(strings).contains("Paste from clipboard")
        assertThat(strings).contains("This doesn\\'t look like a job post")
        assertThat(strings).contains("Import from a link")
    }
}

package com.tailormyresume.feature.onboarding.impl.policy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class NoResumeTextLoggedTest {

    private val mainSources = File("src/main/kotlin")

    private val loggingCalls = Regex("""\b(Log\.[a-z]\(|println\(|print\(|Timber\.|Logger\.|logcat\()""")

    @Test
    fun onboardingSourcesNeverLog() {
        val offenders = mainSources.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file -> file.readLines().any { loggingCalls.containsMatchIn(it) } }
            .map { it.name }
            .toList()

        assertThat(mainSources.exists()).isTrue()
        assertThat(offenders).isEmpty()
    }
}

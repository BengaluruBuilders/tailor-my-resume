package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class SignInStringsTest {

    private val strings = File("src/main/res/values/strings_signin.xml").readText()
    private val forbidden = Regex("""\bmatch\b|ATS score|guarantee|interview|hired""", RegexOption.IGNORE_CASE)

    @Test
    fun noMatchScoreCopy() {
        assertThat(strings).contains("story1_subline")
        assertThat(forbidden.find(strings)).isNull()
    }

    @Test
    fun factualMatchingDoesNotClaimAScore() {
        assertThat(forbidden.find("AI matches your real skills and achievements to each job description.")).isNull()
    }

    @Test
    fun scoresAndOutcomeClaimsRemainForbidden() {
        listOf("Your match", "90% match", "Your ATS score", "Guaranteed interview", "Get hired").forEach {
            assertThat(forbidden.find(it)).isNotNull()
        }
    }

    @Test
    fun jobStickerDoesNotPromiseAResult() {
        assertThat(strings).contains("For this job")
    }

    @Test
    fun googleOnly() {
        assertThat(strings).doesNotContain("Apple")
        assertThat(strings).contains("Continue with Google")
    }
}

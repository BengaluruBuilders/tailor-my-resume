package com.tailormyresume.feature.onboarding.impl.signin

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class SignInStringsTest {

    private val strings = File("src/main/res/values/strings_signin.xml").readText()

    @Test
    fun noMatchScoreCopy() {
        assertThat(strings).contains("story1_subline")
        val forbidden = Regex("match|ATS score|guarantee|interview|hired", RegexOption.IGNORE_CASE)
        assertThat(forbidden.find(strings)).isNull()
    }

    @Test
    fun keywordStickerNamesKeywordsNotMatch() {
        assertThat(strings).contains("92% keywords")
    }

    @Test
    fun googleOnly() {
        assertThat(strings).doesNotContain("Apple")
        assertThat(strings).contains("Continue with Google")
    }
}

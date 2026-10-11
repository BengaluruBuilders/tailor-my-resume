package com.tailormyresume.feature.tailor.impl.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class TailoredNoPerBulletTogglesTest {

    private val resultSources = File("src/main/kotlin/com/tailormyresume/feature/tailor/impl/result")
        .listFiles { file -> file.extension == "kt" }
        .orEmpty()

    @Test
    fun resultComposablesReferenceNoPerBulletAcceptOrReject() {
        val forbidden = Regex("""\b(onAccept|onKeepOriginal|onReject)\b""")

        assertThat(resultSources.map { it.name }).contains("TailoredScreen.kt")
        resultSources.forEach { file ->
            assertThat(forbidden.containsMatchIn(file.readText())).isFalse()
        }
        val screen = resultSources.first { it.name == "TailoredScreen.kt" }.readText()
        assertThat(screen).contains("onUndo")
        assertThat(screen).contains("TmrSecondaryButton")
    }
}

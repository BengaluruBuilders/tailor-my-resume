package com.tailormyresume.feature.applications.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ApplicationsCopyTest {

    private val stringsFile = File("src/main/res/values/strings_applications.xml")

    private val entries: Map<String, String> =
        Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(stringsFile.takeIf { it.isFile }?.readText().orEmpty())
            .associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun noStringUsesMatchFitOrAtsAsAScore() {
        val score = Regex("""\b(match|fit|ATS|likelihood|chance|odds)\b""", RegexOption.IGNORE_CASE)

        assertThat(entries).isNotEmpty()
        assertThat(entries.keys.all { it.startsWith("feature_applications_impl_") }).isTrue()
        assertThat(entries.filterValues { score.containsMatchIn(it) }).isEmpty()
    }

    @Test
    fun keywordMetaFormat() {
        assertThat(entries["feature_applications_impl_row_meta_keywords"]).isEqualTo("%1\$s · %2\$d%% keywords")
        assertThat(entries["feature_applications_impl_file_meta_keywords"]).isEqualTo("PDF · %1\$d%% keywords")
    }
}

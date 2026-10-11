package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class NoLoggingSourceTest {
    private val forbidden = Regex("""(^|[^A-Za-z0-9_.])(Log\.|Timber|println\(|HttpLoggingInterceptor)""")

    @Test
    fun networkAndProdSourcesNeverLog() {
        val roots = listOf(File("src/main"), File("../../app/src/prod"))
        val offenders = roots.flatMap { root ->
            assertThat(root.exists()).isTrue()
            root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
                file.readLines().withIndex().filter { forbidden.containsMatchIn(it.value) }.map { "${file.name}:${it.index + 1}" }
            }.toList()
        }

        assertThat(offenders).isEmpty()
    }
}

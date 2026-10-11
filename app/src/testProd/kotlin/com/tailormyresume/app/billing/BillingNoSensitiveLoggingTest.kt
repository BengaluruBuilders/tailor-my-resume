package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class BillingNoSensitiveLoggingTest {
    private val logging = Regex("""(^|[^A-Za-z0-9_.])(Log\.|Timber|println\(|print\()""")
    private val sensitiveWord = "(?i)(token|orderId|obfuscated|uid|idToken)"
    private val sensitiveFailure = Regex("""(error|check|checkNotNull|require|requireNotNull|\w+Exception)\(.*$sensitiveWord""")

    @Test
    fun billingSourcesLogNothingSensitive() {
        val roots = listOf(File("src/prod/kotlin/com/tailormyresume/app/billing"), File("src/prod/kotlin/com/tailormyresume/app/credits"))
        val offenders = roots.flatMap { root ->
            assertThat(root.exists()).isTrue()
            root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
                file.readLines().withIndex()
                    .filter { logging.containsMatchIn(it.value) || sensitiveFailure.containsMatchIn(it.value) }
                    .map { "${file.name}:${it.index + 1}" }
            }.toList()
        }

        assertThat(offenders).isEmpty()
    }
}

package com.tailormyresume.app.navigation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

class ArchitectureDocNavKeysTest {

    private val architecture = File("../docs/ARCHITECTURE.md").readText()

    private fun section6(): String {
        val start = architecture.indexOf("\n## 6.")
        check(start >= 0) { "ARCHITECTURE.md has no section 6" }
        val end = architecture.indexOf("\n## ", start + 1).let { if (it < 0) architecture.length else it }
        return architecture.substring(start, end)
    }

    @Test
    fun section6ListsEveryNavKey() {
        val section = section6()
        val rows = section.lines()

        CHROME_TABLE.forEach { row ->
            val name = checkNotNull(row.key::class.simpleName)
            val module = row.key::class.java.packageName
                .removePrefix("com.tailormyresume.")
                .split('.')
                .take(2)
                .joinToString("/")
            val line = rows.firstOrNull { Regex("`" + name + "[`(]").containsMatchIn(it) }
            assertWithMessage("$name is listed in section 6").that(line).isNotNull()
            assertWithMessage("$name row names its module $module").that(line).contains(module)
        }
    }

    @Test
    fun section6StatesTheNavigationRules() {
        val section = section6().lowercase()

        listOf("back", "root", "close", "tab bar").forEach { rule ->
            assertThat(section).contains(rule)
        }
    }

    @Test
    fun section6NoLongerMentionsTheRemovedKeysAndDock() {
        val section = section6()

        listOf("ImportResumeNavKey", "AnalysisNavKey", "TailorNavKey", "DeleteAccountNavKey", "TmrDock")
            .forEach { removed -> assertThat(section).doesNotContain(removed) }
    }
}

package com.tailormyresume.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class NoDockReferencesTest {

    private val repoRoot = File("..").canonicalFile

    private fun sourceRoots(): List<File> {
        val modules = listOf("app") +
            File(repoRoot, "core").listFiles().orEmpty().filter { it.isDirectory }.map { "core/${it.name}" } +
            File(repoRoot, "feature").listFiles().orEmpty().filter { it.isDirectory }.flatMap { feature ->
                feature.listFiles().orEmpty().filter { it.isDirectory }.map { "feature/${feature.name}/${it.name}" }
            }
        return modules.flatMap { module -> listOf("main", "debug").map { File(repoRoot, "$module/src/$it") } }
            .filter { it.isDirectory }
    }

    @Test
    fun noSourceReferencesTmrDock() {
        val offenders = sourceRoots()
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml") }.toList() }
            .filter { Regex("TmrDock").containsMatchIn(it.readText()) }
            .map { it.relativeTo(repoRoot).path }

        assertThat(offenders).isEmpty()
    }

    @Test
    fun theDockSourcesAndTheirTestAreGone() {
        val designsystem = File(repoRoot, "core/designsystem/src")
        assertThat(File(designsystem, "main/kotlin/com/tailormyresume/core/designsystem/component/TmrDock.kt").exists()).isFalse()
        assertThat(File(designsystem, "test/kotlin/com/tailormyresume/core/designsystem/component/TmrDockDefaultsTest.kt").exists()).isFalse()
    }
}

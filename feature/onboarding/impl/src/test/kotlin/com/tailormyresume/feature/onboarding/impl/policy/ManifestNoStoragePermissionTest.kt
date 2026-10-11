package com.tailormyresume.feature.onboarding.impl.policy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ManifestNoStoragePermissionTest {

    private val repositoryRoot = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    @Test
    fun mergedManifestDeclaresNoStoragePermission() {
        val manifests = repositoryRoot.walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", ".claude") }
            .filter { it.name == "AndroidManifest.xml" && "src" in it.path }
            .toList()

        assertThat(manifests).isNotEmpty()
        manifests.forEach { manifest ->
            val text = manifest.readText()
            assertThat(text).doesNotContain("READ_EXTERNAL_STORAGE")
            assertThat(text).doesNotContain("WRITE_EXTERNAL_STORAGE")
            assertThat(text).doesNotContain("MANAGE_EXTERNAL_STORAGE")
            assertThat(text).doesNotContain("READ_MEDIA_")
        }
    }
}

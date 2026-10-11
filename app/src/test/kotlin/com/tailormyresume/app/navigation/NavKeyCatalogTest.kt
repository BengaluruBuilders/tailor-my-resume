package com.tailormyresume.app.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tailormyresume.feature.profile.api.navigation.ProfileListSection
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Test
import java.io.File

class NavKeyCatalogTest {

    private val repoRoot = File("..").canonicalFile

    private val allNavKeys: List<NavKey> = CHROME_TABLE.map { it.key }

    @Test
    fun exactlyTwentySevenKeysAndNoDeletedKeys() {
        assertThat(allNavKeys).hasSize(27)
        assertThat(allNavKeys.map { it::class }).containsNoDuplicates()
        assertThat(sourceKeyDeclarations()).isEqualTo(27)

        listOf(
            "com.tailormyresume.feature.onboarding.api.navigation.ImportResumeNavKey",
            "com.tailormyresume.feature.analysis.api.navigation.AnalysisNavKey",
            "com.tailormyresume.feature.tailor.api.navigation.TailorNavKey",
            "com.tailormyresume.feature.settings.api.navigation.DeleteAccountNavKey",
            "com.tailormyresume.feature.tailor.api.navigation.CreditsNavKey",
        ).forEach { name ->
            val found = runCatching { Class.forName(name) }.isSuccess
            assertWithMessage(name).that(found).isFalse()
        }
    }

    @Test
    fun everyKeyRoundTripsThroughJson() {
        allNavKeys.forEach { key ->
            val serializer = serializer(key.javaClass)
            val encoded = Json.encodeToString(serializer, key)

            assertThat(Json.decodeFromString(serializer, encoded)).isEqualTo(key)
        }
    }

    @Test
    fun theProfileListSectionsAreSummaryEducationAchievements() {
        assertThat(ProfileListSection.entries.map { it.name })
            .containsExactly("SUMMARY", "EDUCATION", "ACHIEVEMENTS")
            .inOrder()
    }

    private fun sourceKeyDeclarations(): Int =
        File(repoRoot, "feature").listFiles().orEmpty()
            .map { File(it, "api/src/main/kotlin") }
            .filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { file -> file.extension == "kt" }.toList() }
            .sumOf { file -> Regex("""\)\s*:\s*NavKey\b|object\s+\w+\s*:\s*NavKey\b""").findAll(file.readText()).count() }
}

package com.tailormyresume.core.domain.onboarding

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.ProfileLimits
import com.tailormyresume.core.model.continues
import com.tailormyresume.core.testing.repository.TestProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Instant

private val UNIQUE_WORK_ID = Regex("W-[0-9a-f]{8}")

class SaveImportedProfileUseCaseTest {

    private val repository = TestProfileRepository()
    private val useCase = SaveImportedProfileUseCase(repository, FactIdAllocator())

    @Test
    fun freshIdsAreUniqueAgainstKeptEntries() = runTest {
        repository.sendProfile(
            profile(
                entries = listOf(entry("W-01", EntryCategory.EXPERIENCE, source = FactSource.USER_STATED)),
            ),
        )
        val parsed = profile(
            entries = listOf(
                entry("a", EntryCategory.EXPERIENCE, title = "Backend developer"),
                entry("b", EntryCategory.EXPERIENCE, title = "Mobile developer"),
            ),
        )

        useCase.invoke(parsed, "resume.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        val ids = saved.entries.map { it.id }
        assertThat(ids.first()).isEqualTo("W-01")
        assertThat(ids.drop(1)).containsNoneIn(listOf("W-01"))
        assertThat(ids.toSet()).hasSize(3)
        assertThat(ids.drop(1).all { it.matches(UNIQUE_WORK_ID) }).isTrue()
    }

    @Test
    fun continuationsKeepLinkAfterReid() = runTest {
        val parsed = profile(
            entries = listOf(
                entry("p", EntryCategory.EXPERIENCE, title = "Android developer", bullets = listOf(EvidenceBullet("p-b1", "Shipped the service."))),
                entry("q", EntryCategory.EXPERIENCE, title = "Android developer", bullets = listOf(EvidenceBullet("q-b1", "Shipped the client."))),
            ),
        )

        useCase.invoke(parsed, null)

        val entries = checkNotNull(repository.observeProfile().first()).entries
        assertThat(entries).hasSize(2)
        assertThat(entries[1].continues(entries[0])).isTrue()
        assertThat(entries[0].bullets.map { it.id }).containsExactly(entries[0].id + "-b1")
        assertThat(entries[1].bullets.map { it.id }).containsExactly(entries[1].id + "-b1")
    }

    @Test
    fun userStatedFactsAndSkillsKeptNotOverwritten() = runTest {
        repository.sendProfile(
            profile(
                fullName = "Kept Name",
                email = "kept@example.com",
                phone = "+91 90000 00000",
                city = "Pune",
                linkedinUrl = "https://linkedin.example.com/kept",
                portfolioUrl = "https://portfolio.example.com/kept",
                summary = "Kept summary.",
                headline = "Kept headline",
                skills = listOf("Kotlin"),
                userStatedSkills = listOf("Room"),
                entries = listOf(
                    entry("W-01", EntryCategory.EXPERIENCE, title = "Kept role", source = FactSource.USER_STATED),
                    entry("W-02", EntryCategory.PROJECT, title = "Kept project", source = FactSource.USER_EDITED),
                    entry("W-03", EntryCategory.EDUCATION, title = "Kept degree", source = FactSource.USER_ANSWER),
                    entry("W-04", EntryCategory.CERTIFICATION, title = "Old import", source = FactSource.IMPORTED),
                ),
            ),
        )
        val parsed = profile(
            fullName = "Parsed Name",
            email = "parsed@example.com",
            phone = "+91 80000 00000",
            city = "Mumbai",
            linkedinUrl = "https://linkedin.example.com/parsed",
            portfolioUrl = "https://portfolio.example.com/parsed",
            summary = "Parsed summary.",
            headline = "Parsed headline",
            skills = listOf("Kotlin", "Room", "Coroutines", "kotlin"),
            entries = listOf(
                entry("x1", EntryCategory.EXPERIENCE, title = "Parsed role", source = FactSource.IMPORTED),
            ),
        )

        useCase.invoke(parsed, "resume.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries.map { it.id }.take(3)).containsExactly("W-01", "W-02", "W-03").inOrder()
        assertThat(saved.entries).hasSize(4)
        assertThat(saved.entries.last().id).isNotEqualTo("W-04")
        assertThat(saved.entries.first().title).isEqualTo("Kept role")
        assertThat(saved.fullName).isEqualTo("Kept Name")
        assertThat(saved.email).isEqualTo("kept@example.com")
        assertThat(saved.phone).isEqualTo("+91 90000 00000")
        assertThat(saved.city).isEqualTo("Pune")
        assertThat(saved.linkedinUrl).isEqualTo("https://linkedin.example.com/kept")
        assertThat(saved.portfolioUrl).isEqualTo("https://portfolio.example.com/kept")
        assertThat(saved.summary).isEqualTo("Kept summary.")
        assertThat(saved.headline).isEqualTo("Kept headline")
        assertThat(saved.userStatedSkills).containsExactly("Room")
        assertThat(saved.skills).containsExactly("Room", "Kotlin", "Coroutines").inOrder()
    }

    @Test
    fun sentenceIsNotSavedAsTheName() = runTest {
        val parsed = profile(
            fullName = "The quick brown fox jumps over the lazy dog and keeps running.",
            entries = listOf(entry("x1", EntryCategory.EXPERIENCE)),
        )

        useCase.invoke(parsed, "resume.pdf")

        assertThat(checkNotNull(repository.observeProfile().first()).fullName).isEmpty()
    }

    @Test
    fun userStatedNameKeepsTheWholeExistingContact() = runTest {
        repository.sendProfile(profile(fullName = "Asha Rao", entries = emptyList()))
        val parsed = profile(
            fullName = "Priya Deshmukh",
            email = "priya@example.com",
            phone = "+91 98765 43210",
            city = "Pune",
            entries = listOf(entry("x1", EntryCategory.EXPERIENCE)),
        )

        useCase.invoke(parsed, "resume.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(listOf(saved.fullName, saved.email, saved.phone, saved.city))
            .containsExactly("Asha Rao", "", "", "").inOrder()
    }

    @Test
    fun importedOnlyContactIsReplacedWholeByTheNewImport() = runTest {
        repository.sendProfile(
            profile(
                fullName = "Asha Rao",
                email = "asha@example.com",
                phone = "+91 90000 00000",
                city = "Mumbai",
                entries = listOf(entry("W-01", EntryCategory.EXPERIENCE)),
            ).copy(sourceFileName = "old.pdf"),
        )
        val parsed = profile(
            fullName = "Priya Deshmukh",
            email = "priya@example.com",
            phone = "",
            city = "Pune",
            entries = listOf(entry("x1", EntryCategory.EXPERIENCE)),
        )

        useCase.invoke(parsed, "new.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(listOf(saved.fullName, saved.email, saved.phone, saved.city))
            .containsExactly("Priya Deshmukh", "priya@example.com", "", "Pune").inOrder()
    }

    @Test
    fun importWithoutAValidNameKeepsTheExistingContact() = runTest {
        repository.sendProfile(
            profile(
                fullName = "Asha Rao",
                email = "asha@example.com",
                entries = listOf(entry("W-01", EntryCategory.EXPERIENCE)),
            ).copy(sourceFileName = "old.pdf"),
        )
        val parsed = profile(
            fullName = "The quick brown fox jumps over the lazy dog and keeps running.",
            email = "other@example.com",
            phone = "+91 98765 43210",
            entries = listOf(entry("x1", EntryCategory.EXPERIENCE)),
        )

        useCase.invoke(parsed, "new.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(listOf(saved.fullName, saved.email, saved.phone))
            .containsExactly("Asha Rao", "asha@example.com", "").inOrder()
    }

    @Test
    fun looksLikeNameAcceptsNamesAndRejectsSentences() {
        listOf("Priya Deshmukh", "J. R. Smith", "Dr. Asha Rao", "Madonna").forEach {
            assertThat(looksLikeName(it)).isTrue()
        }
        listOf(
            "",
            "   ",
            "The quick brown fox jumps over the lazy dog and keeps running.",
            "Hello world today.",
            "Priya D, Pune",
            "Priya Deshmukh 2026",
            "one two three four five six",
        ).forEach { assertThat(looksLikeName(it)).isFalse() }
    }

    @Test
    fun importedEntriesMarkedImportedAndUnconfirmed() = runTest {
        val parsed = profile(
            entries = listOf(
                entry("x1", EntryCategory.EXPERIENCE, title = "Parsed role", source = FactSource.USER_STATED, confirmed = true),
                entry("x2", EntryCategory.PROJECT, title = "Parsed project", source = FactSource.IMPORTED, confirmed = true),
            ),
        )

        useCase.invoke(parsed, "resume.pdf")

        val entries = checkNotNull(repository.observeProfile().first()).entries
        assertThat(entries.map { it.source }).containsExactly(FactSource.IMPORTED, FactSource.IMPORTED)
        assertThat(entries.map { it.isConfirmed }).containsExactly(false, false)
    }

    @Test
    fun sourceFileNameRecorded() = runTest {
        val withName = useCase.invoke(profile(entries = emptyList()), "cv-2026.pdf")

        val savedWithName = checkNotNull(repository.observeProfile().first())
        assertThat(savedWithName.sourceFileName).isEqualTo("cv-2026.pdf")
        assertThat(withName).isEqualTo(savedWithName)

        val withoutName = useCase.invoke(profile(entries = emptyList()), null)

        val savedWithoutName = checkNotNull(repository.observeProfile().first())
        assertThat(savedWithoutName.sourceFileName).isNull()
        assertThat(withoutName).isEqualTo(savedWithoutName)
    }

    @Test
    fun limitsEnforced() = runTest {
        val parsed = profile(
            skills = (1..ProfileLimits.MAX_SKILLS + 5).map { "Skill$it" },
            entries = List(ProfileLimits.MAX_ENTRIES + 3) { index ->
                entry(
                    id = "x$index",
                    category = EntryCategory.EXPERIENCE,
                    title = "Role $index",
                    bullets = List(ProfileLimits.MAX_BULLETS_PER_ENTRY) { bullet ->
                        EvidenceBullet("x$index-b$bullet", "Bullet $index.$bullet.")
                    },
                )
            },
        )

        useCase.invoke(parsed, "resume.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries).hasSize(ProfileLimits.MAX_ENTRIES)
        assertThat(saved.entries.first().title).isEqualTo("Role 0")
        assertThat(saved.entries.last().title).isEqualTo("Role ${ProfileLimits.MAX_ENTRIES - 1}")
        assertThat(saved.entries.sumOf { it.bullets.size }).isEqualTo(ProfileLimits.MAX_BULLETS_IN_TOTAL)
        assertThat(saved.skills).hasSize(ProfileLimits.MAX_SKILLS)
        assertThat(saved.skills.last()).isEqualTo("Skill${ProfileLimits.MAX_SKILLS}")
    }

    @Test
    fun reimportReplacesOnlyImportedEntries() = runTest {
        repository.sendProfile(
            profile(
                skills = listOf("Old skill"),
                entries = listOf(
                    entry("W-01", EntryCategory.EXPERIENCE, title = "Kept role", source = FactSource.USER_STATED, confirmed = true),
                    entry("W-02", EntryCategory.EXPERIENCE, title = "Old imported role", source = FactSource.IMPORTED),
                ),
            ).copy(reviewedAt = Instant.fromEpochSeconds(1)),
        )
        val parsed = profile(
            entries = listOf(entry("y1", EntryCategory.EXPERIENCE, title = "New imported role")),
        )

        useCase.invoke(parsed, "resume-v2.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries.first().id).isEqualTo("W-01")
        assertThat(saved.entries.last().id).isNotIn(listOf("W-01", "W-02"))
        assertThat(saved.entries.first().title).isEqualTo("Kept role")
        assertThat(saved.entries.first().source).isEqualTo(FactSource.USER_STATED)
        assertThat(saved.entries.first().isConfirmed).isTrue()
        assertThat(saved.entries.last().title).isEqualTo("New imported role")
        assertThat(saved.sourceFileName).isEqualTo("resume-v2.pdf")
        assertThat(saved.reviewedAt).isNull()
    }

    @Test
    fun reimportNeverGivesAnIdThatARemovedFactHad() = runTest {
        repository.sendProfile(
            profile(
                entries = listOf(
                    entry("W-01", EntryCategory.EXPERIENCE, title = "Old role", source = FactSource.IMPORTED),
                ),
            ),
        )
        val oldReference = "W-01"
        val parsed = profile(entries = listOf(entry("y1", EntryCategory.EXPERIENCE, title = "New role")))

        useCase.invoke(parsed, "resume-v2.pdf")

        val saved = checkNotNull(repository.observeProfile().first())
        assertThat(saved.entries.map { it.id }).doesNotContain(oldReference)
        assertThat(saved.entries.firstOrNull { it.id == oldReference }).isNull()
        assertThat(saved.entries.single().bullets.map { it.id }).doesNotContain("W-01-b1")
    }

    private fun entry(
        id: String,
        category: EntryCategory,
        title: String = "Title",
        organization: String = "Org",
        startDate: String = "2024",
        endDate: String = "2025",
        bullets: List<EvidenceBullet> = listOf(EvidenceBullet("$id-b1", "Text for $id.")),
        source: FactSource = FactSource.IMPORTED,
        confirmed: Boolean = false,
    ) = ProfileEntry(
        id = id,
        category = category,
        title = title,
        organization = organization,
        startDate = startDate,
        endDate = endDate,
        bullets = bullets,
        source = source,
        isConfirmed = confirmed,
    )

    private fun profile(
        fullName: String = "",
        email: String = "",
        phone: String = "",
        city: String = "",
        linkedinUrl: String = "",
        portfolioUrl: String = "",
        summary: String = "",
        headline: String = "",
        skills: List<String> = emptyList(),
        userStatedSkills: List<String> = emptyList(),
        entries: List<ProfileEntry>,
    ) = CandidateProfile(
        fullName = fullName,
        email = email,
        phone = phone,
        headline = headline,
        skills = skills,
        entries = entries,
        userStatedSkills = userStatedSkills,
        city = city,
        linkedinUrl = linkedinUrl,
        portfolioUrl = portfolioUrl,
        summary = summary,
    )
}

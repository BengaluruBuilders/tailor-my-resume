package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.EntryCategory
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TooLongBulletNeverUsedTest {
    private val tooLong = "Built a Kotlin Android app " + "a".repeat(430) + "."
    private val profile = profileOf(
        emptyList(),
        entry("L-1", EntryCategory.EXPERIENCE, "Android developer", tooLong, "Shipped a Kotlin Android app to a small team."),
    )

    @Test
    fun offlineTailorNeverTailorsATooLongBullet() = runTest {
        val job = OfflineJobDescriptionAnalyzer().analyze("Requirements\n- Kotlin")

        val resume = OfflineResumeTailor().tailor(profile, job, OfflineGapMatcher().match(profile, job), "app-1", null, "run-1")

        assertThat(resume.bullets.flatMap { it.sourceIds }).containsExactly("L-1-b2")
    }
}

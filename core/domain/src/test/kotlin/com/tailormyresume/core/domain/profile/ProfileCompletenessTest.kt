package com.tailormyresume.core.domain.profile

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EntryCategory
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.testing.data.PrototypeFixtures
import org.junit.Test

class ProfileCompletenessTest {
    private val fresh = PrototypeFixtures.fresh().profile
    private val fixed = PrototypeFixtures.returning().profile

    @Test
    fun freshFixedAndFixedWithLinkedinGive90_96_100() {
        assertThat(ProfileCompleteness.percent(fresh)).isEqualTo(90)
        assertThat(ProfileCompleteness.percent(fixed)).isEqualTo(96)
        assertThat(ProfileCompleteness.percent(fixed.copy(linkedinUrl = "linkedin.com/in/priya"))).isEqualTo(100)
    }

    @Test
    fun neverBelowZeroWithManyGaps() {
        val gaps = (1..40).map { index ->
            ProfileEntry(
                id = "gap-$index",
                category = EntryCategory.EXPERIENCE,
                title = "Role $index",
                organization = "Acme",
                startDate = "Jun 2021",
                endDate = "",
                bullets = emptyList(),
                source = FactSource.IMPORTED,
                isConfirmed = true,
            )
        }

        assertThat(ProfileCompleteness.percent(fresh.copy(entries = gaps))).isEqualTo(0)
    }

    @Test
    fun aBlankLinkedinUrlCountsAsAbsent() {
        val spaces: CandidateProfile = fixed.copy(linkedinUrl = "   ")

        assertThat(ProfileCompleteness.percent(spaces)).isEqualTo(96)
    }
}

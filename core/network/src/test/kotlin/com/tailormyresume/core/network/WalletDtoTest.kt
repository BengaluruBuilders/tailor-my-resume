package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.network.dto.WalletDto
import org.junit.Test

class WalletDtoTest {
    private val json = tailormyresumeJson()
    private val v2 = """{"credits":6,"freeCredits":1,"purchasedCredits":5,"analysesLeftToday":19,"day":"2026-10-11","resetsAt":"2026-10-11T18:30:00Z"}"""
    private val withDeprecated =
        """{"credits":6,"freeCredits":1,"purchasedCredits":5,"analysesLeftToday":19,"freeTailoringsLeftToday":0,"day":"2026-10-11","resetsAt":"2026-10-11T18:30:00Z","unlockedApplicationIds":["a1"]}"""

    @Test
    fun decodesV2WithAndWithoutDeprecatedFields() {
        val plain = json.decodeFromString(WalletDto.serializer(), v2)
        val legacy = json.decodeFromString(WalletDto.serializer(), withDeprecated)

        assertThat(plain.credits).isEqualTo(6)
        assertThat(plain.analysesLeftToday).isEqualTo(19)
        assertThat(legacy).isEqualTo(plain)
        val wire = json.encodeToString(WalletDto.serializer(), plain)
        assertThat(wire).doesNotContain("freeTailoringsLeftToday")
        assertThat(wire).doesNotContain("unlockedApplicationIds")
    }

    @Test
    fun theApiHasNoUnlockRoute() {
        assertThat(TailorMyResumeApi::class.java.methods.map { it.name }).doesNotContain("unlock")
    }
}

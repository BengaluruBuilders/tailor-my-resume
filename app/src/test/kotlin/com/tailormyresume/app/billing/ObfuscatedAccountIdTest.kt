package com.tailormyresume.app.billing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ObfuscatedAccountIdTest {
    @Test
    fun isTheLowerCaseHexSha256OfTheUid() {
        assertThat(obfuscatedAccountId("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    @Test
    fun isSixtyFourCharacters() {
        assertThat(obfuscatedAccountId("firebase-uid-1")).hasLength(64)
    }
}

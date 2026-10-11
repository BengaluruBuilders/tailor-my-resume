package com.tailormyresume.app

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.app.di.SignInBindings
import com.tailormyresume.core.domain.account.WelcomeCreditGrant
import com.tailormyresume.core.domain.offline.LedgerWelcomeCreditGrant
import org.junit.Test

class DemoWelcomeGrantSeamTest {
    private val bound = SignInBindings::class.java.methods.associate { it.returnType to it.parameterTypes.single() }

    @Test
    fun demoGrantsTheFreeFirstResumeIntoTheLocalLedger() {
        assertThat(bound[WelcomeCreditGrant::class.java]).isEqualTo(LedgerWelcomeCreditGrant::class.java)
    }
}

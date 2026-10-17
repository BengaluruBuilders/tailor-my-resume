package com.tailormyresume.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.AccountMode
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.mock.TestMockStateStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StoredSessionGuestTest {
    @Test fun anonymousModeSurvivesStorageAndOldAccountsRemainGoogle() = runTest {
        val store = TestMockStateStore()
        val subject = StoredSessionRepository(store)
        subject.saveAccount(SignInAccount("guest", "", "", AccountMode.ANONYMOUS))
        assertThat(StoredSessionRepository(store).observeAccount().first()?.mode).isEqualTo(AccountMode.ANONYMOUS)
        store.write("session.account", """{"id":"legacy","displayName":"User","email":"user@example.com"}""")
        assertThat(subject.observeAccount().first()?.mode).isEqualTo(AccountMode.GOOGLE)
    }
}

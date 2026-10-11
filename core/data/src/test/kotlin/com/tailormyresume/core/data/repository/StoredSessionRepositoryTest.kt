package com.tailormyresume.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.KeptJobDescription
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.mock.TestMockStateStore
import com.tailormyresume.core.testing.repository.SessionRepositoryContractTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StoredSessionRepositoryTest : SessionRepositoryContractTest() {

    override fun createSessionRepository(): SessionRepository = StoredSessionRepository(TestMockStateStore())

    @Test
    fun theWholeSessionSurvivesARestartOfTheRepository() = runTest {
        val store = TestMockStateStore()
        val job = KeptJobDescription(text = "line one\nline two", company = "Northwind GCC", role = "Analyst")
        StoredSessionRepository(store).apply {
            saveAccount(SignInAccount.localAccount)
            markOnboardingComplete()
            keepJobDescription(job)
        }

        val restarted = StoredSessionRepository(store)

        assertThat(restarted.observeAccount().first()).isEqualTo(SignInAccount.localAccount)
        assertThat(restarted.observeOnboardingComplete().first()).isTrue()
        assertThat(restarted.observeKeptJobDescription().first()).isEqualTo(job)
    }

    @Test
    fun signOut_removesLegacySessionKeys() = runTest {
        val store = TestMockStateStore()
        store.write("session.consent", "{}")
        store.write("session.careerStage", "STUDENT")

        StoredSessionRepository(store).signOut()

        assertThat(store.read("session.consent")).isNull()
        assertThat(store.read("session.careerStage")).isNull()
    }

    @Test
    fun clear_removesLegacySessionKeys() = runTest {
        val store = TestMockStateStore()
        store.write("session.consent", "{}")
        store.write("session.careerStage", "STUDENT")

        StoredSessionRepository(store).clear()

        assertThat(store.read("session.consent")).isNull()
        assertThat(store.read("session.careerStage")).isNull()
    }
}

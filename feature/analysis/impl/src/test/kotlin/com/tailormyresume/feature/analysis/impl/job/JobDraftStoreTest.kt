package com.tailormyresume.feature.analysis.impl.job

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobDraftStoreTest {

    private val session = TestSessionRepository()

    @Test
    fun consumeReturnsTheDraftOnceThenNull() = runTest(UnconfinedTestDispatcher()) {
        val store = JobDraftStore(session, backgroundScope)
        store.set(text = "Role at Northwind", importedFrom = "careers.northwind.example")

        assertThat(store.consume()).isEqualTo(JobDraft(text = "Role at Northwind", importedFrom = "careers.northwind.example"))
        assertThat(store.consume()).isNull()
        assertThat(store.draft.value).isNull()
    }

    @Test
    fun markNotAJobPostStoresAnEmptyFlaggedDraft() = runTest(UnconfinedTestDispatcher()) {
        val store = JobDraftStore(session, backgroundScope)
        store.markNotAJobPost()

        assertThat(store.draft.value).isEqualTo(JobDraft(notAJobPost = true))
    }

    @Test
    fun clearDropsTheDraft() = runTest(UnconfinedTestDispatcher()) {
        val store = JobDraftStore(session, backgroundScope)
        store.set(text = "Role", importedFrom = "host")
        store.clear()

        assertThat(store.draft.value).isNull()
    }

    @Test
    fun accountChangeClearsTheDraft() = runTest(UnconfinedTestDispatcher()) {
        val store = JobDraftStore(session, backgroundScope)
        store.set(text = "Role", importedFrom = "host")

        session.saveAccount(SignInAccount(id = "account-2", displayName = "Asha", email = "asha@example.com"))

        assertThat(store.draft.value).isNull()
    }
}

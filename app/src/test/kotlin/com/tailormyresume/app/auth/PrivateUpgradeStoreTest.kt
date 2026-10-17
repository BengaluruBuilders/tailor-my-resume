package com.tailormyresume.app.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class PrivateUpgradeStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After fun clean() { File(context.filesDir, "account-upgrades").deleteRecursively() }

    @Test fun crashBeforeFirebaseReturnsIsBoundToTheChosenGoogleProvider() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val expected = GoogleCredentialIdentity.digestSubject("google-B")
        val pending = journal().copy(phase = "SWITCHING", destinationGoogleDigest = expected)
        store.write(pending)
        val restarted = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        assertThat(restarted.readFor("B", "staging", expected)).isEqualTo(pending)
        assertThat(restarted.readFor("C", "staging", GoogleCredentialIdentity.digestSubject("google-C"))).isNull()
        assertThat(restarted.readFor("B", "production", expected)).isNull()
        assertThat(restarted.readFor("C", "staging")).isNull()
    }

    @Test fun boundHandoffIsPrivateToSourceAndDestinationAndOtherJournalsSurviveClear() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val pending = journal().copy(destination = "B", phase = "SWITCHED")
        store.write(pending)
        val other = journal().copy(source = "C", prepareKey = "other")
        store.write(other)
        assertThat(store.readFor("B", "staging")).isEqualTo(pending)
        store.clear(pending)
        assertThat(store.readFor("B", "staging")).isNull()
        assertThat(store.readFor("C", "staging")).isEqualTo(other)
    }

    @Test fun deletionRevokesInFlightWritesAndErasesBothSidesOfHandoff() = runTest {
        val store = PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler))
        val pending = journal().copy(destination = "B", phase = "AUTHORIZED")
        store.write(pending)
        store.eraseFor("A", "staging")
        assertThat(store.readFor("B", "staging")).isNull()
        assertThat(runCatching { store.write(pending.copy(phase = "IMPORTED")) }.isFailure).isTrue()
        assertThat(PrivateUpgradeStore(context, UnconfinedTestDispatcher(testScheduler)).read()).isNull()
    }

    @Test fun routingDigestContainsNeitherRawCredentialNorGoogleSubject() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"google-B"}""".toByteArray())
        val token = "header.$payload.signature"
        assertThat(GoogleCredentialIdentity.digest(token)).isEqualTo(GoogleCredentialIdentity.digestSubject("google-B"))
        assertThat(GoogleCredentialIdentity.digest(token)).doesNotContain("google-B")
        assertThat(runCatching { GoogleCredentialIdentity.digest("invalid") }.isFailure).isTrue()
    }

    private fun journal() = UpgradeJournal("staging", "A", "prepare", "private-proof", completionKey = "completion", observationKey = "observation")
}

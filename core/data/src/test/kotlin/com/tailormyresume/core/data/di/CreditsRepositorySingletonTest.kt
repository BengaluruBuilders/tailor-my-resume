package com.tailormyresume.core.data.di

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.data.repository.CreditSnapshot
import com.tailormyresume.core.data.repository.CreditsRepository
import com.tailormyresume.core.data.repository.RemoteLedgerSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
object ServerLedgerModule {
    @Provides
    fun remote(): RemoteLedgerSource = object : RemoteLedgerSource {
        override val ownsLedger = true

        override fun owner() = "uid-1"

        override suspend fun fetch() = CreditSnapshot(balance = 7, entries = emptyList())
    }
}

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class CreditsRepositorySingletonTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var first: CreditsRepository

    @Inject
    lateinit var second: CreditsRepository

    @Test
    fun everyConsumerSeesTheBalanceRefreshedByAnother() = runBlocking {
        hiltRule.inject()

        first.refresh()

        assertThat(second.observeBalance().first()).isEqualTo(7)
    }
}

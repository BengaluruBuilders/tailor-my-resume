package com.tailormyresume.core.data.mock

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DelayMockLatency @Inject constructor() : MockLatency {

    override suspend fun await(operation: MockOperation) {
        delay(delayMillis(operation))
    }

    internal fun delayMillis(operation: MockOperation): Long = when (operation) {
        MockOperation.SIGN_IN -> SIGN_IN_MILLIS
        MockOperation.EXPORT_DATA -> EXPORT_DATA_MILLIS
        MockOperation.DELETE_ACCOUNT_STEP -> DELETE_ACCOUNT_STEP_MILLIS
    }

    private companion object {
        const val SIGN_IN_MILLIS = 900L
        const val EXPORT_DATA_MILLIS = 700L
        const val DELETE_ACCOUNT_STEP_MILLIS = 900L
    }
}

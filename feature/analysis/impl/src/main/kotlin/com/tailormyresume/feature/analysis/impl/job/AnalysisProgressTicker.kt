package com.tailormyresume.feature.analysis.impl.job

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal fun interface AnalysisProgressTicker {
    fun percents(): Flow<Int>
}

internal class DelayAnalysisProgressTicker @Inject constructor() : AnalysisProgressTicker {
    override fun percents(): Flow<Int> = flow {
        for (percent in STEP_PERCENT..CAP_PERCENT step STEP_PERCENT) {
            emit(percent)
            delay(STEP_MILLIS)
        }
    }

    companion object {
        const val STEP_PERCENT = 5
        const val CAP_PERCENT = 95
        const val STEP_MILLIS = 200L
    }
}

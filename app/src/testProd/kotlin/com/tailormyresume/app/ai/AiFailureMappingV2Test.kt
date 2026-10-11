package com.tailormyresume.app.ai

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AiFailure
import com.tailormyresume.core.network.ApiError
import org.junit.Test

class AiFailureMappingV2Test {
    @Test
    fun notAJobPost() {
        assertThat(ApiError.NotAJobPost.toAiFailure()).isEqualTo(AiFailure.NotAJobPost)
    }

    @Test
    fun jobImportFailed() {
        assertThat(ApiError.JobImportFailed.toAiFailure()).isEqualTo(AiFailure.JobImportFailed)
    }

    @Test
    fun existingAnalysisFailuresAreUnchanged() {
        assertThat(ApiError.AllowanceExhausted.toAiFailure()).isEqualTo(AiFailure.AllowanceExhausted)
        assertThat(ApiError.AnalysisInProgress.toAiFailure()).isEqualTo(AiFailure.AnalysisInProgress)
    }
}

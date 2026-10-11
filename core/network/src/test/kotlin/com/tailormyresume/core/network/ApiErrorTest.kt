package com.tailormyresume.core.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiErrorTest {
    @Test
    fun newCodesAndUnknown422() {
        assertThat(apiErrorOf(422, "NOT_A_JOB_POST", null)).isEqualTo(ApiError.NotAJobPost)
        assertThat(apiErrorOf(422, "JOB_IMPORT_FAILED", null)).isEqualTo(ApiError.JobImportFailed)
        assertThat(apiErrorOf(422, "SOMETHING_ELSE", null)).isEqualTo(ApiError.Unknown(422))
        assertThat(apiErrorOf(422, null, null)).isEqualTo(ApiError.Unknown(422))
        assertThat(apiErrorOf(429, "RATE_LIMITED", 7)).isEqualTo(ApiError.RateLimited(7))
    }
}

package com.tailormyresume.core.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TailoredResumeRunIdMapperTest {
    @Test
    fun runIdSurvivesTheDtoRoundTrip() {
        val resume = testTailoredResume.copy(runId = "run-7")

        assertThat(resume.asDto().runId).isEqualTo("run-7")
        assertThat(resume.asDto().asExternalModel()).isEqualTo(resume)
    }

    @Test
    fun aResumeWithoutARunIdStaysWithout() {
        assertThat(testTailoredResume.copy(runId = null).asDto().asExternalModel().runId).isNull()
    }
}

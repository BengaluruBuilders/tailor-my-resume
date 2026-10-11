package com.tailormyresume.core.database.util

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.json.TailoredResumeDto
import org.junit.Test

class TailoredResumeRunIdJsonTest {
    private val converters = JsonConverters()

    @Test
    fun runIdRoundTripsThroughTheJsonColumn() {
        val resume = TailoredResumeDto(bullets = emptyList(), runId = "run-7")

        val decoded = converters.jsonToTailoredResume(converters.tailoredResumeToJson(resume))

        assertThat(decoded?.runId).isEqualTo("run-7")
    }

    @Test
    fun resumeStoredBeforeRunIdExistedDecodesWithNullRunId() {
        val decoded = checkNotNull(converters.jsonToTailoredResume("""{"bullets":[],"entryIds":["e-1"]}"""))

        assertThat(decoded.runId).isNull()
    }
}

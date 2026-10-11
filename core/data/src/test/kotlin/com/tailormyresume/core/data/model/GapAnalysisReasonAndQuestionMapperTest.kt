package com.tailormyresume.core.data.model

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.database.json.GapAnalysisDto
import com.tailormyresume.core.model.QuickQuestion
import kotlinx.serialization.json.Json
import org.junit.Test

class GapAnalysisReasonAndQuestionMapperTest {

    private val question = QuickQuestion("r1", "Have you presented to senior leaders?", "It is a must-have.")

    @Test
    fun matchReasonAndQuestionSurviveTheJsonColumn() {
        val analysis = testGapAnalysis.copy(
            matches = testGapAnalysis.matches.map { it.copy(reason = "Used SQL daily") },
            question = question,
        )

        val restored = analysis.asDto().asExternalModel()

        assertThat(restored.question).isEqualTo(question)
        assertThat(restored.matches.map { it.reason }.distinct()).containsExactly("Used SQL daily")
    }

    @Test
    fun analysisStoredBeforeTheFieldsExistedStillReads() {
        val storedBeforeTheFields = """
            {"matches":[{"requirement":{"id":"r1","text":"SQL","type":"SKILL","priority":"MUST_HAVE","keywords":["sql"]},
            "status":"MET","evidenceIds":["e1"]}],"keywordCoverage":{"covered":1,"total":2}}
        """.trimIndent()

        val restored = Json.decodeFromString(GapAnalysisDto.serializer(), storedBeforeTheFields).asExternalModel()

        assertThat(restored.question).isNull()
        assertThat(restored.matches.map { it.reason }).containsExactly(null)
        assertThat(restored.matches.single().evidenceIds).containsExactly("e1")
        assertThat(restored.keywordCoverage.total).isEqualTo(2)
    }
}

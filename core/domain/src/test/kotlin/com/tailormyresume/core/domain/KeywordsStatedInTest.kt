package com.tailormyresume.core.domain

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.offline.SkillLexicon
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import org.junit.Test

class KeywordsStatedInTest {
    private fun requirement(vararg keywords: String) = JobRequirement(
        id = "req-1",
        text = "Experience with ${keywords.joinToString()}",
        type = RequirementType.TOOL,
        priority = RequirementPriority.MUST_HAVE,
        keywords = keywords.toList(),
    )

    @Test
    fun displayCasedSkillIsStated() {
        assertThat(keywordsStatedIn(requirement("Power BI"), "Built Power BI dashboards")).containsExactly("Power BI")
        assertThat(keywordsStatedIn(requirement("Google Sheets"), "Tracked budgets in Google Sheets"))
            .containsExactly("Google Sheets")
    }

    @Test
    fun aliasAndCaseVariantsAreStated() {
        listOf("power bi", "PowerBI", "Microsoft Power BI").forEach { keyword ->
            assertThat(keywordsStatedIn(requirement(keyword), "Built Power BI dashboards")).containsExactly("Power BI")
        }
        assertThat(keywordsStatedIn(requirement("ms excel"), "Modelled in Excel")).containsExactly("Excel")
    }

    @Test
    fun shortSkillIsNotStatedInsideSymbolJoinedToken() {
        assertThat(keywordsStatedIn(requirement("C"), "Wrote C++ code")).isEmpty()
        assertThat(keywordsStatedIn(requirement("R"), "Managed the R&D budget")).isEmpty()
        assertThat(keywordsStatedIn(requirement("Go"), "Owned the go-to-market plan")).isEmpty()
    }

    @Test
    fun nonLexiconKeywordStillMatchesByStem() {
        assertThat(keywordsStatedIn(requirement("stakeholders"), "Presented to a stakeholder")).containsExactly("stakeholders")
    }

    private fun stated(keyword: String, statement: String) = keywordsStatedIn(requirement(keyword), statement)

    @Test
    fun looseAliasesDoNotResolveToAnotherSkill() {
        assertThat(stated("SwiftUI", "Wrote Swift code")).isEmpty()
        assertThat(stated("Swift", "Built SwiftUI apps")).isEmpty()
        assertThat(stated("SwiftUI", "Built SwiftUI apps")).containsExactly("SwiftUI")
        assertThat(stated("Shell scripting", "Wrote shell scripting tools")).containsExactly("Shell scripting")
        assertThat(stated("AngularJS", "Built AngularJS apps")).containsExactly("AngularJS")
        assertThat(stated("Angular", "Built AngularJS apps")).isEmpty()
        assertThat(SkillLexicon.displayName("Shell scripting")).isEqualTo("Shell scripting")
        assertThat(SkillLexicon.displayName("AngularJS")).isEqualTo("AngularJS")
        assertThat(SkillLexicon.displayName("SwiftUI")).isEqualTo("SwiftUI")
    }

    @Test
    fun strictAliasesStillResolve() {
        assertThat(SkillLexicon.displayName("PowerBI")).isEqualTo("Power BI")
        assertThat(stated("k8s", "Ran Kubernetes clusters")).containsExactly("Kubernetes")
        assertThat(stated("js", "Wrote JavaScript")).containsExactly("JavaScript")
        assertThat(stated("ms excel", "Modelled in Excel")).containsExactly("Excel")
    }

    @Test
    fun bareGoAndExpressAreStatedOnlyAsStandaloneCapitalisedTokens() {
        assertThat(stated("Go", "Built services in Go")).containsExactly("Go")
        assertThat(stated("Go", "Built services in Golang")).containsExactly("Go")
        assertThat(stated("Express", "Built APIs with Express")).containsExactly("Express")
        assertThat(stated("Express", "Built APIs with Express.js")).containsExactly("Express")
        listOf("Owned the go-to-market plan", "We go live in May", "Go live in May", "Let's go", "Pre-Go checks").forEach {
            assertThat(stated("Go", it)).isEmpty()
        }
        listOf("Express interest in roles", "Expressed concerns", "I express thanks", "Non-Express lanes").forEach {
            assertThat(stated("Express", it)).isEmpty()
        }
    }

    @Test
    fun looseAliasContainingTheSkillNameStatesTheSkill() {
        assertThat(stated("Java", "Strong Core Java skills")).containsExactly("Java")
        assertThat(stated("Tally", "Tally ERP 9")).containsExactly("Tally")
        assertThat(stated("Excel", "Advanced Excel")).containsExactly("Excel")
        assertThat(stated("SAP", "SAP FICO")).containsExactly("SAP")
        assertThat(stated("Angular", "Built angular js apps")).containsExactly("Angular")
        assertThat(stated("angular js", "Built AngularJS apps")).isEmpty()
    }

    @Test
    fun goAndExpressNeedAnIntroducingWord() {
        assertThat(stated("Go", "Python, Go and SQL")).containsExactly("Go")
        listOf(
            "Go to the client site weekly",
            "Go through reports",
            "Go above and beyond",
            "Pokemon Go",
        ).forEach { assertThat(stated("Go", it)).isEmpty() }
        listOf(
            "Express delivery",
            "American Express",
            "Express Entry",
            "Visual Studio Express",
            "SQL Server Express",
        ).forEach { assertThat(stated("Express", it)).isEmpty() }
        assertThat(SkillLexicon.canonicalsIn("Senior Analyst, American Express")).isEmpty()
    }
}

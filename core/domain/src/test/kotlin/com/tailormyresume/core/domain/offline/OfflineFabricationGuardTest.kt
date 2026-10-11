package com.tailormyresume.core.domain.offline

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.AnswerFacts
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import org.junit.Test

class OfflineFabricationGuardTest {
    private val guard = OfflineFabricationGuard()
    private val profile = profileOf(listOf("Kotlin", "Python", "SQL", "Docker"))

    private fun check(proposed: String, vararg sources: String) =
        guard.check(proposed, sources.mapIndexed { i, text -> EvidenceBullet("s$i", text) }, profile)

    private fun assertRejected(source: String, proposed: String) {
        assertThat(check(proposed, source)).isNotEmpty()
    }

    @Test
    fun emptySourcesGiveMissingSourceOnly() {
        assertThat(guard.check("Built an app", emptyList(), profile))
            .containsExactly(GuardrailViolation.MissingSource)
    }

    @Test
    fun unchangedTextHasNoViolations() {
        assertThat(check("Built REST APIs using Kotlin", "Built REST APIs using Kotlin")).isEmpty()
    }

    @Test
    fun newPercentIsUnsupportedNumber() {
        val violations = check("Built REST APIs using Kotlin and cut latency by 40%", "Built REST APIs using Kotlin")
        assertThat(violations).contains(GuardrailViolation.UnsupportedNumber("40%"))
    }

    @Test
    fun newPlainNumberIsUnsupportedNumber() {
        val violations = check("Fixed 25 bugs", "Fixed bugs reported by QA")
        assertThat(violations).contains(GuardrailViolation.UnsupportedNumber("25"))
    }

    @Test
    fun numbersPresentInSourceAreSupported() {
        assertThat(check("Fixed 15 bugs in 2024", "In 2024 I fixed 15 bugs")).isEmpty()
    }

    @Test
    fun commaFormattedAndDecimalNumbersMatchTheirSourceForm() {
        assertThat(check("Cleaned 50000 rows", "Cleaned 50,000 rows")).isEmpty()
        assertThat(check("CGPA 8.4 out of 10", "CGPA 8.4 out of 10")).isEmpty()
        assertThat(check("CGPA 9.4 out of 10", "CGPA 8.4 out of 10"))
            .contains(GuardrailViolation.UnsupportedNumber("9.4"))
    }

    @Test
    fun percentWordAndSymbolAreTheSameNumber() {
        assertThat(check("Raised coverage to 70%", "Raised coverage to 70 percent")).isEmpty()
        assertThat(check("Raised coverage to 70", "Raised coverage to 70 percent"))
            .contains(GuardrailViolation.UnsupportedNumber("70"))
    }

    @Test
    fun tokensWithDigitsInsideWordsAreNotNumbers() {
        assertThat(check("Stored files in S3 and EC2", "Stored files in S3 and EC2")).isEmpty()
    }

    @Test
    fun numbersWithAttachedUnitsAreDetected() {
        val source = "Built API for 3 clients"
        listOf("9M", "5M", "2B", "10k", "3x", "50%", "4ms", "3Cr", "9L").forEach { number ->
            val violations = check("Built API for 3 clients serving $number requests", source)
            assertThat(violations.filterIsInstance<GuardrailViolation.UnsupportedNumber>().map { it.value })
                .contains(number)
        }
    }

    @Test
    fun attachedUnitNumberInSourceIsSupported() {
        assertThat(check("Served 9M users", "Served 9M users")).isEmpty()
    }

    @Test
    fun numberWordsMustExistInSource() {
        val violations = check("Worked in a team of five", "Worked in a team of 3")
        assertThat(violations).contains(GuardrailViolation.UnsupportedNumber("five"))
        listOf("Doubled", "twice", "dozens", "hundreds", "thousand", "ten", "twenty").forEach { word ->
            assertThat(check("Built the app $word times", "Built the app"))
                .contains(GuardrailViolation.UnsupportedNumber(word))
        }
        assertThat(check("Worked in a team of five", "Worked in a team of five")).isEmpty()
    }

    @Test
    fun numberQualifiersMustMatchTheSource() {
        assertThat(check("Handled 10+ tickets", "Handled 10 tickets"))
            .contains(GuardrailViolation.UnsupportedNumber("10+"))
        assertThat(check("Handled over 500 tickets", "Handled 500 tickets"))
            .contains(GuardrailViolation.UnsupportedNumber("over 500"))
        assertThat(check("Handled more than 500 tickets", "Handled 500 tickets")).isNotEmpty()
        assertThat(check("Handled 10+ tickets", "Handled 10+ tickets")).isEmpty()
    }

    @Test
    fun newToolIsUnsupportedTerm() {
        val violations = check("Built REST APIs using Kotlin and Kubernetes", "Built REST APIs using Kotlin")
        assertThat(violations).containsExactly(GuardrailViolation.UnsupportedTerm("Kubernetes"))
    }

    @Test
    fun termSupportedOnlyByProfileSkillsIsRejected() {
        assertThat(check("Built REST APIs using Docker", "Built REST APIs"))
            .contains(GuardrailViolation.UnsupportedTerm("Docker"))
        assertThat(check("Built APIs in Python", "Built APIs"))
            .contains(GuardrailViolation.UnsupportedTerm("Python"))
    }

    @Test
    fun impliedTermsDoNotSupportAProposal() {
        assertThat(check("Managed Social Media Marketing pages", "Managed social media pages"))
            .contains(GuardrailViolation.UnsupportedTerm("Social Media Marketing"))
        assertThat(check("Queried SQL", "Queried MySQL")).contains(GuardrailViolation.UnsupportedTerm("SQL"))
    }

    @Test
    fun aliasInSourceSupportsCanonicalTermInProposal() {
        assertThat(check("Fixed bugs in the JavaScript frontend", "Fixed bugs in the js frontend")).isEmpty()
        assertThat(check("Analysed data in Excel", "Analysed data in ms excel")).isEmpty()
    }

    @Test
    fun looseAliasDoesNotSupportItsCanonicalTerm() {
        val violations = check("Administered Linux servers", "Administered Unix servers")
        assertThat(violations).contains(GuardrailViolation.UnsupportedTerm("Linux"))
        assertThat(check("Managed Supply Chain", "Managed SCM")).isNotEmpty()
    }

    @Test
    fun javaDoesNotSupportJavaScript() {
        val violations = check("Built a JavaScript app", "Built a Java app")
        assertThat(violations).contains(GuardrailViolation.UnsupportedTerm("JavaScript"))
    }

    @Test
    fun cPlusPlusDoesNotSupportC() {
        val violations = check("Wrote drivers in C", "Wrote drivers in C++")
        assertThat(violations).contains(GuardrailViolation.UnsupportedTerm("C"))
    }

    @Test
    fun anyNewWordIsRejected() {
        val violations = check(
            "Built a production login screen in Kotlin for Google Pay",
            "Built a login screen in Kotlin",
        )
        assertThat(violations).containsAtLeast(
            GuardrailViolation.UnsupportedTerm("production"),
            GuardrailViolation.UnsupportedTerm("Google"),
            GuardrailViolation.UnsupportedTerm("Pay"),
        )
    }

    @Test
    fun jobKeywordAndNameAdditionsAreRejected() {
        assertRejected("Built REST APIs", "Built REST APIs for Google Pay")
        assertRejected("Built an app", "Built an app for Infosys")
        assertRejected("Familiar with Docker", "Familiar with Docker in production")
        assertThat(check("Used Docker in production", "Familiar with Docker"))
            .containsAtLeast(
                GuardrailViolation.UnsupportedTerm("Used"),
                GuardrailViolation.UnsupportedTerm("production"),
            )
        assertThat(check("Used Docker in production at scale", "Familiar with Docker"))
            .contains(GuardrailViolation.UnsupportedTerm("scale"))
    }

    @Test
    fun newStopWordsMustAppearInTheSource() {
        assertThat(check("Built the app by hand", "Built the app"))
            .containsAtLeast(GuardrailViolation.UnsupportedTerm("by"), GuardrailViolation.UnsupportedTerm("hand"))
        assertThat(check("Built the app on time", "Built the app")).isNotEmpty()
    }

    @Test
    fun joinerWordsAreAllowedEvenWhenAbsentFromSource() {
        assertThat(check("Using Kotlin, built the app", "Built app Kotlin")).isEmpty()
        assertThat(check("Built an app for the testers", "Built app testers")).isEmpty()
    }

    @Test
    fun assistedToLedIsVerbEscalation() {
        val violations = check("Led the migration to a new database", "Assisted with migration to a new database")
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "led"))
    }

    @Test
    fun helpedToOwnedIsVerbEscalation() {
        val violations = check("Owned the reporting process", "Helped with the reporting process")
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "helped", to = "owned"))
    }

    @Test
    fun contributedToDevelopedIsVerbEscalation() {
        val violations = check("Built the payment screen", "Contributed to the payment screen")
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "contributed", to = "built"))
    }

    @Test
    fun strongVerbsAbsentFromTheOldLadderAreEscalations() {
        val source = "Assisted with deployments"
        assertThat(check("Drove deployments", source))
            .contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "drove"))
        assertThat(check("Responsible for deployments", source))
            .contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "responsible for"))
        assertThat(check("In charge of deployments", source))
            .contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "in charge of"))
    }

    @Test
    fun everyAddedOwnershipWordIsEscalatedFromAssisted() {
        val words = listOf(
            "Launched", "Delivered", "Founded", "Pioneered", "Championed", "Oversaw", "Ran", "Mentored",
            "Spearheaded", "Directed", "Orchestrated", "Architected", "Supervised", "Headed", "Managed",
            "Lead", "Manage", "Own", "Develop", "Build", "Drove", "Owned", "Led",
        )
        words.forEach { word ->
            val violations = check("$word deployments", "Assisted with deployments")
            assertThat(violations.filterIsInstance<GuardrailViolation.VerbEscalation>()).isNotEmpty()
        }
    }

    @Test
    fun synonymsShareTheirRungButNewWordsAreStillRejected() {
        listOf("Created" to "Built", "Supported" to "Helped", "Managed" to "Headed").forEach { (proposed, source) ->
            val violations = check("$proposed the report", "$source the report")
            assertThat(violations.filterIsInstance<GuardrailViolation.VerbEscalation>()).isEmpty()
            assertThat(violations).contains(GuardrailViolation.UnsupportedTerm(proposed))
        }
    }

    @Test
    fun weakerVerbThanSourceIsNotAnEscalationButIsANewWord() {
        val violations = check("Assisted with the report", "Led the report")
        assertThat(violations.filterIsInstance<GuardrailViolation.VerbEscalation>()).isEmpty()
        assertThat(violations).contains(GuardrailViolation.UnsupportedTerm("Assisted"))
    }

    @Test
    fun verbAddedWhereSourceHadNoOwnershipVerbIsEscalation() {
        val violations = check("Led the coding club", "Coding club coordinator")
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "none", to = "led"))
    }

    @Test
    fun leadingVerbEscalationIsCaughtEvenWhenSourceContainsAStrongerLaterVerb() {
        val violations = check("Built the dashboard", "Assisted a senior analyst in building the dashboard")
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "assisted", to = "built"))
    }

    @Test
    fun sourceVerbPhraseCoversItself() {
        assertThat(check("Responsible for deployments", "Responsible for deployments")).isEmpty()
    }

    @Test
    fun teamOfWithoutSourceIsScaleClaimAndNumber() {
        val violations = check("Led a team of 10 to build the app", "Built the app")
        assertThat(violations).contains(GuardrailViolation.UnsupportedScaleClaim("team of 10"))
        assertThat(violations).contains(GuardrailViolation.UnsupportedNumber("10"))
        assertThat(violations).contains(GuardrailViolation.VerbEscalation(from = "built", to = "led"))
    }

    @Test
    fun scaleWordsNeedASource() {
        assertThat(check("Built an app for customers", "Built an app"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("customers"))
        assertThat(check("Improved revenue tracking", "Improved tracking"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("revenue"))
        assertThat(check("Saved Rs 5 lakh", "Saved Rs 5"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("lakh"))
        assertThat(check("Served users", "Built an app"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("users"))
    }

    @Test
    fun extendedScaleNounsNeedASource() {
        listOf("employees", "students", "members", "people", "engineers", "reports", "budget", "sales", "transactions", "requests", "downloads")
            .forEach { noun ->
                assertThat(check("Built an app for $noun", "Built an app"))
                    .contains(GuardrailViolation.UnsupportedScaleClaim(noun))
            }
    }

    @Test
    fun userInterfaceDoesNotSupportUsers() {
        val violations = check("Designed a login screen used by users", "Designed the user interface for a login screen")
        assertThat(violations).contains(GuardrailViolation.UnsupportedScaleClaim("users"))
    }

    @Test
    fun ordinaryCompoundsDoNotSupportScaleNouns() {
        assertThat(check("Built validation for clients", "Built client-side validation"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("clients"))
        assertThat(check("Handled customers", "Handled customer service"))
            .contains(GuardrailViolation.UnsupportedScaleClaim("customers"))
        assertThat(check("Wrote a user story", "Wrote a user story")).isEmpty()
    }

    @Test
    fun scaleWordInSourceIsSupportedInEitherNumber() {
        assertThat(check("Helped user log in", "Helped users log in")).isEmpty()
        assertThat(check("Worked in a team of 4", "Worked in a team of 4")).isEmpty()
    }

    @Test
    fun multipleViolationsAreAllReported() {
        val violations = check("Led a team of 12 using Docker and cut costs by 30%", "Assisted the team")
        assertThat(violations.filterIsInstance<GuardrailViolation.UnsupportedNumber>().map { it.value })
            .containsAtLeast("12", "30%")
        assertThat(violations).contains(GuardrailViolation.UnsupportedTerm("Docker"))
        assertThat(violations.filterIsInstance<GuardrailViolation.VerbEscalation>()).hasSize(1)
    }

    @Test
    fun cleanRewritesProduceNoViolations() {
        val source = "Responsible for building REST APIs using Kotlin and Spring Boot for the inventory module"
        listOf(
            "Building REST APIs using Kotlin and Spring Boot for the inventory module",
            "Using Kotlin and Spring Boot, responsible for building REST APIs for the inventory module",
            "Building REST APIs using Kotlin and Spring Boot",
        ).forEach { assertThat(check(it, source)).isEmpty() }
    }

    @Test
    fun rewordedAliasIsClean() {
        assertThat(check("Fixing bugs in the JavaScript frontend", "Worked on the task of fixing bugs in the js frontend"))
            .isEmpty()
    }

    private val answerRequirement = JobRequirement(
        id = "req-1",
        text = "Presented quarterly results to the board for 3+ years using Looker",
        type = RequirementType.EXPERIENCE,
        priority = RequirementPriority.MUST_HAVE,
        keywords = listOf("looker"),
    )
    private val answerJob = JobDescription("Analyst", "Northwind", "raw", listOf(answerRequirement))

    private fun answerSource(choice: String, detail: String): EvidenceBullet =
        checkNotNull(AnswerFacts.factOf(QuickAnswer("req-1", choice, detail), answerJob))

    private fun checkAgainstAnswer(proposed: String, detail: String, choice: String = AnswerFacts.YES_REGULARLY) =
        guard.check(proposed, listOf(answerSource(choice, detail)), profile)

    @Test
    fun answerBulletFromDetailPasses() {
        val detail = "Presented the monthly variance report to the CFO"

        assertThat(checkAgainstAnswer(detail, detail)).isEmpty()
        assertThat(checkAgainstAnswer("Presented the variance report to the CFO", detail)).isEmpty()
    }

    @Test
    fun answerBulletAddingTermNumberOrScaleIsFlagged() {
        val detail = "Presented the monthly variance report to the CFO"

        assertThat(checkAgainstAnswer("$detail using Python", detail)).contains(GuardrailViolation.UnsupportedTerm("Python"))
        assertThat(checkAgainstAnswer("$detail 12 times", detail)).contains(GuardrailViolation.UnsupportedNumber("12"))
        assertThat(checkAgainstAnswer("$detail to 500 customers", detail)).isNotEmpty()
        assertThat(checkAgainstAnswer("Led the monthly variance report to the CFO", detail))
            .contains(GuardrailViolation.VerbEscalation(from = "none", to = "led"))
    }

    @Test
    fun answerWithoutDetailNeverBorrowsRequirementNumbersOrWording() {
        val violations = checkAgainstAnswer("Presented quarterly results to the board for 3 years", detail = "", A_FEW)

        assertThat(violations).contains(GuardrailViolation.UnsupportedNumber("3"))
        assertThat(violations).isNotEmpty()
    }

    @Test
    fun answerKeywordsGroundANamedToolInGeneralTerms() {
        assertThat(checkAgainstAnswer("Looker", detail = "", A_FEW)).isEmpty()
    }

    @Test
    fun summarySentenceWithoutSourceFallsBack() {
        assertThat(guard.check("Analyst who builds dashboards.", emptyList(), profile))
            .containsExactly(GuardrailViolation.MissingSource)
        assertThat(check("Analyst who builds dashboards in Tableau.", "Built dashboards in Excel")).isNotEmpty()
    }

    private companion object {
        const val A_FEW = AnswerFacts.A_FEW_TIMES
    }
}

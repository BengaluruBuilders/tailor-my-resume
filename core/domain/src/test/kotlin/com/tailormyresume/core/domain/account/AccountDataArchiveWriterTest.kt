package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.domain.PurchaseState
import com.tailormyresume.core.model.BulletDecision
import com.tailormyresume.core.model.CreditKind
import com.tailormyresume.core.model.EditType
import com.tailormyresume.core.model.ExportFormat
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.GuardrailViolation
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.RequirementPriority
import com.tailormyresume.core.model.RequirementType
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import com.tailormyresume.core.testing.data.sampleApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile
import kotlin.time.Instant

class AccountDataArchiveWriterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now = Instant.fromEpochMilliseconds(1_790_000_000_000)

    private val terraformRequirement = JobRequirement(
        id = "req-terraform",
        text = "Terraform pipelines on AWS",
        type = RequirementType.TOOL,
        priority = RequirementPriority.NICE_TO_HAVE,
        keywords = listOf("terraform"),
    )

    private val application = sampleApplication.copy(
        job = sampleApplication.job.copy(
            rawText = "Senior Engineer | Bengaluru | Full-time\nResponsibilities:\nShip Kotlin apps",
            requirements = sampleApplication.job.requirements + terraformRequirement,
        ),
        notes = "Call the recruiter on Tuesday\nAsk about the 2nd round",
        tailoredResume = TailoredResume(
            entryIds = listOf("entry-1", "entry-2"),
            bullets = listOf(
                TailoredBullet(
                    id = "tailored-1",
                    entryId = "entry-1",
                    originalText = "Built a Kotlin app",
                    proposedText = "Built a Kotlin Android app",
                    sourceIds = listOf("bullet-project-1"),
                    editTypes = listOf(EditType.REWORD),
                    keywordsUsed = listOf("kotlin"),
                    violations = listOf(GuardrailViolation.UnsupportedTerm("terraform")),
                    decision = BulletDecision.ACCEPTED,
                ),
                TailoredBullet(
                    id = "tailored-2",
                    entryId = "entry-2",
                    originalText = "Moved services",
                    proposedText = "Led the migration\nCut cost 30%",
                    sourceIds = listOf("bullet-project-1"),
                    editTypes = listOf(EditType.REWORD),
                    keywordsUsed = emptyList(),
                    violations = emptyList(),
                    decision = BulletDecision.ACCEPTED,
                ),
            ),
        ),
    )

    private val data = AccountData(
        generatedAt = now,
        account = SignInAccount.localAccount.copy(id = PLANTED_UID),
        profile = canonicalCandidateProfile,
        applications = listOf(application),
        entitlement = PurchaseEntitlement(
            freeCredits = 1,
            purchasedCredits = 4,
            pendingPackIds = listOf("application_pack_10"),
            unlockedApplicationIds = setOf(application.id),
        ),
        purchases = listOf(PurchaseRecord("application_pack_5", "order-1", now, PurchaseState.COMPLETED)),
        exports = listOf(ExportRecord(application.id, ExportFormat.PDF, "resume.pdf", now, CreditKind.PURCHASED, 2, "Classic")),
    )

    private companion object {
        const val PLANTED_UID = "planted-account-id-4471"
        val SUSPICIOUS_FRAGMENTS = listOf("token", "uid", "secret", "key")
        val ALLOWED_NAMES = setOf("keywords", "keywordsUsed", "keywordCoverage")
    }

    private fun archive(extra: Map<String, String> = emptyMap()): Map<String, String> {
        val target = File(folder.root, "archive.zip")
        AccountDataArchiveWriter().write(data, target, extra)
        return ZipFile(target).use { zip ->
            zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
        }
    }

    private fun myData(): JsonObject =
        Json.parseToJsonElement(archive().getValue("my-data.json")).jsonObject

    private fun JsonObject.array(name: String): JsonArray = getValue(name).jsonArray

    @Test
    fun myDataJsonHoldsEveryApplicationField() {
        val entry = myData().array("applications").single().jsonObject

        assertThat(entry.getValue("id").jsonPrimitive.content).isEqualTo(application.id)
        assertThat(entry.getValue("role").jsonPrimitive.content).isEqualTo(application.job.title)
        assertThat(entry.getValue("company").jsonPrimitive.content).isEqualTo(application.job.company)
        assertThat(entry.getValue("status").jsonPrimitive.content).isEqualTo("SAVED")
        assertThat(entry.getValue("notes").jsonPrimitive.content).isEqualTo("Call the recruiter on Tuesday\nAsk about the 2nd round")
        assertThat(entry.getValue("createdAt").jsonPrimitive.content).isEqualTo(application.createdAt.toString())
        assertThat(entry.getValue("updatedAt").jsonPrimitive.content).isEqualTo(application.updatedAt.toString())

        val job = entry.getValue("job").jsonObject
        assertThat(job.getValue("rawText").jsonPrimitive.content).isEqualTo(application.job.rawText)
        val requirement = job.array("requirements").first().jsonObject
        assertThat(requirement.getValue("type").jsonPrimitive.content).isEqualTo("SKILL")
        assertThat(requirement.getValue("priority").jsonPrimitive.content).isEqualTo("MUST_HAVE")
        assertThat(requirement.array("keywords").map { it.jsonPrimitive.content }).containsExactly("kotlin")

        val analysis = entry.getValue("gapAnalysis").jsonObject
        val match = analysis.array("matches").first().jsonObject
        assertThat(match.getValue("status").jsonPrimitive.content).isEqualTo("MET")
        assertThat(match.array("evidenceIds").map { it.jsonPrimitive.content }).containsExactly("bullet-project-1")
        assertThat(analysis.getValue("keywordCoverage").jsonObject.getValue("covered").jsonPrimitive.int).isEqualTo(1)

        val bullet = entry.getValue("tailoredResume").jsonObject.array("bullets").first().jsonObject
        assertThat(bullet.getValue("original").jsonPrimitive.content).isEqualTo("Built a Kotlin app")
        assertThat(bullet.getValue("proposed").jsonPrimitive.content).isEqualTo("Built a Kotlin Android app")
        assertThat(bullet.getValue("decision").jsonPrimitive.content).isEqualTo("ACCEPTED")
        assertThat(bullet.array("sourceIds").map { it.jsonPrimitive.content }).containsExactly("bullet-project-1")

        assertThat(bullet.array("editTypes").map { it.jsonPrimitive.content }).containsExactly("REWORD")
        assertThat(bullet.array("keywordsUsed").map { it.jsonPrimitive.content }).containsExactly("kotlin")
        assertThat(bullet.array("violations").single().jsonPrimitive.content).contains("terraform")
        val resume = entry.getValue("tailoredResume").jsonObject
        assertThat(resume.array("entryIds").map { it.jsonPrimitive.content }).containsExactly("entry-1", "entry-2").inOrder()

        val export = entry.array("exports").single().jsonObject
        assertThat(export.getValue("fileName").jsonPrimitive.content).isEqualTo("resume.pdf")
        assertThat(export.getValue("creditKind").jsonPrimitive.content).isEqualTo("PURCHASED")
        assertThat(export.getValue("pageCount").jsonPrimitive.int).isEqualTo(2)
        assertThat(export.getValue("templateName").jsonPrimitive.content).isEqualTo("Classic")
    }

    @Test
    fun myDataJsonHoldsAccountProfileAndPurchases() {
        val root = myData()

        val account = root.getValue("account").jsonObject
        assertThat(account.getValue("email").jsonPrimitive.content).isEqualTo(SignInAccount.localAccount.email)
        assertThat(account.getValue("displayName").jsonPrimitive.content).isEqualTo(SignInAccount.localAccount.displayName)

        val profile = root.getValue("profile").jsonObject
        assertThat(profile.getValue("fullName").jsonPrimitive.content).isEqualTo(canonicalCandidateProfile.fullName)
        assertThat(profile.array("skills")).hasSize(canonicalCandidateProfile.skills.size)
        val entries = profile.array("entries")
        assertThat(entries).hasSize(canonicalCandidateProfile.entries.size)
        assertThat(entries.first().jsonObject.keys).containsAtLeast("source", "isConfirmed", "bullets")

        val entitlement = root.getValue("entitlement").jsonObject
        assertThat(entitlement.getValue("freeCredits").jsonPrimitive.int).isEqualTo(1)
        assertThat(entitlement.getValue("purchasedCredits").jsonPrimitive.int).isEqualTo(4)
        assertThat(entitlement.array("unlockedApplicationIds").map { it.jsonPrimitive.content }).containsExactly(application.id)
        assertThat(entitlement.array("pendingPackIds").map { it.jsonPrimitive.content }).containsExactly("application_pack_10")
        val purchase = root.array("purchases").single().jsonObject
        assertThat(purchase.getValue("orderId").jsonPrimitive.content).isEqualTo("order-1")
        assertThat(purchase.getValue("packId").jsonPrimitive.content).isEqualTo("application_pack_5")
        assertThat(purchase.getValue("state").jsonPrimitive.content).isEqualTo("COMPLETED")
    }

    @Test
    fun myDataJsonKeepsWhichSkillsTheUserStatedThemselves() {
        val withStated = canonicalCandidateProfile.copy(userStatedSkills = listOf("SQL"))
        val target = File(folder.root, "stated.zip")
        AccountDataArchiveWriter().write(data.copy(profile = withStated), target, emptyMap())
        val json = ZipFile(target).use { zip ->
            Json.parseToJsonElement(zip.getInputStream(zip.getEntry("my-data.json")).readBytes().decodeToString()).jsonObject
        }

        val profile = json.getValue("profile").jsonObject
        assertThat(profile.array("userStatedSkills").map { it.jsonPrimitive.content }).containsExactly("SQL")
        assertThat(profile.array("skills")).hasSize(canonicalCandidateProfile.skills.size)
    }

    @Test
    fun noEntryHoldsATokenOrKey() {
        val files = archive()
        val everything = files.values.joinToString("\n").lowercase()

        assertThat(everything).doesNotContain(PLANTED_UID.lowercase())
        listOf("token", "obfuscatedaccountid", "apikey").forEach { forbidden ->
            assertThat(everything).doesNotContain(forbidden)
        }
        val names = jsonKeys(Json.parseToJsonElement(files.getValue("my-data.json"))) +
            AccountData::class.java.declaredFields.map { it.name } +
            SignInAccount::class.java.declaredFields.map { it.name }
        val suspicious = names.filter { name ->
            name !in ALLOWED_NAMES && SUSPICIOUS_FRAGMENTS.any { name.lowercase().contains(it) }
        }
        assertThat(suspicious).isEmpty()
    }

    private fun jsonKeys(element: JsonElement): List<String> = when (element) {
        is JsonObject -> element.keys.toList() + element.values.flatMap(::jsonKeys)
        is JsonArray -> element.flatMap(::jsonKeys)
        else -> emptyList()
    }

    @Test
    fun applicationsTextShowsJdAnalysisResumeAndNotes() {
        val text = archive().getValue("applications.txt")

        assertThat(text).contains("jobDescription: Senior Engineer | Bengaluru | Full-time")
        assertThat(text).contains("requirement NICE_TO_HAVE TOOL: Terraform pipelines on AWS")
        assertThat(text).contains("MET")
        assertThat(text).contains("Built a Kotlin Android app")
        assertThat(text).contains("Call the recruiter on Tuesday")
    }

    @Test
    fun everyLineOfAMultiLineValueIsIndented() {
        val lines = archive().getValue("applications.txt").lines().filter { it.isNotEmpty() }

        val atColumnZero = lines.filter { !it.startsWith(" ") }
        assertThat(atColumnZero).hasSize(1)
        assertThat(atColumnZero.single()).startsWith(application.id)
        assertThat(lines).contains("  jobDescription: Senior Engineer | Bengaluru | Full-time")
        assertThat(lines).contains("    Responsibilities:")
        assertThat(lines).contains("    Ask about the 2nd round")
        assertThat(lines).contains("    Cut cost 30% (was: Moved services)")
    }

    @Test
    fun extraEntriesStayNextToTheLocalFiles() {
        val files = archive(mapOf("server.json" to "{}"))

        assertThat(files.keys).containsExactly(
            "account.txt",
            "profile.txt",
            "applications.txt",
            "purchases.txt",
            "my-data.json",
            "server.json",
        )
    }
}

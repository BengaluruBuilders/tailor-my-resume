package com.tailormyresume.core.domain.account

import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.SignInAccount
import com.tailormyresume.core.model.TailoredBullet
import com.tailormyresume.core.model.TailoredResume
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AccountDataArchiveWriter {

    fun write(data: AccountData, target: File, extraEntries: Map<String, String> = emptyMap()) {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, target.name + TEMPORARY_SUFFIX)
        try {
            ZipOutputStream(temporary.outputStream()).use { zip ->
                zip.writeEntry(ACCOUNT_ENTRY, accountText(data))
                zip.writeEntry(PROFILE_ENTRY, profileText(data))
                zip.writeEntry(APPLICATIONS_ENTRY, applicationsText(data))
                zip.writeEntry(PURCHASES_ENTRY, purchasesText(data))
                zip.writeEntry(MY_DATA_ENTRY, myDataJson(data))
                extraEntries.forEach { (name, body) -> zip.writeEntry(name, body) }
            }
            if (!temporary.renameTo(target)) throw IOException("Could not move the archive to ${target.name}")
        } finally {
            temporary.delete()
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, body: String) {
        putNextEntry(ZipEntry(name).apply { time = PINNED_TIME })
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun accountText(data: AccountData): String = buildString {
        appendLine("generatedAt=${data.generatedAt}")
        appendLine("account=${data.account?.email.orEmpty()}")
    }

    private fun profileText(data: AccountData): String = buildString {
        val profile = data.profile ?: return@buildString
        appendLine("${profile.fullName} | ${profile.headline} | ${profile.email} | ${profile.phone}")
        appendLine("skills=${profile.skills.joinToString(",")}")
        appendLine("userStatedSkills=${profile.userStatedSkills.joinToString(",")}")
        profile.entries.forEach { entry ->
            appendLine("${entry.id} | ${entry.category} | ${entry.title} | ${entry.organization} | confirmed=${entry.isConfirmed}")
            entry.bullets.forEach { bullet -> appendIndented(bullet.id, bullet.text) }
        }
    }

    private fun applicationsText(data: AccountData): String = buildString {
        data.applications.forEach { application ->
            appendLine("${application.id} | ${application.job.title} | ${application.job.company} | ${application.status}")
            appendIndented("jobDescription", application.job.rawText)
            application.job.requirements.forEach { appendLine("  requirement ${it.priority} ${it.type}: ${it.text}") }
            application.gapAnalysis?.let { analysis ->
                analysis.matches.forEach { appendLine("  analysis ${it.status}: ${it.requirement.text}") }
                appendLine("  keywordCoverage=${analysis.keywordCoverage.covered}/${analysis.keywordCoverage.total}")
            }
            application.tailoredResume?.bullets?.forEach { bullet ->
                appendIndented("resume ${bullet.decision}", "${bullet.proposedText} (was: ${bullet.originalText})")
            }
            if (application.notes.isNotBlank()) appendIndented("notes", application.notes)
            val exports = data.exports.filter { it.applicationId == application.id }
            exports.forEach { export -> appendLine("  export ${export.format} ${export.fileName} ${export.exportedAt}") }
        }
    }

    private fun StringBuilder.appendIndented(label: String, value: String) {
        val lines = value.lines()
        appendLine("  $label: ${lines.first()}")
        lines.drop(1).forEach { appendLine("    $it") }
    }

    private fun purchasesText(data: AccountData): String = buildString {
        appendLine("freeCredits=${data.entitlement.freeCredits}")
        appendLine("purchasedCredits=${data.entitlement.purchasedCredits}")
        data.purchases.forEach { purchase ->
            appendLine("${purchase.orderId} | ${purchase.packId} | ${purchase.state} | ${purchase.purchasedAt}")
        }
    }

    private fun myDataJson(data: AccountData): String = prettyJson.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("generatedAt", data.generatedAt.toString())
            data.account?.let { put("account", accountJson(it)) }
            data.profile?.let { put("profile", profileJson(it)) }
            put("entitlement", entitlementJson(data.entitlement))
            put("purchases", objects(data.purchases, ::purchaseJson))
            put("applications", objects(data.applications) { applicationJson(it, data) })
        },
    )

    private fun accountJson(account: SignInAccount): JsonObject = buildJsonObject {
        put("email", account.email)
        put("displayName", account.displayName)
    }

    private fun entitlementJson(entitlement: PurchaseEntitlement): JsonObject = buildJsonObject {
        put("freeCredits", entitlement.freeCredits)
        put("purchasedCredits", entitlement.purchasedCredits)
        put("unlockedApplicationIds", strings(entitlement.unlockedApplicationIds.sorted()))
        put("pendingPackIds", strings(entitlement.pendingPackIds))
    }

    private fun purchaseJson(purchase: PurchaseRecord): JsonObject = buildJsonObject {
        put("packId", purchase.packId)
        put("orderId", purchase.orderId)
        put("state", purchase.state.name)
        put("purchasedAt", purchase.purchasedAt.toString())
    }

    private fun profileJson(profile: CandidateProfile): JsonObject = buildJsonObject {
        put("fullName", profile.fullName)
        put("email", profile.email)
        put("phone", profile.phone)
        put("headline", profile.headline)
        put("skills", strings(profile.skills))
        put("userStatedSkills", strings(profile.userStatedSkills))
        put("entries", objects(profile.entries, ::profileEntryJson))
    }

    private fun profileEntryJson(entry: ProfileEntry): JsonObject = buildJsonObject {
        put("id", entry.id)
        put("category", entry.category.name)
        put("title", entry.title)
        put("organization", entry.organization)
        put("startDate", entry.startDate)
        put("endDate", entry.endDate)
        put("source", entry.source.name)
        put("isConfirmed", entry.isConfirmed)
        put(
            "bullets",
            objects(entry.bullets) { bullet ->
                buildJsonObject {
                    put("id", bullet.id)
                    put("text", bullet.text)
                }
            },
        )
    }

    private fun applicationJson(application: JobApplication, data: AccountData): JsonObject = buildJsonObject {
        put("id", application.id)
        put("role", application.job.title)
        put("company", application.job.company)
        put("status", application.status.name)
        put("notes", application.notes)
        put("createdAt", application.createdAt.toString())
        put("updatedAt", application.updatedAt.toString())
        put("job", jobJson(application.job))
        application.gapAnalysis?.let { put("gapAnalysis", gapAnalysisJson(it)) }
        application.tailoredResume?.let { put("tailoredResume", tailoredResumeJson(it)) }
        put("exports", objects(data.exports.filter { it.applicationId == application.id }, ::exportJson))
    }

    private fun jobJson(job: JobDescription): JsonObject = buildJsonObject {
        put("rawText", job.rawText)
        put("requirements", objects(job.requirements, ::requirementJson))
    }

    private fun gapAnalysisJson(analysis: GapAnalysis): JsonObject = buildJsonObject {
        put(
            "matches",
            objects(analysis.matches) { match ->
                buildJsonObject {
                    put("requirementId", match.requirement.id)
                    put("status", match.status.name)
                    put("evidenceIds", strings(match.evidenceIds))
                }
            },
        )
        put(
            "keywordCoverage",
            buildJsonObject {
                put("covered", analysis.keywordCoverage.covered)
                put("total", analysis.keywordCoverage.total)
            },
        )
    }

    private fun tailoredResumeJson(resume: TailoredResume): JsonObject = buildJsonObject {
        resume.entryIds?.let { put("entryIds", strings(it)) }
        put("bullets", objects(resume.bullets, ::tailoredBulletJson))
    }

    private fun tailoredBulletJson(bullet: TailoredBullet): JsonObject = buildJsonObject {
        put("id", bullet.id)
        put("entryId", bullet.entryId)
        put("original", bullet.originalText)
        put("proposed", bullet.proposedText)
        put("sourceIds", strings(bullet.sourceIds))
        put("editTypes", strings(bullet.editTypes.map { it.name }))
        put("keywordsUsed", strings(bullet.keywordsUsed))
        put("violations", strings(bullet.violations.map { it.toString() }))
        put("decision", bullet.decision.name)
    }

    private fun exportJson(export: ExportRecord): JsonObject = buildJsonObject {
        put("format", export.format.name)
        put("fileName", export.fileName)
        put("exportedAt", export.exportedAt.toString())
        put("creditKind", export.creditKind?.name)
        put("pageCount", export.pageCount)
        put("templateName", export.templateName)
    }

    private fun requirementJson(requirement: JobRequirement): JsonObject = buildJsonObject {
        put("id", requirement.id)
        put("text", requirement.text)
        put("type", requirement.type.name)
        put("priority", requirement.priority.name)
        put("keywords", strings(requirement.keywords))
    }

    private fun strings(values: List<String>): JsonArray = buildJsonArray { values.forEach { add(it) } }

    private fun <T> objects(values: List<T>, toJson: (T) -> JsonObject): JsonArray =
        buildJsonArray { values.forEach { add(toJson(it)) } }

    private companion object {
        const val ACCOUNT_ENTRY = "account.txt"
        const val PROFILE_ENTRY = "profile.txt"
        const val APPLICATIONS_ENTRY = "applications.txt"
        const val PURCHASES_ENTRY = "purchases.txt"
        const val MY_DATA_ENTRY = "my-data.json"
        val prettyJson = Json { prettyPrint = true }
        const val TEMPORARY_SUFFIX = ".tmp"
        const val PINNED_TIME = 1_000_000_000_000L
    }
}

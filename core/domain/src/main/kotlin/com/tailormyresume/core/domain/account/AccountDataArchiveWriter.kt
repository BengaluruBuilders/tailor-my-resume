package com.tailormyresume.core.domain.account

import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.domain.PurchaseRecord
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.ExportRecord
import com.tailormyresume.core.model.GapAnalysis
import com.tailormyresume.core.model.JobApplication
import com.tailormyresume.core.model.JobDescription
import com.tailormyresume.core.model.JobRequirement
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.ResumeSettings
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
        appendLine("pageSize=${data.resumeSettings.pageSize}")
        appendLine("fileNameFormat=${data.resumeSettings.fileNameFormat}")
        appendLine("productUpdates=${data.resumeSettings.productUpdates}")
    }

    private fun profileText(data: AccountData): String = buildString {
        val profile = data.profile ?: return@buildString
        appendLine("${profile.fullName} | ${profile.headline} | ${profile.email} | ${profile.phone}")
        appendLine("city=${profile.city}")
        appendLine("linkedinUrl=${profile.linkedinUrl}")
        appendLine("portfolioUrl=${profile.portfolioUrl}")
        appendLine("sourceFileName=${profile.sourceFileName.orEmpty()}")
        appendLine("reviewedAt=${profile.reviewedAt?.toString().orEmpty()}")
        appendIndented("summary", profile.summary)
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
            appendLine("  location=${application.location}")
            appendLine("  appliedOn=${application.appliedOn?.toString().orEmpty()}")
            appendLine("  exportFileName=${application.exportFileName.orEmpty()}")
            appendLine("  changesAcceptedAt=${application.changesAcceptedAt?.toString().orEmpty()}")
            if (application.legacyNotes.isNotEmpty()) appendIndented("notes", application.legacyNotes)
            application.legacyStatus?.let { appendLine("  legacyStatus=$it") }
            application.keywordCoverage?.let { appendLine("  keywordCoverage now=${it.now} upTo=${it.upTo} final=${it.final ?: ""}") }
            application.quickAnswer?.let { appendIndented("quickAnswer ${it.requirementId} ${it.choice}", it.detail) }
            appendIndented("jobDescription", application.job.rawText)
            application.job.requirements.forEach { appendLine("  requirement ${it.priority} ${it.type}: ${it.text}") }
            application.gapAnalysis?.let { analysis ->
                analysis.matches.forEach { appendLine("  analysis ${it.status}: ${it.requirement.text}") }
                appendLine("  keywordCoverage=${analysis.keywordCoverage.covered}/${analysis.keywordCoverage.total}")
            }
            application.tailoredResume?.bullets?.forEach { bullet ->
                appendIndented("resume ${bullet.decision}", "${bullet.proposedText} (was: ${bullet.originalText})")
            }
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
        data.creditLedger.forEach { entry ->
            appendLine("ledger ${entry.createdAt} | ${entry.kind} | ${entry.amount} | ${entry.applicationId.orEmpty()} | ${entry.productId.orEmpty()}")
        }
    }

    private fun myDataJson(data: AccountData): String = prettyJson.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("generatedAt", data.generatedAt.toString())
            data.account?.let { put("account", accountJson(it)) }
            data.profile?.let { put("profile", profileJson(it)) }
            put("entitlement", entitlementJson(data.entitlement))
            put("creditLedger", objects(data.creditLedger, ::ledgerEntryJson))
            put("resumeSettings", resumeSettingsJson(data.resumeSettings))
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

    private fun ledgerEntryJson(entry: CreditLedgerEntry): JsonObject = buildJsonObject {
        put("kind", entry.kind.name)
        put("amount", entry.amount)
        put("applicationId", entry.applicationId)
        put("productId", entry.productId)
        put("createdAt", entry.createdAt.toString())
    }

    private fun resumeSettingsJson(settings: ResumeSettings): JsonObject = buildJsonObject {
        put("pageSize", settings.pageSize.name)
        put("fileNameFormat", settings.fileNameFormat.name)
        put("productUpdates", settings.productUpdates)
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
        put("city", profile.city)
        put("linkedinUrl", profile.linkedinUrl)
        put("portfolioUrl", profile.portfolioUrl)
        put("summary", profile.summary)
        put("sourceFileName", profile.sourceFileName)
        put("reviewedAt", profile.reviewedAt?.toString())
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
        put("location", application.location)
        put("appliedOn", application.appliedOn?.toString())
        put("exportFileName", application.exportFileName)
        put("changesAcceptedAt", application.changesAcceptedAt?.toString())
        if (application.legacyNotes.isNotEmpty()) put("notes", application.legacyNotes)
        application.legacyStatus?.let { put("legacyStatus", it) }
        application.keywordCoverage?.let { put("keywordCoverage", applicationCoverageJson(it)) }
        application.quickAnswer?.let { put("quickAnswer", quickAnswerJson(it)) }
        put("createdAt", application.createdAt.toString())
        put("updatedAt", application.updatedAt.toString())
        put("job", jobJson(application.job))
        application.gapAnalysis?.let { put("gapAnalysis", gapAnalysisJson(it)) }
        application.tailoredResume?.let { put("tailoredResume", tailoredResumeJson(it)) }
        put("exports", objects(data.exports.filter { it.applicationId == application.id }, ::exportJson))
    }

    private fun applicationCoverageJson(coverage: ApplicationKeywordCoverage): JsonObject = buildJsonObject {
        put("now", coverage.now)
        put("upTo", coverage.upTo)
        put("final", coverage.final)
    }

    private fun quickAnswerJson(answer: QuickAnswer): JsonObject = buildJsonObject {
        put("requirementId", answer.requirementId)
        put("choice", answer.choice)
        put("detail", answer.detail)
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

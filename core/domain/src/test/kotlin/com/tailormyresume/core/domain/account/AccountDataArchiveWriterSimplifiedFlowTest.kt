package com.tailormyresume.core.domain.account

import com.google.common.truth.Truth.assertThat
import com.tailormyresume.core.domain.PurchaseEntitlement
import com.tailormyresume.core.model.ApplicationKeywordCoverage
import com.tailormyresume.core.model.CreditLedgerEntry
import com.tailormyresume.core.model.CreditLedgerKind
import com.tailormyresume.core.model.FileNameFormat
import com.tailormyresume.core.model.PageSize
import com.tailormyresume.core.model.QuickAnswer
import com.tailormyresume.core.model.ResumeSettings
import com.tailormyresume.core.testing.data.canonicalApplication
import com.tailormyresume.core.testing.data.canonicalCandidateProfile
import kotlinx.serialization.json.Json
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

class AccountDataArchiveWriterSimplifiedFlowTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val now = Instant.parse("2026-10-10T09:00:00Z")

    private val profile = canonicalCandidateProfile.copy(
        city = "Pune",
        linkedinUrl = "https://www.linkedin.com/in/priya",
        portfolioUrl = "https://priya.example.com",
        summary = "Finance analyst with four years of SQL work.",
        sourceFileName = "Priya_Resume.pdf",
        reviewedAt = now,
    )

    private val application = canonicalApplication.copy(
        location = "Bengaluru - Hybrid",
        appliedOn = now,
        keywordCoverage = ApplicationKeywordCoverage(now = 61, upTo = 92, final = 84),
        exportFileName = "Priya_Northwind_Analyst.pdf",
        quickAnswer = QuickAnswer("req-presenting", "yes", "Quarterly reviews"),
        changesAcceptedAt = now,
    )

    private val data = AccountData(
        generatedAt = now,
        account = null,
        profile = profile,
        applications = listOf(application),
        entitlement = PurchaseEntitlement(freeCredits = 0, purchasedCredits = 0, pendingPackIds = emptyList(), unlockedApplicationIds = emptySet()),
        purchases = emptyList(),
        exports = emptyList(),
        creditLedger = listOf(
            CreditLedgerEntry(CreditLedgerKind.FREE_GRANT, 1, null, null, now),
            CreditLedgerEntry(CreditLedgerKind.PURCHASE, 5, null, "application_pack_5", now),
        ),
        resumeSettings = ResumeSettings(PageSize.LETTER, FileNameFormat.NAME_ROLE, productUpdates = true),
    )

    private fun archive(): Map<String, String> {
        val target = File(folder.root, "archive.zip")
        AccountDataArchiveWriter().write(data, target)
        return ZipFile(target).use { zip ->
            zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
        }
    }

    @Test
    fun archiveHoldsNewFieldsLedgerAndSettingsButNoNotes() {
        val files = archive()
        val root = Json.parseToJsonElement(files.getValue("my-data.json")).jsonObject

        val profileJson = root.getValue("profile").jsonObject
        assertThat(profileJson.string("city")).isEqualTo("Pune")
        assertThat(profileJson.string("linkedinUrl")).isEqualTo("https://www.linkedin.com/in/priya")
        assertThat(profileJson.string("portfolioUrl")).isEqualTo("https://priya.example.com")
        assertThat(profileJson.string("summary")).isEqualTo("Finance analyst with four years of SQL work.")
        assertThat(profileJson.string("sourceFileName")).isEqualTo("Priya_Resume.pdf")
        assertThat(profileJson.string("reviewedAt")).isEqualTo(now.toString())

        val applicationJson = root.getValue("applications").jsonArray.single().jsonObject
        assertThat(applicationJson.string("location")).isEqualTo("Bengaluru - Hybrid")
        assertThat(applicationJson.string("appliedOn")).isEqualTo(now.toString())
        assertThat(applicationJson.string("exportFileName")).isEqualTo("Priya_Northwind_Analyst.pdf")
        assertThat(applicationJson.string("changesAcceptedAt")).isEqualTo(now.toString())
        val coverage = applicationJson.getValue("keywordCoverage").jsonObject
        assertThat(coverage.getValue("now").jsonPrimitive.int).isEqualTo(61)
        assertThat(coverage.getValue("upTo").jsonPrimitive.int).isEqualTo(92)
        assertThat(coverage.getValue("final").jsonPrimitive.int).isEqualTo(84)
        val answer = applicationJson.getValue("quickAnswer").jsonObject
        assertThat(answer.string("requirementId")).isEqualTo("req-presenting")
        assertThat(answer.string("choice")).isEqualTo("yes")
        assertThat(answer.string("detail")).isEqualTo("Quarterly reviews")

        val ledger = root.getValue("creditLedger").jsonArray.map { it.jsonObject }
        assertThat(ledger.map { it.string("kind") }).containsExactly("FREE_GRANT", "PURCHASE").inOrder()
        assertThat(ledger[1].string("productId")).isEqualTo("application_pack_5")
        val settings = root.getValue("resumeSettings").jsonObject
        assertThat(settings.string("pageSize")).isEqualTo("LETTER")
        assertThat(settings.string("fileNameFormat")).isEqualTo("NAME_ROLE")
        assertThat(settings.getValue("productUpdates").jsonPrimitive.content).isEqualTo("true")

        assertThat(applicationJson.keys).doesNotContain("notes")
        assertThat(files.getValue("applications.txt")).doesNotContain("notes")
    }

    @Test
    fun storedLegacyNotesAndStatusAreExportedInTextAndJson() {
        val migrated = data.copy(applications = listOf(application.copy(legacyNotes = "Referral from a colleague", legacyStatus = "NO_RESPONSE")))
        val target = File(folder.root, "legacy.zip")
        AccountDataArchiveWriter().write(migrated, target)
        val files = ZipFile(target).use { zip ->
            zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
        }

        val applicationJson = Json.parseToJsonElement(files.getValue("my-data.json")).jsonObject
            .getValue("applications").jsonArray.single().jsonObject
        assertThat(applicationJson.string("notes")).isEqualTo("Referral from a colleague")
        assertThat(applicationJson.string("legacyStatus")).isEqualTo("NO_RESPONSE")
        assertThat(files.getValue("applications.txt")).contains("Referral from a colleague")
        assertThat(files.getValue("applications.txt")).contains("legacyStatus=NO_RESPONSE")
    }

    @Test
    fun theTextEntriesHoldTheNewFieldsToo() {
        val files = archive()

        assertThat(files.getValue("profile.txt")).contains("Pune")
        assertThat(files.getValue("profile.txt")).contains("Finance analyst with four years of SQL work.")
        assertThat(files.getValue("profile.txt")).contains("https://www.linkedin.com/in/priya")
        assertThat(files.getValue("applications.txt")).contains("Priya_Northwind_Analyst.pdf")
        assertThat(files.getValue("applications.txt")).contains("Quarterly reviews")
        assertThat(files.getValue("purchases.txt")).contains("application_pack_5")
        assertThat(files.getValue("account.txt")).contains("pageSize=LETTER")
    }

    private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content
}

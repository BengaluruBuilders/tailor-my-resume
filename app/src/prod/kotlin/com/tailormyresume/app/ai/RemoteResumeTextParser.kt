package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.ResumeTextParser
import com.tailormyresume.core.domain.fact.FactIdAllocator
import com.tailormyresume.core.model.CandidateProfile
import com.tailormyresume.core.model.EvidenceBullet
import com.tailormyresume.core.model.FactSource
import com.tailormyresume.core.model.ProfileEntry
import com.tailormyresume.core.model.fitBulletsToLimit
import com.tailormyresume.core.model.splitBulletsForEntries
import com.tailormyresume.core.network.TailorMyResumeApi
import com.tailormyresume.core.network.dto.ParsedEntryDto
import com.tailormyresume.core.network.dto.ResumeParseRequest
import javax.inject.Inject

class RemoteResumeTextParser @Inject constructor(
    private val api: TailorMyResumeApi,
    private val ids: FactIdAllocator,
) : ResumeTextParser {
    override suspend fun parse(rawText: String): CandidateProfile = parse(rawText, keptEntries = 0)

    override suspend fun parse(rawText: String, keptEntries: Int): CandidateProfile {
        val response = remoteAi { api.parseResume(ResumeParseRequest(rawText)) }
        val parsed = response.profile
        return CandidateProfile(
            fullName = parsed.fullName.orEmpty(),
            email = parsed.email.orEmpty(),
            phone = parsed.phone.orEmpty(),
            headline = parsed.headline.orEmpty(),
            skills = parsed.skills,
            entries = entriesOf(parsed.entries, keptEntries),
        )
    }

    private fun entriesOf(parsed: List<ParsedEntryDto>, keptEntries: Int): List<ProfileEntry> {
        val entries = mutableListOf<ProfileEntry>()
        parsed.forEachIndexed { index, entry ->
            val texts = entry.bullets.map { it.text }.filter { it.isNotBlank() }
            splitBulletsForEntries(texts, keptEntries + entries.size, parsed.size - index - 1).forEach { chunk ->
                val id = ids.nextId(entry.category, entries, entry.title)
                entries += ProfileEntry(
                    id = id,
                    category = entry.category,
                    title = entry.title,
                    organization = entry.organization.orEmpty(),
                    startDate = entry.startDate.orEmpty(),
                    endDate = entry.endDate.orEmpty(),
                    bullets = fitBulletsToLimit(chunk).mapIndexed { i, text -> EvidenceBullet("$id-b${i + 1}", text) },
                    source = FactSource.IMPORTED,
                    isConfirmed = false,
                )
            }
        }
        return entries
    }
}
